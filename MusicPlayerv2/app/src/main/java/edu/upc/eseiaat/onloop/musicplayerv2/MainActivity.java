package edu.upc.eseiaat.onloop.musicplayerv2;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity  {

    private MediaPlayer mediaPlayer;
    private Button play_btn;
    private int pause;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        play_btn = findViewById(R.id.play_btn);

    }

    public void click_play(View view) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.loonatic);
            mediaPlayer.start();
            play_btn.setText("Pause");
        }

        else {

            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.seekTo(pause);
                mediaPlayer.start();
                play_btn.setText("Pause");

            }

            else {
                mediaPlayer.pause();
                pause = mediaPlayer.getCurrentPosition();
                play_btn.setText("Play");
            }
        }
    }

    //TODO: SERVERS PER REPRODUIR MÚSICA ENCARA QUE SE SURTI DE L'APP
    //Mediaplayer ho para al obrir una altra aplicació o sinó al cap d'una estona si surts de l'app
}