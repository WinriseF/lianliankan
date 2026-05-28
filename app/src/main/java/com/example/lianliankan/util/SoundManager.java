package com.example.lianliankan.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.example.lianliankan.R;

public class SoundManager {

    private final Context context;
    private final SoundPool soundPool;
    private final int clickSound;
    private final int matchSound;
    private final int failSound;
    private final int winSound;
    private final int loseSound;
    private boolean released;

    public SoundManager(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attributes)
                .build();
        clickSound = soundPool.load(this.context, R.raw.sound_click, 1);
        matchSound = soundPool.load(this.context, R.raw.sound_match, 1);
        failSound = soundPool.load(this.context, R.raw.sound_fail, 1);
        winSound = soundPool.load(this.context, R.raw.sound_win, 1);
        loseSound = soundPool.load(this.context, R.raw.sound_lose, 1);
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
        if (released) return;
        released = true;
        soundPool.release();
    }

    private void play(int soundId) {
        if (released || soundId == 0) return;
        try {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
