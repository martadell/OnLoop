package edu.upc.eseiaat.onloop.onloop;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private MediaPlayer player;
    private Uri urisong;
    private final IBinder musicBind = new MusicBinder();
    private boolean loop;
    private int start, end;
    private Runnable runnable;
    private Handler handler;

    @Override
    public void onCreate(){
        //crear el servei
        super.onCreate();
        //crear el reproductor
        player = new MediaPlayer();
        //crear el handler (per reproduir el bucle)
        handler = new Handler();

        initMusicPlayer();
    }

    public void initMusicPlayer(){
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //listener per quan el mediaplayer s'hagi preparat
        player.setOnPreparedListener(this);
        //listener per quan la cançó hagi acabat de sonar (en playback)
        player.setOnCompletionListener(this);
        //listener per si hi ha algún error
        player.setOnErrorListener(this);
    }

    //funcions del reproductor
    public void setSong(Uri song){
        urisong=song;
    }

    public void playSong() {
        player.reset();

        try{
            player.setDataSource(getApplicationContext(), urisong);
            player.setLooping(true);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        preparePlayer();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void replaySongFrom(int pos) {
        if (loop) {
            if (player.getCurrentPosition() <= start || player.getCurrentPosition() >= end) {
                playLoop();
            }
        }
        player.seekTo(pos);
        player.start();
    }

    public void stopPlayer() {
        player.stop();
        player.reset();
    }

    public void seekTo(int i) {
        player.seekTo(i);
    }

    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public void resetPlayer() {
        player.reset();
    }

    //service
    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    //al desconectar el servei s'allibera el reproductor
    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(player.getCurrentPosition()>0){
            playSong();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        mediaPlayer.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //iniciar playback
        //si es crea el bucle abans de donar-li al play
        if (urisong != null) {
            if (loop) {
                mp.seekTo(start);
                mp.start(); }

            else {
                mp.start(); }
        }
    }

    public void preparePlayer() {
        try {
            player.prepareAsync();
            if (loop) {
                playLoop(); }
        } catch (Exception e) {
            //Handle exception
        }
    }

    //reproduir bucle
    private void playLoop() {

        runnable = new Runnable() {
            @Override
            public void run() {
                if (player.getCurrentPosition() < getStartPoint() || player.getCurrentPosition() > getEndPoint()) {
                    player.seekTo(getStartPoint());
                }
                playLoop();
            }
        };
        handler.postDelayed(runnable, 500);
    }

    public void stopLoop(){
        handler.removeCallbacks(runnable);
    }


    //getters i setters
    public void isLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean getLoop() {
        return loop;
    }

    public void setStartPoint(Integer startPoint) {
        start = startPoint;
    }

    public Integer getStartPoint() {
        return start;
    }

    public void setEndPoint(Integer endPoint) {
        end = endPoint;
    }

    public Integer getEndPoint() {
        return end;
    }
}