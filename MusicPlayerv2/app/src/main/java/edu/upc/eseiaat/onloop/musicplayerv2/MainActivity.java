package edu.upc.eseiaat.onloop.musicplayerv2;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity  {

    private MediaPlayer mediaPlayer;
    private Button btn_play;
    private TextView txt_song;
    private int pause;
    private Uri urisong;
    private int READ_REQUEST_CODE = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_play = findViewById(R.id.btn_play);
        txt_song = findViewById(R.id.txt_song);

    }

    public void click_play(View view) throws IOException {

        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(getApplicationContext(), urisong);
            mediaPlayer.prepare();
            mediaPlayer.start();
            btn_play.setText("Pause");
        }

        else {

            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.seekTo(pause);
                mediaPlayer.start();
                btn_play.setText("Pause");

            }

            else {
                mediaPlayer.pause();
                pause = mediaPlayer.getCurrentPosition();
                btn_play.setText("Play");
            }
        }
    }

    public void click_explore(View view) {
        performFileSearch();
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

                Log.i("marta", "uri path: " + urisong.getPath());

                String path = urisong.getPath();
                String songArray[] = path.split("\\/");
                String song = songArray[songArray.length-1];
                String songnameArray[] = song.split("\\.");
                String songname = songnameArray[songnameArray.length-2];

                txt_song.setText(songname);
            }
        }
    }

    //TODO: SERVERS PER REPRODUIR MÚSICA ENCARA QUE SE SURTI DE L'APP
    //Mediaplayer ho para al obrir una altra aplicació o sinó al cap d'una estona si surts de l'app
}