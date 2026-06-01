package com.example.lianliankan.activity;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lianliankan.R;
import com.example.lianliankan.databinding.ActivityBattleBinding;
import com.example.lianliankan.repository.BattleRepository;
import com.example.lianliankan.repository.RepositoryCallback;
import com.example.lianliankan.util.LocaleUtil;
import com.example.lianliankan.util.PreferenceUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

public class BattleActivity extends AppCompatActivity {

    private ActivityBattleBinding binding;
    private BattleRepository battleRepository;
    private ListenerRegistration battleRegistration;
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
        binding = ActivityBattleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        battleRepository = new BattleRepository(this);

        binding.btnCreateRoom.setOnClickListener(v -> createRoom());
        binding.btnJoinRoom.setOnClickListener(v -> joinRoom());
        binding.btnLeaveRoom.setOnClickListener(v -> {
            battleRepository.clearActiveBattle();
            updateStaticStatus();
        });
        updateStaticStatus();
        listenActiveRoom();
    }

    private void createRoom() {
        int difficulty = PreferenceUtil.getDifficulty(this);
        battleRepository.createRoom(difficulty, roomCallback(R.string.room_created));
    }

    private void joinRoom() {
        String roomId = binding.etRoomCode.getText() == null
                ? ""
                : binding.etRoomCode.getText().toString().trim();
        if (roomId.isEmpty()) {
            Toast.makeText(this, R.string.room_code_required, Toast.LENGTH_SHORT).show();
            return;
        }
        battleRepository.joinRoom(roomId, roomCallback(R.string.room_joined));
    }

    private RepositoryCallback<String> roomCallback(int successMessage) {
        return new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String roomId) {
                binding.etRoomCode.setText(roomId);
                Toast.makeText(BattleActivity.this, successMessage, Toast.LENGTH_SHORT).show();
                listenActiveRoom();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(BattleActivity.this,
                        getString(R.string.battle_error, error.getMessage()),
                        Toast.LENGTH_LONG).show();
                updateStaticStatus();
            }
        };
    }

    private void listenActiveRoom() {
        if (battleRegistration != null) {
            battleRegistration.remove();
            battleRegistration = null;
        }
        battleRegistration = battleRepository.listenActiveBattle(new BattleRepository.BattleListener() {
            @Override
            public void onBattleChanged(DocumentSnapshot snapshot) {
                updateRoomStatus(snapshot);
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(BattleActivity.this,
                        getString(R.string.battle_error, error.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
        updateStaticStatus();
    }

    private void updateRoomStatus(DocumentSnapshot snapshot) {
        String roomId = snapshot.getId();
        String status = snapshot.getString("status");
        Long p1Score = snapshot.getLong("player1Score");
        Long p2Score = snapshot.getLong("player2Score");
        binding.tvBattleStatus.setText(getString(
                R.string.battle_status,
                roomId,
                status == null ? "-" : status,
                p1Score == null ? 0 : p1Score,
                p2Score == null ? 0 : p2Score));
    }

    private void updateStaticStatus() {
        if (battleRepository.hasActiveBattle()) {
            binding.tvBattleStatus.setText(getString(
                    R.string.battle_active_room,
                    battleRepository.getActiveRoomId()));
        } else if (battleRepository.canUseOnlineBattle()) {
            binding.tvBattleStatus.setText(R.string.battle_ready);
        } else {
            binding.tvBattleStatus.setText(R.string.battle_requires_login);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (battleRegistration != null) {
            battleRegistration.remove();
            battleRegistration = null;
        }
    }
}
