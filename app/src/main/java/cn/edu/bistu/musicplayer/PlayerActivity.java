package cn.edu.bistu.musicplayer;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import cn.edu.bistu.musicplayer.databinding.ActivityPlayerBinding;
import cn.edu.bistu.musicplayer.service.MusicService;


public class PlayerActivity extends AppCompatActivity {
    private ActivityPlayerBinding binding;
    private final SimpleDateFormat formatter = new SimpleDateFormat("mm:ss", Locale.getDefault());
    private MusicService musicService;
    private String songName;
    public final static String TAG = "MainActivity";
    //快进快退
    private final Handler updaterHandler = new Handler();
    public int mValue;
    private boolean forward = false;
    private boolean backward = false;
    private ObjectAnimator animator;

    private class processUpdater implements Runnable {
        public void run() {
            if (forward) {
                mValue += 10; //change this value to control how much to forward
                musicService.player.seekTo(musicService.player.getCurrentPosition() + mValue);
                musicService.player.pause();
                updaterHandler.postDelayed(new processUpdater(), 50);
            } else if (backward) {
                mValue -= 10; //change this value to control how much to rewind
                musicService.player.seekTo(musicService.player.getCurrentPosition() - mValue);
                musicService.player.pause();
                updaterHandler.postDelayed(new processUpdater(), 50);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        songName = getIntent().getStringExtra("song");
        bindServiceConnection();

        animator = ObjectAnimator.ofFloat(binding.cover, "rotation", 0f, 360.0f);
        animator.setDuration(100000 / 2);
        animator.setInterpolator(new LinearInterpolator());//匀速
        animator.setRepeatCount(-1);//设置动画重复次数（-1代表一直转）
        animator.setRepeatMode(ValueAnimator.RESTART);//动画重复模式

        bindListener();
    }

    //  在Activity中调用 bindService 保持与 Service 的通信
    private void bindServiceConnection() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    //  回调onServiceConnected 函数，通过IBinder 获取 Service对象，实现Activity与 Service的绑定
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MusicBinder) (service)).getService();
            try {
                binding.MusicTitle.setText(musicService.playMusic(songName));
                animator.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            handler.postDelayed(refresh, 200);
            refreshMusicView();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private void bindListener() {
        //拉动进度条
        binding.MusicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicService.player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //下一首监听
        binding.BtnNext.setOnClickListener(v -> {
            try {
                String name = musicService.playNext(MusicService.ACTION_NEXT);
                binding.MusicTitle.setText(name);
                animator.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        //上一首监听
        binding.BtnPre.setOnClickListener(v -> {
            try {
                String name = musicService.playNext(MusicService.ACTION_PREVIOUS);
                binding.MusicTitle.setText(name);
                animator.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        //快进监听
        binding.BtnForward.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.setPressed(true);
                forward = true;
                updaterHandler.post(new processUpdater());
                return false;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.setPressed(false);
                if (forward) {
                    forward = false;
                }
                musicService.player.start();
                return false;
            }
            return false;
        });

        //快退监听
        binding.BtnBackward.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.setPressed(true);
                backward = true;
                updaterHandler.post(new processUpdater());
                return false;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.setPressed(false);
                if (backward) {
                    backward = false;
                }
                musicService.player.start();
                return false;
            }
            return false;
        });

        //播放/暂停监听
        binding.BtnPlayOrPause.setOnClickListener(v -> {
            if (musicService.player != null) {
                binding.MusicSeekBar.setProgress(musicService.player.getCurrentPosition());
                binding.MusicSeekBar.setMax(musicService.player.getDuration());
            }
            musicService.toggle();
            //  由tag的变换来控制事件的调用
        });
    }

    //  通过 Handler 更新 UI 上的组件状态
    private final Handler handler = new Handler();
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            refreshMusicView();
            handler.postDelayed(refresh, 200);
        }
    };

    @SuppressLint("SetTextI18n")
    private void refreshMusicView() {
        binding.MusicTime.setText(formatter.format(musicService.player.getCurrentPosition()));
        binding.MusicTotal.setText(formatter.format(musicService.player.getDuration()));
        binding.MusicSeekBar.setProgress(musicService.player.getCurrentPosition());
        binding.MusicSeekBar.setMax(musicService.player.getDuration());
        if (musicService.isPlaying()) {
            binding.BtnPlayOrPause.setImageResource(android.R.drawable.ic_media_pause);
            binding.MusicStatus.setText("Playing");
            animator.resume();
        } else {
            binding.BtnPlayOrPause.setImageResource(android.R.drawable.ic_media_play);
            binding.MusicStatus.setText("Paused");
            animator.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refresh);
        unbindService(serviceConnection);
    }
}

