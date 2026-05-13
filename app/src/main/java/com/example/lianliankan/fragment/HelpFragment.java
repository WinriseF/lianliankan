package com.example.lianliankan.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lianliankan.R;

public class HelpFragment extends Fragment {

    public HelpFragment() {
    }

    public static HelpFragment newInstance() {
        return new HelpFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        WebView webView = new WebView(requireContext());
        webView.getSettings().setJavaScriptEnabled(true);
        String html = "<html><body style='padding:16px;color:#333;font-family:sans-serif;'>"
                + "<h2 style='color:#FF6F00;'>连连看游戏规则</h2><br>"
                + "<b>基本玩法：</b><br>"
                + "点击两个相同图案的动物进行消除<br>"
                + "连接路径最多允许2个拐点（即最多3段直线）<br>"
                + "路径不能穿过其他未消除的动物方块<br>"
                + "路径可以沿着棋盘外边框绕行<br><br>"
                + "<b>难度说明：</b><br>"
                + "简单：10种动物，容易区分<br>"
                + "中等：15种动物，中等难度<br>"
                + "困难：25种动物，极具挑战<br><br>"
                + "<b>胜利条件：</b><br>"
                + "在限定时间内消除所有动物配对<br><br>"
                + "<b>失败条件：</b><br>"
                + "时间耗尽（2分钟倒计时结束）<br>"
                + "剩余配对无法连接（无可消除配对）<br><br>"
                + "<b>小贴士：</b><br>"
                + "优先消除边缘的配对，打开更多通路<br>"
                + "善用打乱重排功能应对死局<br>"
                + "合理规划路线，避免阻断其他配对<br>"
                + "</body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        return webView;
    }
}