package com.example.lianliankan.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.example.lianliankan.service.MusicService;
import com.example.lianliankan.util.LocaleUtil;
import com.example.lianliankan.util.PreferenceUtil;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_GAME = "open_game";
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;

    private ActivityMainBinding binding;
    private GameResultReceiver gameResultReceiver;
    private NavController navController;
    private Context localeContext;

    @Override
    protected void attachBaseContext(Context newBase) {
        localeContext = LocaleUtil.attachBaseContext(newBase);
        super.attachBaseContext(localeContext);
    }

    @Override
    public Resources getResources() {
        if (localeContext != null) {
            return localeContext.getResources();
        }
        return super.getResources();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        setupBottomNavigation();
        gameResultReceiver = new GameResultReceiver();
        requestNotificationPermissionIfNeeded();
        handleNavigationIntent(getIntent());
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
        IntentFilter filter = new IntentFilter();
        filter.addAction(GameResultReceiver.ACTION_GAME_WIN);
        filter.addAction(GameResultReceiver.ACTION_GAME_RESULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(gameResultReceiver, filter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
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
        } else if (id == R.id.menu_account) {
            startActivity(new Intent(this, AuthActivity.class));
            return true;
        } else if (id == R.id.menu_battle) {
            startActivity(new Intent(this, BattleActivity.class));
            return true;
        } else if (id == R.id.menu_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void handleNavigationIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_OPEN_GAME, false)) {
            binding.bottomNavigation.setSelectedItemId(R.id.gameFragment);
        }
    }

    @Override
    public void onBackPressed() {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.gameFragment) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_exit_title)
                .setMessage(R.string.confirm_exit_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> exitGame())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            binding.bottomNavigation.setSelectedItemId(R.id.gameFragment);
        }
    }

    private void exitGame() {
        Intent stopMusicIntent = new Intent(this, MusicService.class);
        stopMusicIntent.setAction(MusicService.ACTION_STOP);
        startService(stopMusicIntent);
        finish();
    }
}
