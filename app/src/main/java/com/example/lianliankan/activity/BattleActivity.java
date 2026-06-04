package com.example.lianliankan.activity;

import android.content.Context;
import android.content.Intent;
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
    private String currentRoomStatus;

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
        binding.btnStartBattle.setOnClickListener(v -> startBattleAndOpenGame());
        binding.btnLeaveRoom.setOnClickListener(v -> leaveRoom());
        binding.btnBattleLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AuthActivity.class)));
        updateStaticStatus();
        listenActiveRoom();
    }

    private void createRoom() {
        if (!ensureBattleAvailable()) return;
        int difficulty = PreferenceUtil.getDifficulty(this);
        battleRepository.createRoom(difficulty, roomCallback(R.string.room_created));
    }

    private void joinRoom() {
        if (!ensureBattleAvailable()) return;
        String roomId = binding.etRoomCode.getText() == null
                ? ""
                : binding.etRoomCode.getText().toString().trim();
        if (roomId.isEmpty()) {
            Toast.makeText(this, R.string.room_code_required, Toast.LENGTH_SHORT).show();
            return;
        }
        battleRepository.joinRoom(roomId, roomCallback(R.string.room_joined));
    }

    private void startBattleAndOpenGame() {
        if (!battleRepository.hasActiveBattle()) {
            Toast.makeText(this, R.string.battle_create_or_join_first, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ensureBattleAvailable()) return;
        if (BattleRepository.STATUS_WAITING.equals(currentRoomStatus)) {
            Toast.makeText(this, R.string.battle_waiting_for_opponent_start, Toast.LENGTH_SHORT).show();
            return;
        }
        battleRepository.startBattle(new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String roomId) {
                openGame();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(BattleActivity.this,
                        getString(R.string.battle_error, error.getMessage()),
                        Toast.LENGTH_LONG).show();
                updateStaticStatus();
            }
        });
    }

    private void leaveRoom() {
        battleRepository.leaveBattle(new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                binding.etRoomCode.setText("");
                Toast.makeText(BattleActivity.this, R.string.room_left, Toast.LENGTH_SHORT).show();
                listenActiveRoom();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(BattleActivity.this,
                        getString(R.string.battle_error, error.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
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
                updateStaticStatus();
            }
        });
        updateStaticStatus();
    }

    private void updateRoomStatus(DocumentSnapshot snapshot) {
        String roomId = snapshot.getId();
        String status = snapshot.getString("status");
        currentRoomStatus = status;
        String p1Name = playerName(snapshot.getString("player1Name"));
        String p2Name = playerName(snapshot.getString("player2Name"));
        int p1Score = getInt(snapshot, "player1Score");
        int p2Score = getInt(snapshot, "player2Score");
        int p1Remaining = getInt(snapshot, "player1RemainingPairs");
        int p2Remaining = getInt(snapshot, "player2RemainingPairs");
        String detail = getString(
                R.string.battle_status_detail,
                roomId,
                battleStatusLabel(status),
                p1Name,
                p1Score,
                p1Remaining,
                playerStatusLabel(snapshot.getString("player1Status")),
                p2Name,
                p2Score,
                p2Remaining,
                playerStatusLabel(snapshot.getString("player2Status")));

        String winnerUid = snapshot.getString("winnerUid");
        if (winnerUid != null && !winnerUid.trim().isEmpty()) {
            detail += "\n" + getString(R.string.battle_winner,
                    winnerName(snapshot, winnerUid));
        }
        binding.tvBattleStatus.setText(detail);

        boolean hasActiveBattle = battleRepository.hasActiveBattle();
        boolean roomClosed = BattleRepository.STATUS_FINISHED.equals(status)
                || BattleRepository.STATUS_ABANDONED.equals(status);
        binding.btnCreateRoom.setEnabled(!hasActiveBattle);
        binding.btnJoinRoom.setEnabled(!hasActiveBattle);
        binding.etRoomCode.setEnabled(!hasActiveBattle);
        binding.btnStartBattle.setEnabled(hasActiveBattle && !roomClosed);
        binding.btnLeaveRoom.setEnabled(hasActiveBattle);
        binding.btnBattleLogin.setVisibility(battleRepository.isLoggedIn()
                ? android.view.View.GONE
                : android.view.View.VISIBLE);
        binding.btnStartBattle.setText(BattleRepository.STATUS_PLAYING.equals(status)
                ? R.string.continue_battle
                : R.string.start_battle);
    }

    private void updateStaticStatus() {
        boolean hasActiveBattle = battleRepository.hasActiveBattle();
        currentRoomStatus = null;
        if (hasActiveBattle) {
            binding.tvBattleStatus.setText(getString(
                    R.string.battle_active_room,
                    battleRepository.getActiveRoomId()));
        } else if (battleRepository.canUseOnlineBattle()) {
            binding.tvBattleStatus.setText(R.string.battle_ready);
        } else {
            binding.tvBattleStatus.setText(battleUnavailableMessage());
        }
        binding.btnCreateRoom.setEnabled(!hasActiveBattle);
        binding.btnJoinRoom.setEnabled(!hasActiveBattle);
        binding.etRoomCode.setEnabled(!hasActiveBattle);
        binding.btnStartBattle.setEnabled(hasActiveBattle);
        binding.btnLeaveRoom.setEnabled(hasActiveBattle);
        binding.btnBattleLogin.setVisibility(battleRepository.isLoggedIn()
                ? android.view.View.GONE
                : android.view.View.VISIBLE);
    }

    private boolean ensureBattleAvailable() {
        if (battleRepository.canUseOnlineBattle()) {
            return true;
        }
        int message = battleUnavailableMessage();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (!battleRepository.isLoggedIn()) {
            binding.btnBattleLogin.requestFocus();
        }
        return false;
    }

    private int battleUnavailableMessage() {
        if (!battleRepository.isFirebaseAvailable()) {
            return R.string.battle_requires_firebase;
        }
        if (!battleRepository.isLoggedIn()) {
            return R.string.battle_requires_login;
        }
        if (!battleRepository.isNetworkAvailable()) {
            return R.string.battle_requires_network;
        }
        return R.string.battle_requires_login;
    }

    private void openGame() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_GAME, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String battleStatusLabel(String status) {
        if (BattleRepository.STATUS_WAITING.equals(status)) return getString(R.string.battle_status_waiting);
        if (BattleRepository.STATUS_READY.equals(status)) return getString(R.string.battle_status_ready);
        if (BattleRepository.STATUS_PLAYING.equals(status)) return getString(R.string.battle_status_playing);
        if (BattleRepository.STATUS_FINISHED.equals(status)) return getString(R.string.battle_status_finished);
        if (BattleRepository.STATUS_ABANDONED.equals(status)) return getString(R.string.battle_status_abandoned);
        return getString(R.string.unknown);
    }

    private String playerStatusLabel(String status) {
        if (BattleRepository.PLAYER_WAITING.equals(status)) return getString(R.string.player_status_waiting);
        if (BattleRepository.PLAYER_PLAYING.equals(status)) return getString(R.string.player_status_playing);
        if (BattleRepository.PLAYER_FINISHED.equals(status)) return getString(R.string.player_status_finished);
        if (BattleRepository.PLAYER_TIME_UP.equals(status)) return getString(R.string.player_status_time_up);
        if (BattleRepository.PLAYER_FAILED.equals(status)) return getString(R.string.player_status_failed);
        if (BattleRepository.PLAYER_LEFT.equals(status)) return getString(R.string.player_status_left);
        return getString(R.string.player_status_empty);
    }

    private String playerName(String name) {
        return name == null || name.trim().isEmpty()
                ? getString(R.string.battle_waiting_player)
                : name;
    }

    private String winnerName(DocumentSnapshot snapshot, String winnerUid) {
        if (BattleRepository.WINNER_DRAW.equals(winnerUid)) {
            return getString(R.string.battle_draw);
        }
        if (winnerUid.equals(snapshot.getString("player1Uid"))) {
            return playerName(snapshot.getString("player1Name"));
        }
        if (winnerUid.equals(snapshot.getString("player2Uid"))) {
            return playerName(snapshot.getString("player2Name"));
        }
        return getString(R.string.unknown);
    }

    private int getInt(DocumentSnapshot snapshot, String key) {
        Long value = snapshot.getLong(key);
        return value == null ? 0 : value.intValue();
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
