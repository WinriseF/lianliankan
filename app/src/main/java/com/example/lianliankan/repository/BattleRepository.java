package com.example.lianliankan.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.lianliankan.util.NetworkUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class BattleRepository {

    public interface BattleListener {
        void onBattleChanged(DocumentSnapshot snapshot);

        void onError(Exception error);
    }

    private static final String PREFS = "battle_state";
    private static final String KEY_ROOM = "room_id";
    private static final String KEY_SEED = "seed";
    private static final String KEY_DIFFICULTY = "difficulty";

    private final Context appContext;
    private final SharedPreferences prefs;
    private final FirebaseFirestore firestore;
    private final AuthRepository authRepository;

    public BattleRepository(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        firestore = FirebaseAvailability.firestore(appContext);
        authRepository = new AuthRepository(appContext);
    }

    public boolean canUseOnlineBattle() {
        return firestore != null
                && authRepository.isLoggedIn()
                && NetworkUtil.isNetworkAvailable(appContext);
    }

    public String getActiveRoomId() {
        return prefs.getString(KEY_ROOM, null);
    }

    public long getActiveSeed() {
        return prefs.getLong(KEY_SEED, 0L);
    }

    public int getActiveDifficulty(int fallback) {
        return prefs.getInt(KEY_DIFFICULTY, fallback);
    }

    public boolean hasActiveBattle() {
        return getActiveRoomId() != null;
    }

    public void clearActiveBattle() {
        prefs.edit().clear().apply();
    }

    public void createRoom(int difficulty, RepositoryCallback<String> callback) {
        if (!canUseOnlineBattle()) {
            callback.onError(new IllegalStateException("Login and network are required"));
            return;
        }
        String roomId = createRoomCode();
        long seed = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("seed", seed);
        data.put("difficulty", difficulty);
        data.put("status", "waiting");
        data.put("player1Uid", authRepository.getCurrentUid());
        data.put("player1Name", authRepository.getDisplayName());
        data.put("player1Score", 0);
        data.put("player1RemainingPairs", 36);
        data.put("createdAt", System.currentTimeMillis());

        firestore.collection("battles").document(roomId)
                .set(data)
                .addOnSuccessListener(ignored -> {
                    rememberRoom(roomId, seed, difficulty);
                    callback.onSuccess(roomId);
                })
                .addOnFailureListener(callback::onError);
    }

    public void joinRoom(String roomId, RepositoryCallback<String> callback) {
        if (!canUseOnlineBattle()) {
            callback.onError(new IllegalStateException("Login and network are required"));
            return;
        }
        String safeRoomId = roomId == null ? "" : roomId.trim().toUpperCase(Locale.US);
        firestore.collection("battles").document(safeRoomId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onError(new IllegalArgumentException("Room not found"));
                        return;
                    }
                    long seed = snapshot.getLong("seed") == null ? 0L : snapshot.getLong("seed");
                    int difficulty = snapshot.getLong("difficulty") == null
                            ? 0
                            : snapshot.getLong("difficulty").intValue();
                    Map<String, Object> data = new HashMap<>();
                    data.put("player2Uid", authRepository.getCurrentUid());
                    data.put("player2Name", authRepository.getDisplayName());
                    data.put("player2Score", 0);
                    data.put("player2RemainingPairs", 36);
                    data.put("status", "playing");
                    firestore.collection("battles").document(safeRoomId)
                            .update(data)
                            .addOnSuccessListener(ignored -> {
                                rememberRoom(safeRoomId, seed, difficulty);
                                callback.onSuccess(safeRoomId);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration listenActiveBattle(BattleListener listener) {
        String roomId = getActiveRoomId();
        if (roomId == null || firestore == null) return null;
        return firestore.collection("battles").document(roomId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                    } else if (snapshot != null) {
                        listener.onBattleChanged(snapshot);
                    }
                });
    }

    public void publishProgress(int score, int remainingPairs, String status) {
        String roomId = getActiveRoomId();
        if (roomId == null || !canUseOnlineBattle()) return;
        firestore.collection("battles").document(roomId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String uid = authRepository.getCurrentUid();
                    String slot = uid.equals(snapshot.getString("player1Uid")) ? "player1" : "player2";
                    Map<String, Object> data = new HashMap<>();
                    data.put(slot + "Score", score);
                    data.put(slot + "RemainingPairs", remainingPairs);
                    data.put(slot + "UpdatedAt", System.currentTimeMillis());
                    if (status != null) {
                        data.put(slot + "Status", status);
                        if ("finished".equals(status)) {
                            data.put("status", "finished");
                            data.put("winnerUid", uid);
                            data.put("finishedAt", System.currentTimeMillis());
                        }
                    }
                    firestore.collection("battles").document(roomId).update(data);
                });
    }

    private void rememberRoom(String roomId, long seed, int difficulty) {
        prefs.edit()
                .putString(KEY_ROOM, roomId)
                .putLong(KEY_SEED, seed)
                .putInt(KEY_DIFFICULTY, difficulty)
                .apply();
    }

    private String createRoomCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
