package com.example.lianliankan.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.lianliankan.util.GameEngine;

public class PreferenceUtil {

    private static final String PREFS_NAME = "lianliankan_prefs";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";
    private static final String KEY_PLAYER_NAME = "player_name";

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
        // 全局设置应用入口
        boolean musicEnabled = isMusicEnabled(context);
        // 可在此处控制全局音乐行为
    }
}