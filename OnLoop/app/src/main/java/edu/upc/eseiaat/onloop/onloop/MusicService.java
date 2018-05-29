package edu.upc.eseiaat.onloop.onloop;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {


    private static final String PLAYER_PAUSE = "edu.upc.eseiaat.onloop.onloop.pause",
            PLAYER_STOP = "edu.upc.eseiaat.onloop.onloop.stop",
            CLOSE_NOTIF = "edu.upc.eseiaat.onloop.onloop.exit";

    private MediaPlayer player;
    private Uri urisong;
    private String songname, songartist;
    private Integer duration;
    private final IBinder musicBind = new MusicBinder();
    private boolean loop;
    private int start, end;
    private Runnable runnable;
    private Handler handler;

    private static final int NOTIFICATION_ID = 2904;
    private static final String CHANNEL_ID = "edu.upc.eseiaat.onloop.onloop.MUSIC_CHANNEL_ID";
    private MediaSession mediaSession;

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(PLAYER_PAUSE)) {

            if (player.isPlaying()) {
                player.pause();
            } else {
                player.start();
            }

            changeIcon();
        }

        if (intent.getAction().equals(PLAYER_STOP)) {
            player.pause();
            player.seekTo(getStartPoint());

            changeIcon();
        }

        if (intent.getAction().equals(CLOSE_NOTIF)) {
            closeNotification();

            android.os.Process.killProcess(android.os.Process.myPid());
        }

        return Service.START_STICKY;
    }

    private void changeIcon() {
        if (player.isPlaying()) {

        }
    }


    @Override
    public void onCreate(){
        //crear el servei
        super.onCreate();
        //crear el reproductor
        player = new MediaPlayer();
        //crear el handler (per reproduir el bucle)
        handler = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

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
    public void setSongParams(String songname, String songartist, Uri urisong, Integer songduration) {
        this.urisong = urisong;
        this.songname = songname;
        this.songartist = songartist;
        duration = songduration;
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
                if (getCurrentPosition() < getStartPoint() || getCurrentPosition() > getEndPoint()) {
                    seekTo(getStartPoint());
                }
                playLoop();
            }
        };
        handler.postDelayed(runnable, 500);
    }

    //velocitat
    public void changeplayerSpeed(float speed) {
        // this checks on API 23 and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean p = true;
            int pos = 0;

            if (player != null) {
                if (!player.isPlaying()) {
                    p = false;
                    pos = player.getCurrentPosition();
                }
                player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
                if (p == false) {
                    player.stop();
                    player.seekTo(pos);
                    //todo: ?? no va
                }
            }
        }
    }

    //notificació
    public void showNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

    // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID, buildNotification());
    }

    public Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MusicPlayerActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        // Create a new MediaSession
        final MediaSession mediaSession = new MediaSession(this, "MediaSession");
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_ARTIST, songartist)
                .putString(MediaMetadata.METADATA_KEY_MEDIA_URI, urisong.toString())
                .putString(MediaMetadata.METADATA_KEY_TITLE, songname)
                .build());
        // Indicate you're ready to receive media commands
        mediaSession.setActive(true);
        // Attach a new Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSession.Callback() {

            // Implement your callbacks

        });
        // Indicate you want to receive transport controls via your Callback
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);


        builder.setContentIntent(pendInt)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(false)
                .setContentTitle(songname)
                .setContentText(songartist)
                .setSubText("Now playing")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken())
                // Show our playback controls in the compat view
                .setShowActionsInCompactView(0, 1, 2))
                .addAction(android.R.drawable.ic_media_play, "pause", retreivePlaybackAction(0))
                .addAction(android.R.drawable.ic_media_next, "stop", retreivePlaybackAction(1))
                .addAction(android.R.drawable.ic_menu_delete, "exit", retreivePlaybackAction(2));

        return builder.build();
    }


    private PendingIntent retreivePlaybackAction(int which) {
        Intent action;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(this, MusicService.class);
        switch (which) {
            case 0:
                //pause
                action = new Intent(PLAYER_PAUSE);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 0, action, PendingIntent.FLAG_CANCEL_CURRENT);
                return pendingIntent;
            case 1:
                // stop
                action = new Intent(PLAYER_STOP);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 1, action, PendingIntent.FLAG_CANCEL_CURRENT);
                return pendingIntent;
            case 2:
                //tancar
                action = new Intent(CLOSE_NOTIF);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 2, action, PendingIntent.FLAG_CANCEL_CURRENT);
                return pendingIntent;
            default:
                break;
        }
        return null;
    }

    public void closeNotification() {

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    //Per android 8.0 o més
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    //getters i setters
    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isLoop() {
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