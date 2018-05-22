package edu.upc.eseiaat.onloop.onloop;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    private Button btn_play, btn_stop, btn_start, btn_end, btn_restore;
    private TextView txt_song, txt_artist, txt_song_timing, txt_song_duration;
    private Uri urisong;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean stop=true;
    private SeekBar seekBar;
    //    private RangeSeekBar<Double> rangeSeekBar;
    private Handler handler;
    private Runnable runnable;
    private Integer duration =0;

    //Iniciar el servei a onstart (perquè s'obri ja quan s'obre també l'app)
    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onDestroy() {
        if(musicSrv != null){
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
        setContentView(R.layout.activity_music_player);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK},1);

                return;
            }}

        //Inicialitzar button i textview
        btn_play = findViewById(R.id.btn_play);
        btn_stop = findViewById(R.id.btn_stop_loop);
        txt_song = findViewById(R.id.txt_song);
        txt_artist = findViewById(R.id.txt_artist);
        txt_song_timing = findViewById(R.id.txt_song_timing);
        txt_song_duration = findViewById(R.id.txt_song_duration);

        btn_start = findViewById(R.id.btn_start);
        btn_end = findViewById(R.id.btn_end);
        btn_restore = findViewById(R.id.btn_restore);

        txt_song.setText(R.string.nothing_playing);
        txt_artist.setText("---");

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
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicSrv = null;
        }
    };

    public void click_play(View view){

        if (urisong != null) {

            //actualitzar seekbar
            playCycle();

            if (stop) { //si no s'està reproduïnt res

                if (musicSrv.getCurrentPosition() == 0) { //si és el primer cop que es reprodueix (està a 0)
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
                stop = false;

            } else {

                musicSrv.pausePlayer(); //pause
                btn_play.setText("Play");
                stop = true;

            }
        }

    }

    public void click_stop(View view) {
        if (urisong != null) {
            if (musicSrv.getLoop() == true) {
                musicSrv.pausePlayer();
                musicSrv.seekTo(musicSrv.getStartPoint());
            }

            else {
                musicSrv.resetPlayer();
            }

            if (!stop) {
                btn_play.setText("Play");
                stop = true;
            }
        }
    }

    //Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.musiclist_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.explore:
                performFileSearch();

                return true;

            case R.id.clear:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.clear_player);
                builder.setMessage(R.string.clear_player_message);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (urisong != null) {
                            urisong = null;
                            txt_song.setText("");
                            musicSrv.resetPlayer();
                            musicSrv.isLoop(false);

                            txt_song.setText(R.string.pick_song_first);
                            txt_artist.setText("---");
                            txt_song_duration.setText("00:00");
                            txt_song_timing.setText("00:00");
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.create().show();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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
                    String songartist = data.getStringExtra("songartist");
                    String songpath = data.getStringExtra("songpath");
                    String songduration = data.getStringExtra("songduration");


                    Log.i("marta", "song name: " + songname);
                    txt_song.setText(songname);
                    txt_artist.setText(songartist);
                    urisong = Uri.parse(songpath);
                    duration = Integer.valueOf(songduration);

                    txt_song_duration.setText(generateMinutesandSeconds(duration));

                    //inicialitzar els valors i colocar la seekbar a 0
                    seekBar.setProgress(0);
                    seekBar.setMax(duration);

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

    private String generateMinutesandSeconds(Integer millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    //actualitzar  seekbar (i timing)
    private void playCycle() {

        runnable = new Runnable() {
            @Override
            public void run() {
                seekBar.setProgress(musicSrv.getCurrentPosition());
                txt_song_timing.setText(generateMinutesandSeconds(musicSrv.getCurrentPosition()));
                playCycle();
            }
        };
        handler.postDelayed(runnable, 500);
    }

    //inici del bucle
    public void click_start(View view) {
        if (urisong != null) {
            musicSrv.setStartPoint(musicSrv.getCurrentPosition());
        }
    }

    //fi del bucle
    public void click_end(View view) {
        if (urisong != null) {
            musicSrv.setEndPoint(musicSrv.getCurrentPosition());
            musicSrv.isLoop(true);
            musicSrv.replaySongFrom(musicSrv.getStartPoint());
        }
    }

    //eliminar punts del bucle
    public void click_restore(View view) {
        if (urisong != null) {
            musicSrv.setStartPoint(0);
            musicSrv.setEndPoint(duration);
            musicSrv.stopLoop();
            musicSrv.isLoop(false);
        }
    }

    //TODO: ARREGLAR PROBLEMA AMB FORMAT (CONVERSIÓ DE FORMAT??)
}
