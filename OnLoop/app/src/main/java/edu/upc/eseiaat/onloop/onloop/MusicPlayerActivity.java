package edu.upc.eseiaat.onloop.onloop;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarFinalValueListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    private ImageButton btn_play;
    private Button btn_explore;
    private TextView txt_song, txt_artist, txt_song_timing, txt_loop_start, txt_loop_end,
            txt_speed, txt_speed_value;
    private Uri urisong;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean stop=true, doubleBackToExitPressedOnce=false;
    private CrystalRangeSeekbar loopSeekBar;
    private SeekBar songSeekBar, speedSeekBar;
    private Handler handler;
    private Runnable runnable;
    private Integer duration =0;

    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

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
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        outState.putString("songname", txt_song.getText().toString());
        outState.putString("songartist", txt_artist.getText().toString());
        outState.putInt("duration", duration);
        outState.putInt("position", musicSrv.getCurrentPosition());
        outState.putInt("startLoop", musicSrv.getStartPoint());
        outState.putString("urisong", urisong.toString());
        Log.i("marta", urisong.toString());
        outState.putInt("speed", speedSeekBar.getProgress());
    }

    @Override
    protected void onStop() {
        if (urisong != null) {
            musicSrv.showNotification();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(musicSrv != null){
            stopService(playIntent);
            unbindService(musicConnection);

            handler.removeCallbacks(runnable);

            if (phoneStateListener != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
            unregisterReceiver(becomingNoisyReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.back_exit, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        //permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK},1);

                return;
            }}

        //inicialitzar
        btn_play = findViewById(R.id.btn_play);
        btn_explore = findViewById(R.id.btn_explore);
        txt_song = findViewById(R.id.txt_song);
        txt_artist = findViewById(R.id.txt_artist);
        txt_song_timing = findViewById(R.id.txt_song_timing);
        txt_loop_start = findViewById(R.id.txt_loop_start);
        txt_loop_end = findViewById(R.id.txt_loop_end);
        songSeekBar =  findViewById(R.id.songSeekBar);
        loopSeekBar = findViewById(R.id.loopSeekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        txt_speed = findViewById(R.id.txt_speed);
        txt_speed_value = findViewById(R.id.txt_speed_value);

        btn_explore.setVisibility(View.GONE);

        //crear handler (per la seekbar)
        handler = new Handler();

        registerBecomingNoisyReceiver();
        callStateListener();


        //velocitat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                speedSeekBar.setMax(10);
                speedSeekBar.setProgress(5);
                txt_speed_value.setText("1.0");
                speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        if (urisong != null) {
                            musicSrv.changeplayerSpeed(getConvertedValue(i)); }
                        if (i != 9) {
                            txt_speed_value.setText(String.valueOf(getConvertedValue(i))); }
                        else {
                            txt_speed_value.setText("1.4");
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

            else  {
            speedSeekBar.setVisibility(View.GONE);
            txt_speed.setVisibility(View.GONE);
            txt_speed_value.setVisibility(View.GONE);
        }

        if(savedInstanceState != null) {
            Bundle state = savedInstanceState;
            txt_song.setText(state.getString("songname"));
            txt_artist.setText(state.getString("songartist"));
            songSeekBar.setProgress(state.getInt("position"));
            loopSeekBar.setMinStartValue(state.getInt("startLoop"));
            loopSeekBar.setMinStartValue(state.getInt("endLoop"));
            duration = state.getInt("duration");
            speedSeekBar.setProgress(state.getInt("speed"));
            urisong = Uri.parse(state.getString("urisong"));
        }

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

    //Buttons
    public void click_play(View view){

        if (urisong != null) {

            //actualitzar seekbar
            playCycle();

            if (stop) { //si no s'està reproduïnt res

                if (musicSrv.getCurrentPosition() == 0) { //si és el primer cop que es reprodueix (està a 0)
                    //reproduir cançó
                    musicSrv.playSong();
                } else {
                    if (songSeekBar.getProgress() != musicSrv.getCurrentPosition()) { //actualitzar cançó
                        // si es canvia la posició a la seekbar
                        musicSrv.replaySongFrom(songSeekBar.getProgress());
                    }

                    else {
                        musicSrv.replaySongFrom(musicSrv.getCurrentPosition());
                   }
                }

                btn_play.setBackgroundResource(android.R.drawable.ic_media_pause);
                stop = false;

            } else {

                musicSrv.pausePlayer(); //pause
                btn_play.setBackgroundResource(android.R.drawable.ic_media_play);
                stop = true;

            }
        }

    }

    public void click_stop(View view) {
        if (urisong != null) {
            if (musicSrv.isLoop() == true) {
                musicSrv.pausePlayer();
                musicSrv.seekTo(musicSrv.getStartPoint());
            }

            else {
                musicSrv.resetPlayer();
            }

            if (!stop) {
                btn_play.setBackgroundResource(android.R.drawable.ic_media_play);
                stop = true;
            }
        }
    }

    public void click_explore(View view) {
        if (urisong == null) {
            performFileSearch();
        }
    }

    public void click_explore2(View view) {
            performFileSearch();
    }

    //buscar cançó
    public void performFileSearch() {
        Intent intent = new Intent(this,MusicListActivity.class);
        startActivityForResult(intent, 0);
    }

    //resultat
    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    //obtenir dades
                    String songname = data.getStringExtra("songname");
                    String songartist = data.getStringExtra("songartist");
                    String songpath = data.getStringExtra("songpath");
                    String songduration = data.getStringExtra("songduration");

                    txt_song.setText(songname);
                    txt_artist.setText(songartist);
                    urisong = Uri.parse(songpath);
                    duration = Integer.valueOf(songduration);

                    musicSrv.setSongParams(songname, songartist, urisong, duration);

                    btn_explore.setVisibility(View.VISIBLE);

                    txt_loop_end.setText(generateMinutesandSeconds(duration));

                    //inicialitzar els valors i colocar la seekbar de la cançó a 0
                    songSeekBar.setMax(duration);

                    songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                            if (musicSrv.isLoop()) {
                                if (i < musicSrv.getStartPoint() || i > musicSrv.getEndPoint()) {
                                    //si es mou per fora del bucle
                                    musicSrv.seekTo(musicSrv.getStartPoint());
                                }

                                else {
                                    if (b) {
                                        musicSrv.seekTo(i);
                                    }
                                }
                            }

                            else {
                                if (b) { //si el canvi ha estat fet per l'usuari
                                    musicSrv.seekTo(i);
                                }
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

                    //el mateix per la rangeSeekBar
                   loopSeekBar.setMinValue(0);
                   loopSeekBar.setMaxValue(duration);

                   loopSeekBar.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener() {
                       @Override
                       public void valueChanged(Number minValue, Number maxValue) {
                           //valors txt canvi
                           txt_loop_start.setText(generateMinutesandSeconds(minValue.intValue()));
                           txt_loop_end.setText(generateMinutesandSeconds(maxValue.intValue()));
                       }
                   });

                   loopSeekBar.setOnRangeSeekbarFinalValueListener(new OnRangeSeekbarFinalValueListener() {
                       @Override
                       public void finalValue(Number minValue, Number maxValue) {
                           musicSrv.setStartPoint(minValue.intValue());
                           musicSrv.setEndPoint(maxValue.intValue());
                           musicSrv.setLoop(true);

                           //si s'està reproduint fora del bucle es posa a dins
                           if (musicSrv.getCurrentPosition() < minValue.intValue() || musicSrv.getCurrentPosition() > maxValue.intValue()) {
                               musicSrv.seekTo(minValue.intValue());
                           }

                           //eliminar bucle
                           if (minValue.intValue() == 0 && maxValue == duration) {
                               musicSrv.setLoop(false);
                           }
                       }
                   });
                }
        }
    }

    //Generar temps en format 00:00
    private String generateMinutesandSeconds(Integer millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    //Actualitzar  seekbar (i timing)
    private void playCycle() {

        runnable = new Runnable() {
            @Override
            public void run() {
                songSeekBar.setProgress(musicSrv.getCurrentPosition());
                txt_song_timing.setText(generateMinutesandSeconds(musicSrv.getCurrentPosition()));
                playCycle();
            }
        };
        handler.postDelayed(runnable, 500);
    }

    //auriculars
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            musicSrv.pausePlayer();
            btn_play.setBackgroundResource(android.R.drawable.ic_media_play);
            stop = true;
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    //trucades
    private void callStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //si al menys hi h una trucada durant la reproducció
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (musicSrv != null) {
                            musicSrv.pausePlayer();
                            btn_play.setBackgroundResource(android.R.drawable.ic_media_play);
                            stop = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (musicSrv != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                musicSrv.seekTo(songSeekBar.getProgress());
                            }
                        }
                        break;
                }
            }
        };
        //registrar el listener amb el telephony manager i "escoltar" canvis
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public float getConvertedValue(int i) {
        float fl = (float) 0.5;
        fl = fl + .10f * i;
        return fl;
    }
}
