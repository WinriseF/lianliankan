package com.example.lianliankan.activity;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lianliankan.R;
import com.example.lianliankan.util.NetworkUtil;

public class VersionCheckActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_version_check);

        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        if (NetworkUtil.isNetworkAvailable(this)) {
            webView.loadUrl("file:///android_asset/version_check.html");
        } else {
            Toast.makeText(this, "网络已断开，当前显示本地版本信息", Toast.LENGTH_LONG).show();
            webView.loadDataWithBaseURL(
                    null,
                    "<html><body style='font-family:sans-serif;padding:24px;'>"
                            + "<h2>网络连接不可用</h2>"
                            + "<p>暂时无法检查在线版本，请恢复网络后重试。</p>"
                            + "<p>本地版本：连连看 v1.0</p>"
                            + "</body></html>",
                    "text/html",
                    "UTF-8",
                    null);
        }

        Button btnUpgrade = findViewById(R.id.btnUpgrade);
        Button btnCancel = findViewById(R.id.btnCancel);

        btnUpgrade.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        btnCancel.setOnClickListener(v -> finish());
    }
}
