package edu.upc.eseiaat.onloop.musicplayerv2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  {

    private Button btn_play, btn_explore, btn_stop;
    private TextView txt_song;
    private Uri urisong;
    private int READ_REQUEST_CODE = 0;
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

    public void click_explore(View view) {
        performFileSearch();
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

    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("audio/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            urisong = null;

            if (resultData != null) {
                urisong = resultData.getData();

                musicSrv.stop();
                musicSrv.setSong(urisong);

                String songname = getSongName();

                Log.i("marta", "song name: " + songname);
                txt_song.setText(songname);

                btn_play.setVisibility(View.VISIBLE);
                btn_stop.setVisibility(View.VISIBLE);
            }
        }
    }

    private String getSongName() {
        String path = urisong.getPath();
        String songArray[] = path.split("\\/");
        String song = songArray[songArray.length-1];
        String songnameArray[] = song.split("\\.");
        return songnameArray[songnameArray.length-2];
    }

    //TODO: MIRAR QUE AL MARXAR DE L'APP AMB EL BOTÓ "BACK" NO DESTRUEIXI EL SERVEI
    // (onStartCommand en comptes de onBind???)
}