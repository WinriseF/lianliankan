package com.example.lianliankan.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.lianliankan.R;
import com.example.lianliankan.databinding.FragmentSettingsBinding;
import com.example.lianliankan.service.MusicService;
import com.example.lianliankan.util.GameEngine;
import com.example.lianliankan.util.LocaleUtil;
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
        int currentVolume = PreferenceUtil.getMusicVolume(requireContext());
        binding.seekMusicVolume.setValue(currentVolume);
        updateMusicVolumeText(currentVolume);

        setupLanguageToggle();

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
            Toast.makeText(requireContext(), R.string.difficulty_modified, Toast.LENGTH_SHORT).show();
        });

        binding.switchSoundEffects.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceUtil.saveSoundEnabled(requireContext(), isChecked);
            Toast.makeText(requireContext(),
                    isChecked ? R.string.sound_effects_on : R.string.sound_effects_off,
                    Toast.LENGTH_SHORT).show();
        });

        binding.switchBackgroundMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceUtil.saveMusicEnabled(requireContext(), isChecked);
            Toast.makeText(requireContext(),
                    isChecked ? R.string.bg_music_on : R.string.bg_music_off,
                    Toast.LENGTH_SHORT).show();
            if (isChecked) {
                startMusicService(MusicService.ACTION_PLAY);
            } else {
                startMusicService(MusicService.ACTION_STOP);
            }
        });

        binding.seekMusicVolume.addOnChangeListener((slider, value, fromUser) -> {
            int volume = (int) value;
            updateMusicVolumeText(volume);
            if (fromUser) {
                PreferenceUtil.saveMusicVolume(requireContext(), volume);
                startMusicService(MusicService.ACTION_SET_VOLUME);
            }
        });

        binding.btnResetSettings.setOnClickListener(v -> {
            PreferenceUtil.saveDifficulty(requireContext(), GameEngine.DIFFICULTY_EASY);
            PreferenceUtil.saveSoundEnabled(requireContext(), true);
            PreferenceUtil.saveMusicEnabled(requireContext(), true);
            PreferenceUtil.saveMusicVolume(requireContext(), PreferenceUtil.DEFAULT_MUSIC_VOLUME);
            PreferenceUtil.saveLanguage(requireContext(), LocaleUtil.LANGUAGE_EN);
            binding.radioGroupDifficulty.check(R.id.radio_easy);
            binding.switchSoundEffects.setChecked(true);
            binding.switchBackgroundMusic.setChecked(true);
            binding.seekMusicVolume.setValue(PreferenceUtil.DEFAULT_MUSIC_VOLUME);
            binding.languageToggleGroup.check(R.id.btn_language_en);
            updateMusicVolumeText(PreferenceUtil.DEFAULT_MUSIC_VOLUME);
            startMusicService(MusicService.ACTION_PLAY);
            Toast.makeText(requireContext(), R.string.settings_reset, Toast.LENGTH_SHORT).show();
            requireActivity().recreate();
        });
    }

    private void setupLanguageToggle() {
        String currentLang = PreferenceUtil.getLanguage(requireContext());
        binding.languageToggleGroup.check(LocaleUtil.LANGUAGE_ZH.equals(currentLang)
                ? R.id.btn_language_zh
                : R.id.btn_language_en);

        binding.languageToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            String selectedLang = checkedId == R.id.btn_language_zh
                    ? LocaleUtil.LANGUAGE_ZH
                    : LocaleUtil.LANGUAGE_EN;
            if (!selectedLang.equals(PreferenceUtil.getLanguage(requireContext()))) {
                PreferenceUtil.saveLanguage(requireContext(), selectedLang);
                requireActivity().recreate();
            }
        });
    }

    private void updateMusicVolumeText(int volume) {
        binding.tvMusicVolume.setText(volume + "%");
    }

    private void startMusicService(String action) {
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(action);
        intent.putExtra(MusicService.EXTRA_VOLUME, PreferenceUtil.getMusicVolume(requireContext()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && MusicService.ACTION_PLAY.equals(action)) {
            ContextCompat.startForegroundService(requireContext(), intent);
        } else {
            requireContext().startService(intent);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
