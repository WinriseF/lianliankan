package com.example.lianliankan.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.lianliankan.R;
import com.example.lianliankan.databinding.ActivityMainBinding;
import com.example.lianliankan.fragment.GameFragment;
import com.example.lianliankan.fragment.HelpFragment;
import com.example.lianliankan.fragment.RankingFragment;
import com.example.lianliankan.fragment.SettingsFragment;
import com.example.lianliankan.receiver.GameResultReceiver;
import com.example.lianliankan.util.PreferenceUtil;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private GameResultReceiver gameResultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new GameFragment())
                    .commit();
        }

        setupBottomNavigation();
        gameResultReceiver = new GameResultReceiver(this);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_game) {
                selectedFragment = new GameFragment();
            } else if (itemId == R.id.nav_help) {
                selectedFragment = new HelpFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            } else if (itemId == R.id.nav_ranking) {
                selectedFragment = new RankingFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceUtil.applySettings(this);
        // 使用 LocalBroadcastManager 注册，与 GameFragment 发送的广播保持一致
        IntentFilter filter = new IntentFilter();
        filter.addAction(GameResultReceiver.ACTION_GAME_WIN);
        filter.addAction(GameResultReceiver.ACTION_GAME_RESULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(gameResultReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gameResultReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_difficulty) {
            // 切换到设置Fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
            binding.bottomNavigation.setSelectedItemId(R.id.nav_settings);
            return true;
        } else if (id == R.id.menu_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("关于连连看")
                .setMessage("连连看 v1.0\n\n移动应用开发技术课程作业\n\n游戏规则：\n• 点击两个相同图案消除\n• 路径最多2个拐点\n• 在2分钟内消除所有配对\n\n难度说明：\n• 简单：10种动物\n• 中等：15种动物\n• 困难：25种动物")
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (current instanceof GameFragment) {
            // 游戏页面按返回键不退出，防止误操作
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("确认退出")
                    .setMessage("确定要退出游戏吗？当前进度将丢失。")
                    .setPositiveButton("确定", (dialog, which) -> finish())
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            super.onBackPressed();
            binding.bottomNavigation.setSelectedItemId(R.id.nav_game);
        }
    }
}