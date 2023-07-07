package cn.edu.bistu.musicplayer.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import cn.edu.bistu.musicplayer.Song;

public class DownloadService extends Service {
    public interface Listener {
        public void onFinished(Song song);
    }

    private static final String TAG = "DownloadService";
    private static final String MUSIC_URL = "http://music.163.com/song/media/outer/url?id=%s.mp3";
    private static final String CHANNEL_ID = "Downloading";
    private static int notificationId = 1;
    private final Object o = new Object();
    private static final CharSequence CHANNEL_NAME = "ABC";
    private final Map<String, Boolean> map = new HashMap<>();
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_MIN);
            notificationChannel.enableLights(false);//如果使用中的设备支持通知灯，则说明此通知通道是否应显示灯
            notificationChannel.setShowBadge(false);//是否显示角标
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void downloadMusic(Song song, Listener listener) {
        if (Boolean.FALSE.equals(map.get(song.getName()))) {
            return;
        }
        map.put(song.getName(), false);
        new Thread(() -> {
            int nid;
            synchronized (o) {
                notificationId++;
                nid = notificationId;
            }
            NotificationCompat.Builder builder;
            // 检查私有文件夹中是否已存在该音乐文件
            File musicFile = new File(getFilesDir(), song.getName() + ".mp3");
            try {
                if (musicFile.exists()) {
                    Log.i(TAG, "已存在音乐文件: " + musicFile.getAbsolutePath());
                    return;
                }
                Log.d(TAG, "downloadMusic:" + String.format(MUSIC_URL, song.getUrl()));

                builder = getNotification("开始下载")
                        .setContentText(song.getUrl());
                notificationManager.notify(nid, builder.build());

                // 创建URL对象并建立连接
                URL url = new URL(String.format(MUSIC_URL, song.getUrl()));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.connect();

                int contentLength = connection.getContentLength();

                // 创建输出流和输入流
                FileOutputStream fos = new FileOutputStream(musicFile);
                InputStream is = connection.getInputStream();

                // 从输入流读取数据并写入文件
                byte[] buffer = new byte[1024];
                int bytesRead;
                int total = 0;
                int lastProgress = 0;
                while ((bytesRead = is.read(buffer)) != -1) {
                    total += bytesRead;
                    int progress = (total) * 100 / contentLength;
                    if (lastProgress != progress) {
                        lastProgress = progress;
                        builder = getNotification("歌曲下载...")
                                .setContentText(song.getName() + progress + "%")
                                .setProgress(100, progress, false)
                                .setAutoCancel(true);
                        notificationManager.notify(nid, builder.build());
                    }
                    fos.write(buffer, 0, bytesRead);
                }
                // 关闭流
                fos.close();
                is.close();

                Log.i(TAG, "音乐文件下载完成: " + musicFile.getAbsolutePath());
                notificationManager.cancel(nid);
                builder = getNotification("下载完成")
                        .setContentText(song.getName() + "已经成功下载完成");
                notificationManager.notify(nid, builder.build());
                map.put(song.getName(), true);
                listener.onFinished(song);
            } catch (Exception e) {
                e.printStackTrace();
                musicFile.delete();

                builder = getNotification("下载失败")
                        .setContentText(song.getName() + "下载失败");
                notificationManager.notify(nid, builder.build());
            }
        }).start();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return binder;
    }

    private NotificationCompat.Builder getNotification(String title) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title);
    }

    private final DownloadBinder binder = new DownloadBinder();

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }
}