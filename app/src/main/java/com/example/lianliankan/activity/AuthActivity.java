package com.example.lianliankan.activity;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
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

    private ActivityAuthBinding binding;
    private AuthRepository authRepository;
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
        authRepository.login(email, password, accountCallback(R.string.login_success));
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
        authRepository.register(email, password, nickname, accountCallback(R.string.register_success));
    }

    private RepositoryCallback<FirebaseUser> accountCallback(int successMessage) {
        return new RepositoryCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser value) {
                updateAccountState();
                Toast.makeText(AuthActivity.this, successMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(AuthActivity.this,
                        getString(R.string.account_error, error.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        };
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
        return view.getText() == null ? "" : view.getText().toString().trim();
    }
}
