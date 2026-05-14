package com.example.lianliankan.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.example.lianliankan.service.MusicService;

public class PreferenceUtil {

    private static final String PREFS_NAME = "lianliankan_prefs";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";
    private static final String KEY_MUSIC_VOLUME = "music_volume";
    private static final String KEY_PLAYER_NAME = "player_name";
    public static final int DEFAULT_MUSIC_VOLUME = 70;

    public static void saveDifficulty(Context context, int difficulty) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_DIFFICULTY, difficulty);
        editor.apply();
    }

    public static int getDifficulty(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_DIFFICULTY, GameEngine.DIFFICULTY_EASY);
    }

    public static void saveSoundEnabled(Context context, boolean enabled) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_SOUND_ENABLED, enabled);
        editor.apply();
    }

    public static boolean isSoundEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public static void saveMusicEnabled(Context context, boolean enabled) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_MUSIC_ENABLED, enabled);
        editor.apply();
    }

    public static boolean isMusicEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_MUSIC_ENABLED, true);
    }

    public static void saveMusicVolume(Context context, int volume) {
        int safeVolume = Math.max(0, Math.min(100, volume));
        SharedPreferences.Editor editor = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_MUSIC_VOLUME, safeVolume);
        editor.apply();
    }

    public static int getMusicVolume(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME);
    }

    public static void savePlayerName(Context context, String name) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_PLAYER_NAME, name);
        editor.apply();
    }

    public static String getPlayerName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PLAYER_NAME, "玩家");
    }

    public static void applySettings(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.putExtra(MusicService.EXTRA_VOLUME, getMusicVolume(context));
        if (isMusicEnabled(context)) {
            intent.setAction(MusicService.ACTION_PLAY);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent);
            } else {
                context.startService(intent);
            }
        } else {
            intent.setAction(MusicService.ACTION_PAUSE);
            context.startService(intent);
        }
    }
}
