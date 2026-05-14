package com.example.lianliankan.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lianliankan.R;
import com.example.lianliankan.databinding.FragmentSettingsBinding;
import com.example.lianliankan.service.MusicService;
import com.example.lianliankan.util.GameEngine;
import com.example.lianliankan.util.PreferenceUtil;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    public SettingsFragment() {
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        // 加载当前设置
        int currentDifficulty = PreferenceUtil.getDifficulty(requireContext());
        switch (currentDifficulty) {
            case GameEngine.DIFFICULTY_EASY:
                binding.radioGroupDifficulty.check(R.id.radio_easy);
                break;
            case GameEngine.DIFFICULTY_MEDIUM:
                binding.radioGroupDifficulty.check(R.id.radio_medium);
                break;
            case GameEngine.DIFFICULTY_HARD:
                binding.radioGroupDifficulty.check(R.id.radio_hard);
                break;
        }

        binding.switchSoundEffects.setChecked(PreferenceUtil.isSoundEnabled(requireContext()));
        binding.switchBackgroundMusic.setChecked(PreferenceUtil.isMusicEnabled(requireContext()));

        // 难度切换
        binding.radioGroupDifficulty.setOnCheckedChangeListener((group, checkedId) -> {
            int difficulty;
            if (checkedId == R.id.radio_easy) {
                difficulty = GameEngine.DIFFICULTY_EASY;
            } else if (checkedId == R.id.radio_medium) {
                difficulty = GameEngine.DIFFICULTY_MEDIUM;
            } else {
                difficulty = GameEngine.DIFFICULTY_HARD;
            }
            PreferenceUtil.saveDifficulty(requireContext(), difficulty);
            Toast.makeText(requireContext(), "难度已修改，下次游戏生效", Toast.LENGTH_SHORT).show();
        });

        // 音效开关
        binding.switchSoundEffects.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceUtil.saveSoundEnabled(requireContext(), isChecked);
            Toast.makeText(requireContext(),
                    isChecked ? "音效已开启" : "音效已关闭", Toast.LENGTH_SHORT).show();
        });

        // 背景音乐开关
        binding.switchBackgroundMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceUtil.saveMusicEnabled(requireContext(), isChecked);
            Toast.makeText(requireContext(),
                    isChecked ? "背景音乐已开启" : "背景音乐已关闭", Toast.LENGTH_SHORT).show();
            if (isChecked) {
                startMusicService(MusicService.ACTION_PLAY);
            } else {
                startMusicService(MusicService.ACTION_PAUSE);
            }
        });

        // 重置设置
        binding.btnResetSettings.setOnClickListener(v -> {
            PreferenceUtil.saveDifficulty(requireContext(), GameEngine.DIFFICULTY_EASY);
            PreferenceUtil.saveSoundEnabled(requireContext(), true);
            PreferenceUtil.saveMusicEnabled(requireContext(), true);
            binding.radioGroupDifficulty.check(R.id.radio_easy);
            binding.switchSoundEffects.setChecked(true);
            binding.switchBackgroundMusic.setChecked(true);
            Toast.makeText(requireContext(), "已恢复默认设置", Toast.LENGTH_SHORT).show();
        });
    }

    private void startMusicService(String action) {
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(action);
        requireContext().startService(intent);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }
}
