package com.example.lianliankan.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lianliankan.databinding.FragmentHelpBinding;

public class HelpFragment extends Fragment {

    private FragmentHelpBinding binding;

    public HelpFragment() {
    }

    public static HelpFragment newInstance() {
        return new HelpFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHelpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.tvRules.setText("点击两个相同动物即可尝试连接。路径只能横向或纵向延伸，最多允许 2 个拐点，且不能穿过未消除的方块。2 分钟内清空全部 36 对动物即可胜利；时间耗尽或无可消除配对则游戏结束。");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
