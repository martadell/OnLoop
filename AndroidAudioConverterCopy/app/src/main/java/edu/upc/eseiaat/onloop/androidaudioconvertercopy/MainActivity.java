package edu.upc.eseiaat.onloop.androidaudioconvertercopy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class MainActivity extends AppCompatActivity {

    private TextView songname;
    private int READ_REQUEST_CODE = 0;
    private File file;
    private Uri uri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        if (file.getName().contains(".mp3")) {

            Log.i("marta", "és mp3");

            AndroidAudioConverter.with(this)
                    .setFile(file)
                    .setFormat(AudioFormat.WAV)
                    .setCallback(callback)
                    .convert();

        }

        else {
            if (file.getName().contains(".wav")) {

                Log.i("marta", "és wav");

                AndroidAudioConverter.with(this)
                        .setFile(file)
                        .setFormat(AudioFormat.MP3)
                        .setCallback(callback)
                        .convert();
            }

            else {
                Log.i("marta", "alguna cosa no funciona :(");
            }
        }
    }

    //TODO: DIU QUE EL FITXER NO EXISTEIX AL CONVERTIR-LO ??? ERROR

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
}
