package edu.upc.eseiaat.onloop.onloop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarFinalValueListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    private ImageButton btn_play;
    private Button btn_explore;
    private TextView txt_song, txt_artist, txt_song_timing, txt_loop_start, txt_loop_end,
            txt_speed, txt_speed_value;
    private Uri urisong;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean binding=false;
    private CrystalRangeSeekbar loopSeekBar;
    private SeekBar songSeekBar, speedSeekBar;
    private Handler handler;
    private Runnable runnable;
    private Integer duration =0;


    @Override
    protected void onStop() {
        if (urisong != null) {
            musicSrv.showNotification();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unbindService(musicConnection);
        handler.removeCallbacks(runnable);

        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        //permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK},1);

                return;
            }}

        //inicialitzar
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

        btn_explore.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                performFileSearch();
                return true;
            }
        });

        //crear handler (per la seekbar)
        handler = new Handler();


        if (isFirstTime()) {
            txt_song.setText(R.string.nothing_playing);
        }

        //velocitat
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

                        btn_play.setBackgroundResource(R.mipmap.pause);
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
            startService(playIntent); //cridem a startService perquè no es tanqui el servei encara que es desvinculi
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            }
        }

    //Connexió al servei
    private ServiceConnection musicConnection = new ServiceConnection(){

        @SuppressLint("SetTextI18n")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //"get service"
            musicSrv = binder.getService();
            //obtenir una referència de l'objecte que enllaça el servei
            binding = true;

            //actualitzar interficie si està reproduint una cançó
            if (musicSrv.isServicePlayingASong()) {
                txt_song.setText(musicSrv.getSongname());
                txt_artist.setText(musicSrv.getSongartist());
                duration = musicSrv.getDuration();
                urisong = musicSrv.getUrisong();
                songSeekBar.setMax(duration);
                loopSeekBar.setMinValue(0);
                loopSeekBar.setMaxValue(duration);
                loopSeekBar.setMinStartValue(musicSrv.getStartPoint());
                loopSeekBar.setMaxStartValue(musicSrv.getEndPoint());
                txt_loop_start.setText(generateMinutesandSeconds(musicSrv.getStartPoint()));
                txt_loop_end.setText(generateMinutesandSeconds(musicSrv.getEndPoint()));
                setLoopSeekBarListeners();

                setSongSeekBarListeners();
                playCycle();

                if (!musicSrv.isPlaying()) {
                    musicSrv.changeplayerSpeed(1);
                }

                btn_play.setBackgroundResource(R.mipmap.pause);

                //velocitat
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    txt_speed_value.setText(Float.toString(musicSrv.getSpeed()));

                    int pos =  getConvertedIntegerValue(musicSrv.getSpeed());
                    speedSeekBar.setProgress(pos);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binding = false;
        }
    };

    //Buttons
    public void click_play(View view){

        if (urisong != null) {

            //actualitzar seekbar
            playCycle();

            if (!musicSrv.isPlaying()) { //si no s'està reproduïnt res

                if (musicSrv.getCurrentPosition() == 0) { //si és el primer cop que es reprodueix (està a 0)
                    //reproduir cançó
                    musicSrv.playSong();
                } else {
                    if (songSeekBar.getProgress() != musicSrv.getCurrentPosition()) { //actualitzar cançó
                        // si es canvia la posició a la seekbar
                        musicSrv.replaySongFrom(songSeekBar.getProgress());
                    }

                    else {
                        musicSrv.replaySongFrom(musicSrv.getCurrentPosition());
                   }
                }

                btn_play.setBackgroundResource(R.mipmap.pause);

            } else {

                musicSrv.pausePlayer(); //pause
                btn_play.setBackgroundResource(R.mipmap.play);

            }
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

            if (!musicSrv.isPlaying()) {
                btn_play.setBackgroundResource(R.mipmap.play);
            }
        }
    }

    //buscar cançó
    public void performFileSearch() {
        Intent intent = new Intent(this,MusicListActivity.class);
        startActivityForResult(intent, 0);
    }

    //resultat
    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    //obtenir dades
                    String songname = data.getStringExtra("songname");
                    String songartist = data.getStringExtra("songartist");
                    String songpath = data.getStringExtra("songpath");
                    String songduration = data.getStringExtra("songduration");

                    txt_song.setText(songname);
                    txt_artist.setText(songartist);
                    urisong = Uri.parse(songpath);
                    duration = Integer.valueOf(songduration);

                    musicSrv.setSongParams(songname, songartist, urisong, duration);

                    txt_loop_end.setText(generateMinutesandSeconds(duration));

                    //inicialitzar els valors i colocar la seekbar de la cançó a 0
                    songSeekBar.setMax(duration);
                    setSongSeekBarListeners();

                    //el mateix per la rangeSeekBar
                    loopSeekBar.setMinValue(0);
                    loopSeekBar.setMaxValue(duration);
                    setLoopSeekBarListeners();
                }
            case 1:
                btn_play.setBackgroundResource(R.mipmap.play);
        }
    }

    //Listeners seekbars
    private void setSongSeekBarListeners() {
        songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (musicSrv.getStartPoint() > 0 || musicSrv.getEndPoint() < duration) { //si hi ha bucle
                    if (i < musicSrv.getStartPoint() || i > musicSrv.getEndPoint()) {
                        //si es mou per fora del bucle
                        musicSrv.seekTo(musicSrv.getStartPoint());
                    }

                    else {
                        if (b) {
                            musicSrv.seekTo(i);
                        }
                    }
                }

                else {
                    if (b) { //si el canvi ha estat fet per l'usuari
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
                //valors txt canvi
                txt_loop_start.setText(generateMinutesandSeconds(minValue.intValue()));
                txt_loop_end.setText(generateMinutesandSeconds(maxValue.intValue()));
            }
        });

        loopSeekBar.setOnRangeSeekbarFinalValueListener(new OnRangeSeekbarFinalValueListener() {
            @Override
            public void finalValue(Number minValue, Number maxValue) {
                musicSrv.setStartPoint(minValue.intValue());
                musicSrv.setEndPoint(maxValue.intValue());

                //si s'està reproduint fora del bucle es posa a dins
                if (musicSrv.getCurrentPosition() < minValue.intValue() || musicSrv.getCurrentPosition() > maxValue.intValue()) {
                    musicSrv.seekTo(minValue.intValue());
                }
            }
        });
    }

    //actualitzar seekbar (i timing)
    private void playCycle() {

        runnable = new Runnable() {
            @Override
            public void run() {
                songSeekBar.setProgress(musicSrv.getCurrentPosition());
                txt_song_timing.setText(generateMinutesandSeconds(musicSrv.getCurrentPosition()));
                playCycle();
            }
        };
        handler.postDelayed(runnable, 500);
    }

    //Generar temps en format 00:00
    private String generateMinutesandSeconds(Integer millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    //getters i setters
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

    private boolean isFirstTime()
    {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean ranBefore = preferences.getBoolean("RanBefore", false);
        if (!ranBefore) {
            // first time
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("RanBefore", true);
            editor.apply();
        }
        return ranBefore;
    }
}
