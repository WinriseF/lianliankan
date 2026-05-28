package com.example.lianliankan.activity;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lianliankan.R;
import com.example.lianliankan.util.NetworkUtil;
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
        if (NetworkUtil.isNetworkAvailable(this)) {
            webView.loadUrl("file:///android_asset/version_check.html");
        } else {
            Toast.makeText(this, R.string.network_disconnected, Toast.LENGTH_LONG).show();
            webView.loadDataWithBaseURL(
                    null,
                    "<html><body style='font-family:sans-serif;padding:24px;'>"
                            + "<h2>" + getString(R.string.network_unavailable_heading) + "</h2>"
                            + "<p>" + getString(R.string.network_unavailable_desc) + "</p>"
                            + "<p>" + getString(R.string.local_version_info) + "</p>"
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
