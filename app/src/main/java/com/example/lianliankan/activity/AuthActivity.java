package com.example.lianliankan.activity;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lianliankan.R;
import com.example.lianliankan.databinding.ActivityAuthBinding;
import com.example.lianliankan.repository.AuthRepository;
import com.example.lianliankan.repository.RepositoryCallback;
import com.example.lianliankan.util.LocaleUtil;
import com.example.lianliankan.util.NetworkUtil;
import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {

    private static final String TAG = "AuthActivity";
    private static final long AUTH_TIMEOUT_MS = 20000L;

    private ActivityAuthBinding binding;
    private AuthRepository authRepository;
    private Context localeContext;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int authOperationToken;

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
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        authRepository = new AuthRepository(this);

        binding.btnLogin.setOnClickListener(v -> login());
        binding.btnRegister.setOnClickListener(v -> register());
        binding.btnLogout.setOnClickListener(v -> {
            authRepository.logout();
            updateAccountState();
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
        });
        updateAccountState();
    }

    private void login() {
        if (!ensureNetwork()) return;
        String email = text(binding.etEmail);
        String password = text(binding.etPassword);
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.account_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }
        int token = beginAuthOperation(R.string.login_in_progress);
        Log.d(TAG, "Login requested for " + email);
        authRepository.login(email, password, accountCallback(R.string.login_success, token));
    }

    private void register() {
        if (!ensureNetwork()) return;
        String email = text(binding.etEmail);
        String password = text(binding.etPassword);
        String nickname = text(binding.etNickname);
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.account_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }
        int token = beginAuthOperation(R.string.register_in_progress);
        Log.d(TAG, "Register requested for " + email);
        authRepository.register(email, password, nickname, accountCallback(R.string.register_success, token));
    }

    private RepositoryCallback<FirebaseUser> accountCallback(int successMessage, int token) {
        return new RepositoryCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser value) {
                if (!isCurrentOperation(token)) return;
                finishAuthOperation();
                updateAccountState();
                Toast.makeText(AuthActivity.this, successMessage, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Auth operation succeeded");
                finish();
            }

            @Override
            public void onError(Exception error) {
                if (!isCurrentOperation(token)) return;
                finishAuthOperation();
                String message = error.getMessage() == null
                        ? error.getClass().getSimpleName()
                        : error.getMessage();
                Toast.makeText(AuthActivity.this,
                        getString(R.string.account_error, message),
                        Toast.LENGTH_LONG).show();
                binding.tvAccountStatus.setText(getString(R.string.account_error, message));
                Log.e(TAG, "Auth operation failed", error);
            }
        };
    }

    private int beginAuthOperation(int statusRes) {
        int token = ++authOperationToken;
        setBusy(true);
        binding.tvAccountStatus.setText(statusRes);
        handler.postDelayed(() -> {
            if (!isCurrentOperation(token)) return;
            authOperationToken++;
            setBusy(false);
            binding.tvAccountStatus.setText(R.string.account_timeout);
            Toast.makeText(this, R.string.account_timeout, Toast.LENGTH_LONG).show();
            Log.w(TAG, "Auth operation timed out");
        }, AUTH_TIMEOUT_MS);
        return token;
    }

    private boolean isCurrentOperation(int token) {
        return token == authOperationToken && !isFinishing();
    }

    private void finishAuthOperation() {
        setBusy(false);
        authOperationToken++;
    }

    private void setBusy(boolean busy) {
        binding.progressAuth.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!busy);
        binding.btnRegister.setEnabled(!busy);
        binding.btnLogout.setEnabled(!busy);
    }

    private boolean ensureNetwork() {
        if (!NetworkUtil.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.network_unavailable_desc, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void updateAccountState() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            binding.tvAccountStatus.setText(R.string.account_guest);
        } else {
            String name = user.getDisplayName() == null ? user.getEmail() : user.getDisplayName();
            binding.tvAccountStatus.setText(getString(R.string.account_logged_in, name));
        }
    }

    private String text(android.widget.TextView view) {
        return view.getText().toString().trim();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
