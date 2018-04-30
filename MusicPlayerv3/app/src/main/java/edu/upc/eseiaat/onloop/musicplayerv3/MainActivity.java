package edu.upc.eseiaat.onloop.musicplayerv3;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Button btn_play, btn_explore, btn_stop;
    private TextView txt_song;
    private Uri urisong;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false, pause=true;

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
        btn_stop = findViewById(R.id.btn_stop);
        txt_song = findViewById(R.id.txt_song);

        btn_play.setVisibility(View.GONE);
        btn_stop.setVisibility(View.GONE);
        txt_song.setText("Nothing! Choose a song to listen");
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

            if (pause == true) { //si no s'està reproduïnt res

                if (musicSrv.firstPlay()) { //si és el primer cop que es reprodueix

                    // seleccionar cançó
                    musicSrv.setSong(urisong);
                    //i reproduirla
                    musicSrv.playSong();
                } else {

                    musicSrv.replaySong();
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

        btn_play.setVisibility(view.GONE);
        btn_stop.setVisibility(view.GONE);
        btn_explore.setVisibility(View.VISIBLE);

        urisong = null;
        txt_song.setText("");
        musicSrv.resetPlayer();

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
                    String songname = data.getStringExtra("songname");
                    String songpath = data.getStringExtra("songpath");

                    Log.i("marta", "song name: " + songname);
                    txt_song.setText(songname);
                    urisong = Uri.parse(songpath);

                    btn_play.setVisibility(View.VISIBLE);
                    btn_stop.setVisibility(View.VISIBLE);
                }
        }
    }

}
