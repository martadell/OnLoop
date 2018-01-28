package edu.upc.eseiaat.onloop.musicplayerv1;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity  {

    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void click_play(View view) {
        if (mediaPlayer != null){
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.loonatic);
        mediaPlayer.start();
    }
}
