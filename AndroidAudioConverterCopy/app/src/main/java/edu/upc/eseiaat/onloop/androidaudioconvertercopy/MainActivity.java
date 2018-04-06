package edu.upc.eseiaat.onloop.androidaudioconvertercopy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class MainActivity extends AppCompatActivity {

    private TextView songname;
    private int READ_REQUEST_CODE = 0;
    private File file;
    private MediaPlayer mediaPlayer;
    private Button play_btn;
    private int pause;
    private Uri uri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        play_btn = findViewById(R.id.btn_play);
        songname = findViewById(R.id.txt_songname);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(getResources().getColor(R.color.colorPrimaryDark)));
        }

        Util.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        Util.requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public void convertAudio(View v){
        /**
         *  Update with a valid audio file!
         *  Supported formats: {@link AndroidAudioConverter.AudioFormat}
         */

        File finalfile = file.getAbsoluteFile();

        IConvertCallback callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                Toast.makeText(MainActivity.this, "SUCCESS: " + convertedFile.getPath(), Toast.LENGTH_LONG).show();
            }
            @Override
            public void onFailure(Exception error) {
                Toast.makeText(MainActivity.this, "ERROR: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
        Toast.makeText(this, "Converting audio file...", Toast.LENGTH_SHORT).show();

        if (finalfile.getName().contains(".mp3")) {

            Log.i("marta", "és mp3");

            AndroidAudioConverter.with(this)
                    .setFile(finalfile)
                    .setFormat(AudioFormat.WAV)
                    .setCallback(callback)
                    .convert();

        }

        else {
            if (finalfile.getName().contains(".wav")) {

                Log.i("marta", "és wav");

                AndroidAudioConverter.with(this)
                        .setFile(finalfile)
                        .setFormat(AudioFormat.MP3)
                        .setCallback(callback)
                        .convert();
            }

            else {
                Log.i("marta", "alguna cosa no funciona :(");
            }
        }
    }

    public void clic_song(View view) {
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
            uri = null;

            if (resultData != null) {
                uri = resultData.getData();

                file = new File (uri.getPath());

                Log.i("marta", "uri path: " + uri.getPath() + ", filename: " + file.getName());
                songname.setText(file.getName());
            }
        }
    }

    public void clic_play(View view) throws IOException {

        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepare();
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
}


