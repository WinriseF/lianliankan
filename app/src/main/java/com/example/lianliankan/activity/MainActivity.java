package com.example.lianliankan.activity;

import android.Manifest;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.lianliankan.R;
import com.example.lianliankan.databinding.ActivityMainBinding;
import com.example.lianliankan.receiver.GameResultReceiver;
import com.example.lianliankan.util.PreferenceUtil;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_POST_NOTIFICATIONS = 1001;

    private ActivityMainBinding binding;
    private GameResultReceiver gameResultReceiver;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        setupBottomNavigation();
        gameResultReceiver = new GameResultReceiver();
        requestNotificationPermissionIfNeeded();
    }

    private void setupBottomNavigation() {
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_POST_NOTIFICATIONS);
        }
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
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.settingsFragment) {
                binding.bottomNavigation.setSelectedItemId(R.id.settingsFragment);
            }
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
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.gameFragment) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("确认退出")
                    .setMessage("确定要退出游戏吗？当前进度将丢失。")
                    .setPositiveButton("确定", (dialog, which) -> finish())
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            binding.bottomNavigation.setSelectedItemId(R.id.gameFragment);
        }
    }
}
