package edu.upc.eseiaat.onloop.musicplayerv4;

import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer player;
    private Button play_btn;
    private int position;
    private boolean isPaused;
    private Spinner speedOptions;
    private ArrayAdapter<String> arrayAdapter;
    private String[] speeds() {
        return new String[]{"0.25", "0.5", "0.75", "1.0", "1.25", "1.5", "1.75", "2.0"};
    }

    private SeekBar seekBar;
    private Handler handler;
    private Runnable runnable;

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        play_btn = findViewById(R.id.btn_play);
        speedOptions = findViewById(R.id.speedOptions);

        //Spinner
        arrayAdapter =  new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, speeds());
        speedOptions.setAdapter(arrayAdapter);
        setSpeedOptions();
        speedOptions.setSelection(3);

        handler = new Handler();
        seekBar = findViewById(R.id.seekBar);
    }

    private void setSpeedOptions() {

        speedOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (player != null) {
                    float selectedSpeed = Float.parseFloat(
                            speedOptions.getItemAtPosition(i).toString());

                    //canviar la velocitat a la opció seleccionada
                    changeplayerSpeed(selectedSpeed);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void changeplayerSpeed(float speed) {
        // this checks on API 23 and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
            if (isPaused) {
                player.pause();
                //TODO: No es para???
            }
        }

        else {
            //TODO: BUSCAR ALTERNATIVA
        }
    }

    public void click_play(View view) {
        if (player == null) {
            player = MediaPlayer.create(MainActivity.this, R.raw.loonatic);
            player.setLooping(true);
            player.start();
            play_btn.setText("Pause");
            updateSeekBar();
            isPaused = false;
        }

        else {

            if (isPaused) {
                if (position != seekBar.getProgress()) {
                    player.seekTo(seekBar.getProgress());
                }
                else {
                    player.seekTo(position);
                }

                player.start();
                play_btn.setText("Pause");
                isPaused = false;

            }

            else {
                player.pause();
                position = player.getCurrentPosition();
                play_btn.setText("Play");
                isPaused = true;
            }
        }
    }

    public void updateSeekBar() {

        seekBar.setProgress(0);
        seekBar.setMax(player.getDuration());

        playCycle();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if (b) {
                    player.seekTo(i);
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

    private void playCycle() {

        runnable = new Runnable() {
            @Override
            public void run() {
                seekBar.setProgress(player.getCurrentPosition());
                playCycle();
            }
        };
        handler.postDelayed(runnable, 1000);
    }




}
