package edu.upc.eseiaat.onloop.musicplayerv3;

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

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

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
    public void onDestroy() {
        handler.removeCallbacks(runnable);
    }

    @Override
    public void onCreate(){
        //create the service
        super.onCreate();
        //create player
        player = new MediaPlayer();

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

    public void setSong(Uri song){
        urisong=song;
        Log.i("marta", "uri path server: " + urisong.getPath());
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

    public boolean firstPlay() {
        if (player.getCurrentPosition() <= 0) {
            return true;
        }

        return false;
    }

    public void pauseSong() {
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

    public void resetPlayer() {
        player.reset();
    }

    public void seekTo(int i) {
        player.seekTo(i);
    }

    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

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
        //start playback
        if (urisong != null) {
            if (loop) {
                mp.seekTo(start);
                mp.start(); }

            else {
                mp.start(); }
        }
    }

    public void isLoop(boolean loop) {
        this.loop = loop;
    }

    public void setStartPoint(Integer startPoint) {
        start = startPoint;
    }

    public void setEndPoint(Integer endPoint) {
        end = endPoint;
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

    private void playLoop() {

        runnable = new Runnable() {
            @Override
            public void run() {
                if (player.getCurrentPosition() < start || player.getCurrentPosition() > end) {
                    player.seekTo(start);
                }
                playLoop();
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    public void stopLoop(){
        handler.removeCallbacks(runnable);
    }
}


