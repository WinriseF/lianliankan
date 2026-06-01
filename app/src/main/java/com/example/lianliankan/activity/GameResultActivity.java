package com.example.lianliankan.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.lianliankan.R;
import com.example.lianliankan.databinding.ActivityGameResultBinding;
import com.example.lianliankan.util.LocaleUtil;

import java.io.File;
import java.io.FileOutputStream;

public class GameResultActivity extends AppCompatActivity {

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

    private ActivityGameResultBinding binding;
    private String shareText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnRestart.setOnClickListener(v -> finish());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnShareResult.setOnClickListener(v -> shareResult());

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            displayDefaultResult();
            return;
        }

        String result = extras.getString("result", "unknown");
        int score = extras.getInt("score", 0);
        int timeUsed = extras.getInt("time_used", 0);
        int difficulty = extras.getInt("difficulty", 0);
        int pairsCleared = extras.getInt("pairs_cleared", 0);

        displayResult(result, score, timeUsed, difficulty, pairsCleared);
    }

    private void displayDefaultResult() {
        binding.tvResultTitle.setText(R.string.game_over);
        binding.tvScore.setText("0");
        binding.tvTimeUsed.setText("00:00");
        binding.tvDifficultyResult.setText(R.string.unknown);
        binding.tvResultDetail.setText("");
        shareText = getString(R.string.share_result_text, 0, "00:00", getString(R.string.unknown));
    }

    private void displayResult(String result, int score, int timeUsed,
                               int difficulty, int pairsCleared) {
        if ("win".equals(result)) {
            binding.tvResultTitle.setText(R.string.game_success);
            binding.tvResultTitle.setTextColor(getResources().getColor(R.color.success_green));
            binding.tvScore.setText(String.valueOf(score));
            binding.tvResultDetail.setText(getString(R.string.result_pairs_cleared_win, pairsCleared));
        } else if ("time_up".equals(result)) {
            binding.tvResultTitle.setText(R.string.time_up);
            binding.tvResultTitle.setTextColor(getResources().getColor(R.color.fail_red));
            binding.tvScore.setText(String.valueOf(score));
            binding.tvResultDetail.setText(getString(R.string.result_pairs_cleared_timeup, pairsCleared));
        } else if ("deadlock".equals(result)) {
            binding.tvResultTitle.setText(R.string.no_linkable_pairs_end);
            binding.tvResultTitle.setTextColor(getResources().getColor(R.color.fail_red));
            binding.tvScore.setText(String.valueOf(score));
            binding.tvResultDetail.setText(R.string.no_pairs_game_over);
        } else {
            binding.tvResultTitle.setText(R.string.game_fail);
            binding.tvResultTitle.setTextColor(getResources().getColor(R.color.fail_red));
            binding.tvScore.setText(String.valueOf(score));
            binding.tvResultDetail.setText(R.string.game_fail);
        }

        int minutes = timeUsed / 60;
        int seconds = timeUsed % 60;
        binding.tvTimeUsed.setText(String.format("%02d:%02d", minutes, seconds));

        int diffTextRes;
        switch (difficulty) {
            case 0: diffTextRes = R.string.easy; break;
            case 1: diffTextRes = R.string.medium; break;
            case 2: diffTextRes = R.string.hard; break;
            default: diffTextRes = R.string.unknown; break;
        }
        binding.tvDifficultyResult.setText(diffTextRes);
        shareText = getString(R.string.share_result_text,
                score,
                binding.tvTimeUsed.getText().toString(),
                binding.tvDifficultyResult.getText().toString());
    }

    private void shareResult() {
        try {
            Bitmap bitmap = createBitmapFromView(binding.infoLayout);
            File imageDir = new File(getCacheDir(), "images");
            if (!imageDir.exists() && !imageDir.mkdirs()) {
                shareTextOnly();
                return;
            }
            File imageFile = new File(imageDir, "lianliankan_result.png");
            try (FileOutputStream out = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.share_result)));
        } catch (Exception e) {
            shareTextOnly();
        }
    }

    private Bitmap createBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void shareTextOnly() {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(intent, getString(R.string.share_result)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
