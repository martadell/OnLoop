package edu.upc.eseiaat.onloop.musicplayerv2;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private MediaPlayer player;
    private int songPos;
    private Uri urisong;
    private final IBinder musicBind = new MusicBinder();

    @Override
    public void onCreate(){
        //create the service
        super.onCreate();
        //initialize position
        songPos=0;
        //create player
        player = new MediaPlayer();

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

    public void playSong(){
        player.reset();

        try{
            player.setDataSource(getApplicationContext(), urisong);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        player.prepareAsync();
    }

    public boolean firstPlay() {
        if (player.getCurrentPosition() <= 0) {
            return true;
        }

        return false;
    }

    public void pauseSong() {
        player.pause();
        songPos = player.getCurrentPosition();
    }

    public void replaySong() {
        player.seekTo(songPos);
        player.start();
    }

    public void stop() {
        player.stop();
    }

    public void resetPlayer() {
        player.reset();
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
            mp.start();
        }

    }

}
