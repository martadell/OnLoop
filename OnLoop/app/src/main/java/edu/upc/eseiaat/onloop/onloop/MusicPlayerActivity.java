package edu.upc.eseiaat.onloop.onloop;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarFinalValueListener;
import com.crystal.crystalrangeseekbar.widgets.BubbleThumbRangeSeekbar;

import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    private Button btn_play, btn_stop;
    private TextView txt_song, txt_artist, txt_song_timing, txt_song_duration, txt_speed, txt_speed_value;
    private Uri urisong;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean stop=true;
    private BubbleThumbRangeSeekbar loopSeekBar;
    private SeekBar songSeekBar, speedSeekBar;
    private Handler handler;
    private Runnable runnable;
    private Integer duration =0;

    private static final int NOTIFICATION_ID = 2904;
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
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    @Override
    protected void onDestroy() {
        if(musicSrv != null){
            stopService(playIntent);
            unbindService(musicConnection);
            musicSrv=null;

            handler.removeCallbacks(runnable);

            if (phoneStateListener != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
            unregisterReceiver(becomingNoisyReceiver);
        }
        super.onDestroy();
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
        btn_stop = findViewById(R.id.btn_stop);
        txt_song = findViewById(R.id.txt_song);
        txt_artist = findViewById(R.id.txt_artist);
        txt_song_timing = findViewById(R.id.txt_song_timing);
        txt_song_duration = findViewById(R.id.txt_song_duration);
        songSeekBar =  findViewById(R.id.songSeekBar);
        loopSeekBar = findViewById(R.id.loopSeekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        txt_speed = findViewById(R.id.txt_speed);
        txt_speed_value = findViewById(R.id.txt_speed_value);

        //crear handler (per la seekbar)
        handler = new Handler();

        registerBecomingNoisyReceiver();
        callStateListener();


        //velocitat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                speedSeekBar.setMax(8);
                speedSeekBar.setProgress(4);
                txt_speed_value.setText("1.0");
                speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        if (urisong != null) {
                        musicSrv.changeplayerSpeed(getConvertedValue(i)); }
                        txt_speed_value.setText(String.valueOf(getConvertedValue(i)));
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
                    // seleccionar cançó
                    musicSrv.setSong(urisong);
                    //i reproduirla
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
            if (musicSrv.isLoop() == true) {
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

    //Menu (crear)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.musiclist_menu, menu);
        return true;
    }

    //Menu (sel·lecció d'items)
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
                            musicSrv.setLoop(false);
                            loopSeekBar.setMinValue(0);
                            loopSeekBar.setMaxValue(duration);

                            txt_song.setText(R.string.nothing_playing);
                            txt_artist.setText("---");
                            txt_song_duration.setText("00:00");
                            txt_song_timing.setText("00:00");
                            speedSeekBar.setProgress(4);
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

    //buscar cançó
    public void performFileSearch() {
        Intent intent = new Intent(this,MusicListActivity.class);
        startActivityForResult(intent, 0);
    }

    //resultat
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

                    txt_song_duration.setText(generateMinutesandSeconds(duration));

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
            btn_play.setText("Play");
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
                            btn_play.setText("Play");
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
        float fl = (float) 0.0;
        fl = .25f * i;
        return fl;
    }
}
