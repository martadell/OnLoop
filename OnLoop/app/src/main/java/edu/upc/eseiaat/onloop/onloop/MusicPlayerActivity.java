package edu.upc.eseiaat.onloop.onloop;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarFinalValueListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    private TextView txt_song, txt_artist, txt_song_timing, txt_loop_start, txt_loop_end,
            txt_speed, txt_speed_value;
    private ImageButton btn_play;
    private Button btn_explore;
    private SeekBar songSeekBar, speedSeekBar;
    private CrystalRangeSeekbar loopSeekBar;
    private Uri urisong;
    private Integer duration =0;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean binding=false, ready = false;
    private Handler handler, dhandler;
    private Runnable runnable, drunnable;
    private Boolean firstTime = null;

    @Override
    protected void onDestroy() {
        if (musicSrv != null) {
            unbindService(musicConnection);
            handler.removeCallbacks(runnable);
        }

        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (musicSrv != null) {
            musicSrv.showNotification();
        }

        super.onStop();
    }

    @Override
    protected void onResume() {
        if (musicSrv != null) {
            musicSrv.closeNotification();
        }
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK, Manifest.permission.READ_PHONE_STATE},1);

                return;
            }}

        btn_play = findViewById(R.id.btn_play);
        txt_song = findViewById(R.id.txt_song);
        txt_artist = findViewById(R.id.txt_artist);
        txt_song_timing = findViewById(R.id.txt_song_timing);
        txt_loop_start = findViewById(R.id.txt_loop_start);
        txt_loop_end = findViewById(R.id.txt_loop_end);
        songSeekBar =  findViewById(R.id.songSeekBar);
        loopSeekBar = findViewById(R.id.loopSeekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        txt_speed = findViewById(R.id.txt_speed);
        txt_speed_value = findViewById(R.id.txt_speed_value);

        btn_explore = findViewById(R.id.viewSongInfo);
        btn_explore.setLongClickable(true);
        btn_explore.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                performFileSearch();
                return true;
            }
        });

        handler = new Handler();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                speedSeekBar.setMax(10);
                speedSeekBar.setProgress(5);
                txt_speed_value.setText("1.0");
                speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        if (urisong != null) {
                            musicSrv.changeplayerSpeed(getConvertedFloatValue(i)); }
                        if (i != 9) {
                            txt_speed_value.setText(String.valueOf(getConvertedFloatValue(i))); }
                        else {
                            txt_speed_value.setText("1.4");
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
        }

        else  {
            speedSeekBar.setVisibility(View.GONE);
            txt_speed.setVisibility(View.GONE);
            txt_speed_value.setVisibility(View.GONE);
        }


        if(!binding){
            playIntent = new Intent(this, MusicService.class);
            startService(playIntent);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            }

        if (isFirstTime()) {
        txt_song.setText(R.string.nothing_playing);

        }
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            musicSrv = binder.getService();
            binding = true;

            if (musicSrv.isServicePlayingASong()) {
                txt_song.setText(musicSrv.getSongname());
                txt_artist.setText(musicSrv.getSongartist());
                duration = musicSrv.getDuration();
                urisong = musicSrv.getUrisong();
                songSeekBar.setMax(duration);
                loopSeekBar.setMinValue(0);
                loopSeekBar.setMaxValue(duration);

                loopSeekBar.setMinStartValue(musicSrv.getStartPoint()).setMaxStartValue(musicSrv.getEndPoint()).apply();
                txt_loop_start.setText(generateMinutesandSeconds(musicSrv.getStartPoint()));
                txt_loop_end.setText(generateMinutesandSeconds(musicSrv.getEndPoint()));


                setLoopSeekBarListeners();
                setSongSeekBarListeners();

                playCycle();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        float speed = musicSrv.getSpeed();
                        speedSeekBar.setProgress(getConvertedIntegerValue(speed));

                    if (getConvertedIntegerValue(speed) != 9) {
                        txt_speed_value.setText(String.valueOf(getConvertedFloatValue(getConvertedIntegerValue(speed)))); }
                    else {
                        txt_speed_value.setText("1.4");
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binding = false;
        }
    };

    public void click_play(View view){

        if (urisong != null && ready) {

            playCycle();

            if (!musicSrv.isPlaying()) {

                if (songSeekBar.getProgress() == 0) {
                    musicSrv.playSong();
                } else {
                    if (songSeekBar.getProgress() != musicSrv.getCurrentPosition()) {
                        musicSrv.replaySongFrom(songSeekBar.getProgress());
                    } else {
                        musicSrv.replaySongFrom(musicSrv.getCurrentPosition());
                    }

                }
            }

            else {
                    musicSrv.pausePlayer();
                }
            }

            else {
            if (!ready) Toast.makeText(musicSrv, R.string.Loading, Toast.LENGTH_SHORT).show();
        }
        }

    public void click_stop(View view) {
        if (urisong != null) {
            if (musicSrv.getStartPoint() > 0 || musicSrv.getEndPoint() < duration) {
                musicSrv.pausePlayer();
                musicSrv.seekTo(musicSrv.getStartPoint());
            }

            else {
                musicSrv.pausePlayer();
                musicSrv.seekTo(0);
            }
        }
    }

    public void performFileSearch() {
        ready = false;
        Intent intent = new Intent(this,MusicListActivity.class);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    String songname = data.getStringExtra("songname");
                    String songartist = data.getStringExtra("songartist");
                    String songpath = data.getStringExtra("songpath");

                    txt_song.setText(songname);
                    txt_artist.setText(songartist);
                    urisong = Uri.parse(songpath);

                    musicSrv.setSongParams(songname, songartist, urisong);

                    if (musicSrv.isPlaying()) {
                        musicSrv.pausePlayer();
                        musicSrv.seekTo(0);
                    }

                    setSongSeekBarListeners();
                    setLoopSeekBarListeners();

                    dhandler = new Handler();
                    changeDuration();
                }
        }
    }

    private void changeDuration() {
        drunnable = new Runnable() {
            @Override
            public void run() {
                if (musicSrv.getDuration() != null) {
                    if (duration == 0 || duration != musicSrv.getDuration()) {
                        duration = musicSrv.getDuration();
                        txt_loop_end.setText(generateMinutesandSeconds(duration));
                        songSeekBar.setMax(duration);
                        songSeekBar.setProgress(0);
                        musicSrv.setStartPoint(0);
                        musicSrv.setEndPoint(duration);
                        loopSeekBar.setMinStartValue(0).setMaxStartValue(duration).apply();
                        loopSeekBar.setMinValue(0).setMaxValue(duration).apply();

                        dhandler.removeCallbacks(drunnable);
                        ready = true;
                    }
                }else {
                   changeDuration();
                }
            }
        };
        handler.postDelayed(drunnable, 50);
    }

    //Listeners seekbars
    private void setSongSeekBarListeners() {
        songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if ((musicSrv.getStartPoint() > 0 || musicSrv.getEndPoint() < duration) &&
                        (i < musicSrv.getStartPoint() || i > musicSrv.getEndPoint())) {
                        musicSrv.seekTo(musicSrv.getStartPoint());
                    }

                    else {
                        if (b) {
                            musicSrv.seekTo(i);
                        }
                    }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setLoopSeekBarListeners() {
        loopSeekBar.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener() {
            @Override
            public void valueChanged(Number minValue, Number maxValue) {
                txt_loop_start.setText(generateMinutesandSeconds(minValue.intValue()));
                txt_loop_end.setText(generateMinutesandSeconds(maxValue.intValue()));
            }
        });

        loopSeekBar.setOnRangeSeekbarFinalValueListener(new OnRangeSeekbarFinalValueListener() {
            @Override
            public void finalValue(Number minValue, Number maxValue) {
                musicSrv.setStartPoint(minValue.intValue());
                musicSrv.setEndPoint(maxValue.intValue());

                if (musicSrv.getCurrentPosition() < minValue.intValue() || musicSrv.getCurrentPosition() > maxValue.intValue()) {
                    musicSrv.seekTo(minValue.intValue());
                }
            }
        });
    }

    private void playCycle() {

        runnable = new Runnable() {
            @Override
            public void run() {
                if (!musicSrv.isPlaying()) {
                        btn_play.setBackgroundResource(R.drawable.play);
                }

                else {
                    btn_play.setBackgroundResource(R.drawable.pause);
                }

                songSeekBar.setProgress(musicSrv.getCurrentPosition());
                txt_song_timing.setText(generateMinutesandSeconds(musicSrv.getCurrentPosition()));
                playCycle();
            }
        };
        handler.postDelayed(runnable, 500);
    }

    private String generateMinutesandSeconds(Integer millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    public float getConvertedFloatValue(int i) {
        float fl = (float) 0.5;
        fl = fl + .10f * i;
        return fl;
    }

    public Integer getConvertedIntegerValue(float f) {
        Double d = (double) f;
        d = d / .10;

        Integer i = d.intValue();
        i = i - 5;

        return i;
    }

    private boolean isFirstTime() {
        if (firstTime == null) {
            SharedPreferences mPreferences = this.getSharedPreferences("first_time", Context.MODE_PRIVATE);
            firstTime = mPreferences.getBoolean("firstTime", true);
            if (firstTime) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean("firstTime", false);
                editor.apply();
            }
        }
        return firstTime;
    }
}
