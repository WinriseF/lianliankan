package com.example.lianliankan.util;

import android.content.Context;
import android.media.MediaPlayer;

import com.example.lianliankan.R;
import com.example.lianliankan.util.PreferenceUtil;

public class SoundManager {

    private Context context;
    private MediaPlayer clickSound;
    private MediaPlayer matchSound;
    private MediaPlayer failSound;
    private MediaPlayer winSound;
    private MediaPlayer loseSound;

    public SoundManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void playClickSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        releaseMediaPlayer(clickSound);
        clickSound = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        if (clickSound != null) {
            clickSound.start();
        }
    }

    public void playMatchSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        releaseMediaPlayer(matchSound);
        matchSound = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        if (matchSound != null) {
            matchSound.start();
        }
    }

    public void playFailSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        releaseMediaPlayer(failSound);
        failSound = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        if (failSound != null) {
            failSound.start();
        }
    }

    public void playWinSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        releaseMediaPlayer(winSound);
        winSound = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        if (winSound != null) {
            winSound.start();
        }
    }

    public void playLoseSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        releaseMediaPlayer(loseSound);
        loseSound = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        if (loseSound != null) {
            loseSound.start();
        }
    }

    public void playDefaultSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        MediaPlayer mp = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        if (mp != null) {
            mp.start();
            mp.setOnCompletionListener(MediaPlayer::release);
        }
    }

    public void release() {
        releaseMediaPlayer(clickSound);
        releaseMediaPlayer(matchSound);
        releaseMediaPlayer(failSound);
        releaseMediaPlayer(winSound);
        releaseMediaPlayer(loseSound);
    }

    private void releaseMediaPlayer(MediaPlayer mp) {
        if (mp != null) {
            try {
                mp.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}