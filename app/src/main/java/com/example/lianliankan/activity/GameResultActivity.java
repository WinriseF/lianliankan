package com.example.lianliankan.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lianliankan.R;
import com.example.lianliankan.databinding.ActivityGameResultBinding;
import com.example.lianliankan.util.PreferenceUtil;

public class GameResultActivity extends AppCompatActivity {

    private ActivityGameResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        binding.btnRestart.setOnClickListener(v -> {
            finish();
        });

        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void displayDefaultResult() {
        binding.tvResultTitle.setText("游戏结束");
        binding.tvScore.setText("0");
        binding.tvTimeUsed.setText("00:00");
        binding.tvDifficultyResult.setText("未知");
        binding.tvResultDetail.setText("");
    }

    private void displayResult(String result, int score, int timeUsed,
                               int difficulty, int pairsCleared) {
        if ("win".equals(result)) {
            binding.tvResultTitle.setText(R.string.game_success);
            binding.tvResultTitle.setTextColor(getResources().getColor(R.color.success_green));
            binding.tvScore.setText(String.valueOf(score));
            binding.tvResultDetail.setText("共消除 " + pairsCleared + " 对动物");
        } else if ("time_up".equals(result)) {
            binding.tvResultTitle.setText(R.string.time_up);
            binding.tvResultTitle.setTextColor(getResources().getColor(R.color.fail_red));
            binding.tvScore.setText(String.valueOf(score));
            binding.tvResultDetail.setText("时间耗尽！共消除 " + pairsCleared + " 对");
        } else if ("deadlock".equals(result)) {
            binding.tvResultTitle.setText("无可消除配对");
            binding.tvResultTitle.setTextColor(getResources().getColor(R.color.fail_red));
            binding.tvScore.setText(String.valueOf(score));
            binding.tvResultDetail.setText("无可消除配对，游戏结束");
        } else {
            binding.tvResultTitle.setText(R.string.game_fail);
            binding.tvResultTitle.setTextColor(getResources().getColor(R.color.fail_red));
            binding.tvScore.setText(String.valueOf(score));
            binding.tvResultDetail.setText("游戏失败");
        }

        int minutes = timeUsed / 60;
        int seconds = timeUsed % 60;
        binding.tvTimeUsed.setText(String.format("%02d:%02d", minutes, seconds));

        String diffText;
        switch (difficulty) {
            case 0: diffText = "简单"; break;
            case 1: diffText = "中等"; break;
            case 2: diffText = "困难"; break;
            default: diffText = "未知"; break;
        }
        binding.tvDifficultyResult.setText(diffText);
    }
}
