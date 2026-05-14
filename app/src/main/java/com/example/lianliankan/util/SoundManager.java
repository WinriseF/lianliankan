package com.example.lianliankan.util;

import android.content.Context;
import android.media.MediaPlayer;

import com.example.lianliankan.R;

public class SoundManager {

    private final Context context;
    private MediaPlayer clickSound;
    private MediaPlayer matchSound;
    private MediaPlayer failSound;
    private MediaPlayer winSound;
    private MediaPlayer loseSound;

    public SoundManager(Context context) {
        this.context = context.getApplicationContext();
        clickSound = createPlayer(R.raw.sound_click);
        matchSound = createPlayer(R.raw.sound_match);
        failSound = createPlayer(R.raw.sound_fail);
        winSound = createPlayer(R.raw.sound_win);
        loseSound = createPlayer(R.raw.sound_lose);
    }

    public void playClickSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        play(clickSound);
    }

    public void playMatchSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        play(matchSound);
    }

    public void playFailSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        play(failSound);
    }

    public void playWinSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        play(winSound);
    }

    public void playLoseSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        play(loseSound);
    }

    public void playDefaultSound() {
        if (!PreferenceUtil.isSoundEnabled(context)) return;
        play(clickSound);
    }

    public void release() {
        releaseMediaPlayer(clickSound);
        releaseMediaPlayer(matchSound);
        releaseMediaPlayer(failSound);
        releaseMediaPlayer(winSound);
        releaseMediaPlayer(loseSound);
        clickSound = null;
        matchSound = null;
        failSound = null;
        winSound = null;
        loseSound = null;
    }

    private MediaPlayer createPlayer(int resId) {
        return MediaPlayer.create(context, resId);
    }

    private void play(MediaPlayer player) {
        if (player == null) return;
        try {
            if (player.isPlaying()) {
                player.pause();
            }
            player.seekTo(0);
            player.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
