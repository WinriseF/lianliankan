package com.example.lianliankan.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.lianliankan.R;
import com.example.lianliankan.activity.MainActivity;

public class MusicService extends Service {

    public static final String ACTION_PLAY = "com.example.lianliankan.PLAY_MUSIC";
    public static final String ACTION_PAUSE = "com.example.lianliankan.PAUSE_MUSIC";
    public static final String ACTION_STOP = "com.example.lianliankan.STOP_MUSIC";
    public static final String ACTION_SET_VOLUME = "com.example.lianliankan.SET_MUSIC_VOLUME";
    public static final String EXTRA_VOLUME = "music_volume";
    public static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private int musicVolume = 70;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            musicVolume = intent.getIntExtra(EXTRA_VOLUME, musicVolume);
            if (ACTION_PLAY.equals(action)) {
                startMusic();
            } else if (ACTION_PAUSE.equals(action)) {
                pauseMusic();
            } else if (ACTION_STOP.equals(action)) {
                stopMusic();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_SET_VOLUME.equals(action)) {
                applyVolume();
            } else {
                // 默认启动播放
                startMusic();
            }
        }
        return START_STICKY;
    }

    private void startMusic() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
                if (mediaPlayer == null) return;
                mediaPlayer.setLooping(true);
                mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
                mediaPlayer.setOnCompletionListener(mp -> {
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                    }
                });
                applyVolume();
            }
            if (!mediaPlayer.isPlaying()) {
                applyVolume();
                mediaPlayer.start();
                showForegroundNotification("背景音乐正在播放", ACTION_PAUSE, "暂停");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pauseMusic() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            showForegroundNotification("背景音乐已暂停", ACTION_PLAY, "继续");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyVolume() {
        if (mediaPlayer == null) return;
        float volume = Math.max(0, Math.min(100, musicVolume)) / 100f;
        mediaPlayer.setVolume(volume, volume);
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
        }
    }

    private void showForegroundNotification(String text, String action, String actionTitle) {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent actionIntent = new Intent(this, MusicService.class);
            actionIntent.setAction(action);
            actionIntent.putExtra(EXTRA_VOLUME, musicVolume);
            PendingIntent actionPendingIntent = PendingIntent.getService(
                    this, action.hashCode(), actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("连连看")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentIntent(pendingIntent)
                    .setOngoing(ACTION_PAUSE.equals(action))
                    .addAction(ACTION_PAUSE.equals(action)
                                    ? android.R.drawable.ic_media_pause
                                    : android.R.drawable.ic_media_play,
                            actionTitle,
                            actionPendingIntent)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "背景音乐", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("连连看背景音乐播放控制");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMusic();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
