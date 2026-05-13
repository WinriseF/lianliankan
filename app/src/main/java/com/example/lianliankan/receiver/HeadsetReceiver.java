package com.example.lianliankan.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.lianliankan.service.MusicService;

public class HeadsetReceiver extends BroadcastReceiver {

    private static final String TAG = "HeadsetReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.HEADSET_PLUG".equals(action)) {
            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    Log.d(TAG, "耳机拔出，暂停背景音乐");
                    Intent pauseIntent = new Intent(context, MusicService.class);
                    pauseIntent.setAction(MusicService.ACTION_PAUSE);
                    context.startService(pauseIntent);
                    break;
                case 1:
                    Log.d(TAG, "耳机插入");
                    break;
            }
        }
    }
}