package com.example.lianliankan.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.lianliankan.R;
import com.example.lianliankan.activity.GameResultActivity;
import com.example.lianliankan.dao.RankDatabaseHelper;
import com.example.lianliankan.provider.RankContract;
import com.example.lianliankan.util.PreferenceUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GameResultReceiver extends BroadcastReceiver {

    public static final String ACTION_GAME_WIN = "com.example.lianliankan.ACTION_GAME_WIN";
    public static final String ACTION_GAME_RESULT = "com.example.lianliankan.ACTION_GAME_RESULT";
    private static final String TAG = "GameResultReceiver";

    public GameResultReceiver() {
    }

    public GameResultReceiver(Context context) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_GAME_WIN.equals(action)) {
            int score = intent.getIntExtra("score", 0);
            int timeUsed = intent.getIntExtra("time_used", 0);
            int difficulty = intent.getIntExtra("difficulty", 0);

            saveResultToDb(context, "win", score, timeUsed, difficulty);
            showCongratulationsNotification(context, score, timeUsed);

            Intent resultIntent = new Intent(context, GameResultActivity.class);
            resultIntent.putExtra("result", "win");
            resultIntent.putExtra("score", score);
            resultIntent.putExtra("time_used", timeUsed);
            resultIntent.putExtra("difficulty", difficulty);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(resultIntent);

            Log.d(TAG, "收到游戏胜利广播: score=" + score + ", time=" + timeUsed + "s");

        } else if (ACTION_GAME_RESULT.equals(action)) {
            String result = intent.getStringExtra("result");
            int score = intent.getIntExtra("score", 0);
            int timeUsed = intent.getIntExtra("time_used", 0);
            int difficulty = intent.getIntExtra("difficulty", 0);

            if (!"win".equals(result)) {
                saveResultToDb(context, result, score, timeUsed, difficulty);
            }

            Log.d(TAG, "收到游戏结果广播: result=" + result + ", score=" + score);
        }
    }

    public static void saveResultToDb(Context context, String result, int score,
                                      int timeUsed, int difficulty) {
        try {
            RankDatabaseHelper dbHelper = new RankDatabaseHelper(context);
            String playerName = PreferenceUtil.getPlayerName(context);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(RankContract.RankEntry.COLUMN_PLAYER_NAME, playerName);
            values.put(RankContract.RankEntry.COLUMN_SCORE, score);
            values.put(RankContract.RankEntry.COLUMN_TIME_USED, timeUsed);
            values.put(RankContract.RankEntry.COLUMN_DIFFICULTY, difficulty);
            values.put(RankContract.RankEntry.COLUMN_TIMESTAMP, timestamp);

            context.getContentResolver().insert(RankContract.RankEntry.CONTENT_URI, values);
            dbHelper.close();

            Log.d(TAG, "排名数据已保存: " + playerName + ", score=" + score);
        } catch (Exception e) {
            Log.e(TAG, "保存排名失败", e);
        }
    }

    private static void showCongratulationsNotification(Context context, int score, int timeUsed) {
        try {
            Intent notifyIntent = new Intent(context, GameResultActivity.class);
            notifyIntent.putExtra("result", "win");
            notifyIntent.putExtra("score", score);
            notifyIntent.putExtra("time_used", timeUsed);
            notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, notifyIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                            android.app.PendingIntent.FLAG_IMMUTABLE);

            android.app.NotificationManager nm =
                    (android.app.NotificationManager)
                            context.getSystemService(Context.NOTIFICATION_SERVICE);

            String channelId = "game_result";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel =
                        new android.app.NotificationChannel(
                                channelId, "游戏结果",
                                android.app.NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, channelId)
                            .setContentTitle("🎉 恭喜通关！")
                            .setContentText("得分: " + score + " | 用时: " + formatTime(timeUsed))
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

            nm.notify(1001, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "显示通知失败", e);
        }
    }

    private static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }
}