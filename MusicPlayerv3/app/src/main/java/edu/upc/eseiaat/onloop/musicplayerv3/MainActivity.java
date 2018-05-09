package edu.upc.eseiaat.onloop.musicplayerv3;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.florescu.android.rangeseekbar.RangeSeekBar;

public class MainActivity extends AppCompatActivity {

    private Button btn_play, btn_explore, btn_stop, btn_start, btn_end, btn_restore;
    private TextView txt_song;
    private Uri urisong;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false, pause=true;
    private SeekBar seekBar;
//    private RangeSeekBar<Double> rangeSeekBar;
    private Handler handler;
    private Runnable runnable;
    private Integer duration =0, start, end;

    //Iniciar el servei a onstart (perquè s'obri ja quan s'obre també l'app)
    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onDestroy() {
        if(musicBound){
            stopService(playIntent);
            unbindService(musicConnection);
            musicSrv=null;

            handler.removeCallbacks(runnable);
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK},1);

                return;
            }}

        //Inicialitzar button i textview
        btn_play = findViewById(R.id.btn_play);
        btn_explore = findViewById(R.id.btn_explore);
        btn_stop = findViewById(R.id.btn_stop_loop);
        txt_song = findViewById(R.id.txt_song);
        btn_start = findViewById(R.id.btn_start);
        btn_end = findViewById(R.id.btn_end);
        btn_restore = findViewById(R.id.btn_restore);

        btn_play.setVisibility(View.GONE);
        btn_stop.setVisibility(View.GONE);
        btn_start.setVisibility(View.GONE);
        btn_end.setVisibility(View.GONE);
        btn_restore.setVisibility(View.GONE);
        txt_song.setText("Nothing! Choose a song to listen");

        handler = new Handler();

        seekBar = findViewById(R.id.seekBar);

    }

    //Connexió al servei
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //"get service"
            musicSrv = binder.getService();
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void click_play(View view){

        btn_explore.setVisibility(view.GONE);

        if (urisong == null) {
            Toast.makeText(musicSrv, "please pick a song first", Toast.LENGTH_SHORT).show();
        }

        else {

            if (pause) { //si no s'està reproduïnt res

                if (musicSrv.firstPlay()) { //si és el primer cop que es reprodueix

                    // seleccionar cançó
                    musicSrv.setSong(urisong);
                    //i reproduirla
                    musicSrv.playSong();
                } else {
                    if (seekBar.getProgress() != musicSrv.getCurrentPosition()) { //actualitzar cançó
                                                                // si es canvia la posició a la seekbar
                        musicSrv.replaySongFrom(seekBar.getProgress());
                    }

                    else {
                        musicSrv.replaySongFrom(musicSrv.getCurrentPosition());
                    }
                }

                btn_play.setText("Pause");
                pause = false;

            } else {

                musicSrv.pauseSong(); //pause
                btn_play.setText("Play");
                pause = true;

            }
        }

    }

    public void click_stop(View view) {

        btn_play.setVisibility(View.GONE);
        btn_stop.setVisibility(View.GONE);
        btn_explore.setVisibility(View.VISIBLE);
        btn_start.setVisibility(View.GONE);
        btn_end.setVisibility(View.GONE);
        btn_restore.setVisibility(View.GONE);

        urisong = null;
        txt_song.setText("");
        musicSrv.resetPlayer();

        musicSrv.isLoop(false);

        txt_song.setText("Nothing! Choose a song to listen");
    }

    public void click_explore(View view) {
        performFileSearch();
    }

    public void performFileSearch() {
        //1. Crear un intent
        Intent intent = new Intent(this,MusicListActivity.class);
        //3. Passar l'intent a Android perquè inicialitzi l'activitat
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                // (IV)
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    //obtenir dades
                    String songname = data.getStringExtra("songname");
                    String songpath = data.getStringExtra("songpath");
                    String songduration = data.getStringExtra("songduration");

                    Log.i("marta", "song name: " + songname);
                    txt_song.setText(songname);
                    urisong = Uri.parse(songpath);
                    duration = Integer.valueOf(songduration);

                    btn_play.setVisibility(View.VISIBLE);
                    btn_stop.setVisibility(View.VISIBLE);
                    btn_start.setVisibility(View.VISIBLE);
                    btn_end.setVisibility(View.VISIBLE);
                    btn_restore.setVisibility(View.VISIBLE);

                    //inicialitzar els valors i colocar la seekbar a 0
                    seekBar.setProgress(0);
                    seekBar.setMax(duration);

                    //actualitzar seekbar
                    playCycle();

                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                            if (b) {
                                musicSrv.seekTo(i);
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
        }
    }

    //actualitzar  seekbar
    private void playCycle() {

        runnable = new Runnable() {
            @Override
            public void run() {
                seekBar.setProgress(musicSrv.getCurrentPosition());
                playCycle();
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    //inici del bucle
    public void click_start(View view) {
        start = musicSrv.getCurrentPosition();
        musicSrv.setStartPoint(start);
    }

    //fi del bucle
    public void click_end(View view) {
        end = musicSrv.getCurrentPosition();
        musicSrv.setEndPoint(end);
        musicSrv.isLoop(true);
        musicSrv.replaySongFrom(start);
    }

    //eliminar punts del bucle
    public void click_restore(View view) {
        start = 0;
        end = duration;
        musicSrv.stopLoop();
        musicSrv.isLoop(false);
    }

    //TODO: ARREGLAR PROBLEMA AMB FORMAT MPEG (CONVERSIÓ DE FORMAT??)
}
