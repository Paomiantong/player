package cn.edu.bistu.musicplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.LinkedList;

import cn.edu.bistu.musicplayer.databinding.ActivityMainBinding;
import cn.edu.bistu.musicplayer.service.DownloadService;

public class MainActivity extends AppCompatActivity {
    private boolean downloading;
    private ActivityMainBinding binding;
    private DownloadService downloadService;

    private final LinkedList<Song> songs = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        bindServiceConnection();

        songs.add(new Song("我和我的祖国", "1392990601", "王菲", "NA"));
        songs.add(new Song("Dragonsong", "41672542", "Susan Calloway", "NA"));
        songs.add(new Song("Footfalls", "2032407923", "Lollia,RichaadEB,The8BitDrummer,Suna,Sab Irene", "NA"));
        songs.add(new Song("入海", "1449782341", "毛不易", "NA"));
        songs.add(new Song("许你", "1905106740", "程响", "NA"));
        songs.add(new Song("富士山下", "64517", "陈奕迅", "NA"));
        songs.add(new Song("人间烟火", "1929363849", "程响", "NA"));
        binding.songsList.setAdapter(new SongListAdapter(this, R.layout.songslist, songs));

        binding.songsList.setOnItemClickListener(((adapterView, view, i, l) -> {
            if (downloading) {
                Toast.makeText(this, "请等待歌曲下载完成", Toast.LENGTH_SHORT).show();
                return;
            }
            Song song = songs.get(i);
            File musicFile = new File(getFilesDir(), song.getName() + ".mp3");
            if (!musicFile.exists()) {
                Toast.makeText(this, "音乐未下载", Toast.LENGTH_SHORT).show();
                downloading = true;
                downloadService.downloadMusic(song, (s) -> {
                    downloading = false;
                    Intent intent = new Intent(this, PlayerActivity.class);
                    intent.putExtra("song", s.getName());
                    startActivity(intent);

                });
            } else {
                Intent intent = new Intent(this, PlayerActivity.class);
                intent.putExtra("song", song.getName());
                startActivity(intent);
            }
        }));
    }

    private void bindServiceConnection() {
        Intent intent = new Intent(this, DownloadService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    //  回调onServiceConnected 函数，通过IBinder 获取 Service对象，实现Activity与 Service的绑定
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadService = ((DownloadService.DownloadBinder) (service)).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            downloadService = null;
        }
    };
}