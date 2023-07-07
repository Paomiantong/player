package cn.edu.bistu.musicplayer.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicService extends Service {
    public MediaPlayer player;
    public static final String TAG = "MusicService";
    //列表循环
    public static final int MENU_RECYCLE = 0;
    //随机播放
    public static final int RANDOM = 1;
    //单曲循环
    public static final int SINGLE_RECYCLE = 2;
    //顺序播放
    public static final int LINE = 3;
    //播放下一首
    public static final int ACTION_NEXT = 1;
    //播放上一首
    public static final int ACTION_PREVIOUS = -1;
    //保存当前播放模式
    private int MODE = MENU_RECYCLE;
    //用于显示播放列表的数据源
    private List<Map<String, Object>> musicList = new ArrayList<>();
    private Map<String, Integer> map = new HashMap<>();
    //当前播放的歌曲索引
    private int currentIndex = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();
        musicList = getMusicList();
        currentIndex = 0;
        Log.d(TAG, "onCreate");
    }

    //  通过 Binder 来保持 Activity 和 Service 的通信
    public MusicBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.release();
    }

    //获取歌曲列表
    public List<Map<String, Object>> getMusicList() {
        File[] files = getFilesDir().listFiles((f, n) -> n.endsWith(".mp3"));
        musicList = new ArrayList<>();
        if (files != null) {
            int index = 0;
            for (File musicFile : files) {
                //获取当前目录的名称和绝对路径
                String path = musicFile.getAbsolutePath();
                String name = musicFile.getName().replace(".mp3", "");
                Map<String, Object> currentMap = new HashMap<>();
                currentMap.put("name", name);
                currentMap.put("path", path);
                musicList.add(currentMap);
                Log.d(TAG, name + ": " + index);
                map.put(name, index++);
            }
        }
        return musicList;
    }

    //播放音乐
    private String playMusic(int index) throws IOException {
        Log.d(TAG, "playMusic: " + index + "/" + musicList.size());
        Map<String, Object> music = musicList.get(index);
        Log.i("i", (String) music.get("name"));
        String path = (String) music.get("path");
        player.reset();
        player.setDataSource(path);
        player.prepare();
        player.start();
        currentIndex = index;
        return (String) music.get("name");
    }

    public String playMusic(String name) throws IOException {
        Log.d(TAG, "playMusic(String name): " + name);
        return playMusic(map.get(name));
    }

    //按照播放模式播放音乐
    public String playNext(int action) throws IOException {
        if (musicList.size() > 0) {
            int index;
            switch (action) {
                case ACTION_NEXT:
                    index = currentIndex + 1;
                    if (index >= musicList.size()) {
                        player.reset();
                        Toast.makeText(this, "没有下一首了", Toast.LENGTH_SHORT).show();
                        index = musicList.size() - 1;
                    }
                    return playMusic(index);
                case ACTION_PREVIOUS:
                    index = currentIndex - 1;
                    if (index < 0) {
                        player.reset();
                        Toast.makeText(this, "没有上一首了", Toast.LENGTH_SHORT).show();
                        index = 0;
                    }
                    return playMusic(index);
            }
        }
        return "无歌曲";
    }

    //播放/暂停
    public void toggle() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.start();
        }
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }
}