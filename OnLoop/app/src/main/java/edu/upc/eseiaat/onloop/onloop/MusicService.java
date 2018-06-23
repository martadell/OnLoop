package edu.upc.eseiaat.onloop.onloop;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private MediaPlayer player;
    private Uri urisong;
    private String songname, songartist;
    private Integer duration;
    private float speed;
    private IBinder musicBind = new MusicBinder();
    private int start, end;
    private Runnable runnable;
    private Handler handler;
    private boolean binding = false;

    private static final int NOTIFICATION_ID = 2904;
    private static final String CHANNEL_ID = "edu.upc.eseiaat.onloop.onloop.MUSIC_CHANNEL_ID";
    private static final String PLAYER_PAUSEPLAY = "edu.upc.eseiaat.onloop.onloop.pause",
            PLAYER_STOP = "edu.upc.eseiaat.onloop.onloop.stop",
            CLOSE_NOTIF = "edu.upc.eseiaat.onloop.onloop.exit";

    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    @Override
    public void onDestroy() {
        stopForeground(true);

        if (player.isPlaying()) {
            player. stop();
        }

        player.reset();
        player.release();

        closeNotification();

        handler.removeCallbacks(runnable);

        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        unregisterReceiver(becomingNoisyReceiver);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction() != null) {

            if (intent.getAction().equals(PLAYER_PAUSEPLAY)) {

                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.start();
                }

                showNotification();
            }

            if (intent.getAction().equals(PLAYER_STOP)) {
                player.pause();
                player.seekTo(getStartPoint());
                showNotification();
            }

            if (intent.getAction().equals(CLOSE_NOTIF)) {
                player.pause();

                closeNotification();
            }
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        player = new MediaPlayer();
        handler = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        registerBecomingNoisyReceiver();
        callStateListener();

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        binding = true;
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        showNotification();
        binding = false;
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (urisong != null) {
            duration = mp.getDuration();
            setEndPoint(duration);

            if (getStartPoint() > 0 || getEndPoint() < duration){
                mp.seekTo(getStartPoint()); }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    SetSpeedValue(1);
                    mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(speed));
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            onCreate();
        return false;
    }

    public void preparePlayer() {
        try {
            player.prepareAsync();
        } catch (Exception e) {
            player.reset();
        }
    }

    public void setSongParams(String songname, String songartist, Uri urisong) {
        this.urisong = urisong;
        this.songname = songname;
        this.songartist = songartist;

        player.reset();

        try {
            player.setDataSource(getApplicationContext(), this.urisong);
        } catch (IOException e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        player.setLooping(true);
        preparePlayer();
    }

    public void playSong() {
        onPrepared(player);
        player.start();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void replaySongFrom(int pos) {
        if (getStartPoint() > 0 || getEndPoint() < getDuration()) {
            if (player.getCurrentPosition() <= getStartPoint() || player.getCurrentPosition() >= getEndPoint()) {
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

    public void changeplayerSpeed(float speed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (urisong != null) {
                if (player != null) {
                    SetSpeedValue(speed);
                    player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
                }
            }
        }
    }

    public void showNotification() {
        if (urisong != null) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            notificationManager.notify(NOTIFICATION_ID, buildNotification());
        }
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
        // Indicate you want to receive transport controls via your Callback
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (player.isPlaying()) {

                builder.setContentIntent(pendInt)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setSmallIcon(R.mipmap.logo)
                        .setShowWhen(false)
                        .setContentTitle(songname)
                        .setContentText(songartist)
                        .setSubText(getString(R.string.now_playing))
                        .setChannelId(CHANNEL_ID)
                        .setOngoing(true)
                        .setAutoCancel(true)
                        .setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(0, 1, 2))
                        .addAction(R.drawable.spause, "pause", retreivePlaybackAction(0))
                        .addAction(R.drawable.sstop, "stop", retreivePlaybackAction(1))
                        .addAction(R.drawable.scross, "exit", retreivePlaybackAction(2));
            } else {
                builder.setContentIntent(pendInt)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setSmallIcon(R.mipmap.logo)
                        .setShowWhen(false)
                        .setContentTitle(songname)
                        .setContentText(songartist)
                        .setSubText(getString(R.string.now_playing))
                        .setChannelId(CHANNEL_ID)
                        .setOngoing(true)
                        .setAutoCancel(true)
                        .setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(0, 1, 2))
                        .addAction(R.drawable.splay, "pause", retreivePlaybackAction(0))
                        .addAction(R.drawable.sstop, "stop", retreivePlaybackAction(1))
                        .addAction(R.drawable.scross, "exit", retreivePlaybackAction(2));
            }
        }

        else {
            if (player.isPlaying()) {

                builder.setContentIntent(pendInt)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setSmallIcon(R.mipmap.logo)
                        .setShowWhen(false)
                        .setContentTitle(songname)
                        .setContentText(songartist)
                        .setSubText(getString(R.string.now_playing))
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(0, 1, 2))
                        .addAction(R.drawable.spause, "pause", retreivePlaybackAction(0))
                        .addAction(R.drawable.sstop, "stop", retreivePlaybackAction(1))
                        .addAction(R.drawable.scross, "exit", retreivePlaybackAction(2));
            } else {
                builder.setContentIntent(pendInt)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setSmallIcon(R.mipmap.logo)
                        .setShowWhen(false)
                        .setContentTitle(songname)
                        .setContentText(songartist)
                        .setSubText(getString(R.string.now_playing))
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(0, 1, 2))
                        .addAction(R.drawable.splay, "pause", retreivePlaybackAction(0))
                        .addAction(R.drawable.sstop, "stop", retreivePlaybackAction(1))
                        .addAction(R.drawable.scross, "exit", retreivePlaybackAction(2));
            }
        }

            return builder.build();
    }


    private PendingIntent retreivePlaybackAction(int which) {
        Intent action;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(this, MusicService.class);
        switch (which) {
            case 0:
                action = new Intent(PLAYER_PAUSEPLAY);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 0, action, PendingIntent.FLAG_CANCEL_CURRENT);
                return pendingIntent;
            case 1:
                action = new Intent(PLAYER_STOP);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 1, action, PendingIntent.FLAG_CANCEL_CURRENT);
                return pendingIntent;
            case 2:
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
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            player.pause();
            if (!binding) {
                showNotification();
            }

        }
    };

    private void registerBecomingNoisyReceiver() {
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);

    }

    private void callStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (player != null) {
                            player.pause();
                            if (!binding)
                                showNotification();
                        }
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public boolean isPlaying() {
        return player.isPlaying();
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

    public boolean isServicePlayingASong() {
        return urisong != null;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getSongname() {
        return songname;
    }

    public String getSongartist() {
        return songartist;
    }

    public Uri getUrisong() {
        return urisong;
    }

    public float getSpeed() {
        return speed;
    }

    private void SetSpeedValue(float speed) {
        this.speed = speed;
    }

}