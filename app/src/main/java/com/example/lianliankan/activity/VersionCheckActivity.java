package com.example.lianliankan.activity;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lianliankan.R;
import com.example.lianliankan.util.LocaleUtil;

public class VersionCheckActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_version_check);

        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/version_check.html");

        Button btnUpgrade = findViewById(R.id.btnUpgrade);
        Button btnCancel = findViewById(R.id.btnCancel);

        btnUpgrade.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        btnCancel.setOnClickListener(v -> finish());
    }
}
