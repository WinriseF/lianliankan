package com.example.lianliankan.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.lianliankan.util.GameEngine;
import com.example.lianliankan.util.NetworkUtil;
import com.google.firebase.firestore.DocumentReference;
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

    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_READY = "ready";
    public static final String STATUS_PLAYING = "playing";
    public static final String STATUS_FINISHED = "finished";
    public static final String STATUS_ABANDONED = "abandoned";

    public static final String PLAYER_EMPTY = "empty";
    public static final String PLAYER_WAITING = "waiting";
    public static final String PLAYER_PLAYING = "playing";
    public static final String PLAYER_FINISHED = "finished";
    public static final String PLAYER_TIME_UP = "time_up";
    public static final String PLAYER_FAILED = "failed";
    public static final String PLAYER_LEFT = "left";

    public static final String WINNER_DRAW = "draw";

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

    public boolean isFirebaseAvailable() {
        return firestore != null;
    }

    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    public boolean isNetworkAvailable() {
        return NetworkUtil.isNetworkAvailable(appContext);
    }

    public String getCurrentUid() {
        return authRepository.getCurrentUid();
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
        long now = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("seed", seed);
        data.put("difficulty", difficulty);
        data.put("status", STATUS_WAITING);
        data.put("player1Uid", authRepository.getCurrentUid());
        data.put("player1Name", authRepository.getDisplayName());
        data.put("player1Score", 0);
        data.put("player1RemainingPairs", GameEngine.PAIRS_COUNT);
        data.put("player1TimeUsed", 0);
        data.put("player1Status", PLAYER_WAITING);
        data.put("player1UpdatedAt", now);
        data.put("player2Uid", "");
        data.put("player2Name", "");
        data.put("player2Score", 0);
        data.put("player2RemainingPairs", GameEngine.PAIRS_COUNT);
        data.put("player2TimeUsed", 0);
        data.put("player2Status", PLAYER_EMPTY);
        data.put("player2UpdatedAt", 0);
        data.put("winnerUid", "");
        data.put("createdAt", now);
        data.put("startedAt", 0);
        data.put("finishedAt", 0);

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
        if (safeRoomId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Room code is required"));
            return;
        }
        DocumentReference roomRef = firestore.collection("battles").document(safeRoomId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(roomRef);
                    if (!snapshot.exists()) {
                        throw new IllegalArgumentException("Room not found");
                    }
                    String currentUid = authRepository.getCurrentUid();
                    String player1Uid = snapshot.getString("player1Uid");
                    String player2Uid = snapshot.getString("player2Uid");
                    long seed = getLong(snapshot, "seed", 0L);
                    int difficulty = getInt(snapshot, "difficulty", 0);

                    if (currentUid.equals(player1Uid) || currentUid.equals(player2Uid)) {
                        return new RoomInfo(safeRoomId, seed, difficulty);
                    }
                    if (hasText(player2Uid)) {
                        throw new IllegalStateException("Room is full");
                    }
                    String status = snapshot.getString("status");
                    if (STATUS_FINISHED.equals(status) || STATUS_ABANDONED.equals(status)) {
                        throw new IllegalStateException("Room is closed");
                    }

                    long now = System.currentTimeMillis();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("player2Uid", currentUid);
                    updates.put("player2Name", authRepository.getDisplayName());
                    updates.put("player2Score", 0);
                    updates.put("player2RemainingPairs", GameEngine.PAIRS_COUNT);
                    updates.put("player2TimeUsed", 0);
                    updates.put("player2Status", PLAYER_WAITING);
                    updates.put("player2UpdatedAt", now);
                    updates.put("status", STATUS_READY);
                    transaction.update(roomRef, updates);
                    return new RoomInfo(safeRoomId, seed, difficulty);
                })
                .addOnSuccessListener(info -> {
                    rememberRoom(info.roomId, info.seed, info.difficulty);
                    callback.onSuccess(info.roomId);
                })
                .addOnFailureListener(callback::onError);
    }

    public void startBattle(RepositoryCallback<String> callback) {
        String roomId = getActiveRoomId();
        if (roomId == null) {
            callback.onError(new IllegalStateException("No active room"));
            return;
        }
        if (!canUseOnlineBattle()) {
            callback.onError(new IllegalStateException("Login and network are required"));
            return;
        }
        DocumentReference roomRef = firestore.collection("battles").document(roomId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(roomRef);
                    if (!snapshot.exists()) {
                        throw new IllegalArgumentException("Room not found");
                    }
                    String slot = getCurrentSlot(snapshot);
                    if (slot == null) {
                        throw new IllegalStateException("You are not in this room");
                    }
                    String otherUid = "player1".equals(slot)
                            ? snapshot.getString("player2Uid")
                            : snapshot.getString("player1Uid");
                    if (!hasText(otherUid)) {
                        throw new IllegalStateException("Waiting for opponent");
                    }
                    String status = snapshot.getString("status");
                    if (STATUS_FINISHED.equals(status) || STATUS_ABANDONED.equals(status)) {
                        throw new IllegalStateException("Room is closed");
                    }

                    long now = System.currentTimeMillis();
                    long startAt = now + 3000L;
                    Map<String, Object> updates = new HashMap<>();
                    updates.put(slot + "Status", PLAYER_PLAYING);
                    updates.put(slot + "UpdatedAt", now);
                    if (!STATUS_PLAYING.equals(status)) {
                        updates.put("status", STATUS_PLAYING);
                    }
                    if (getLong(snapshot, "startedAt", 0L) == 0L) {
                        updates.put("startedAt", startAt);
                    }
                    transaction.update(roomRef, updates);
                    return roomId;
                })
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration listenActiveBattle(BattleListener listener) {
        String roomId = getActiveRoomId();
        if (roomId == null || firestore == null) return null;
        return firestore.collection("battles").document(roomId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                    } else if (snapshot != null && snapshot.exists()) {
                        listener.onBattleChanged(snapshot);
                    }
                });
    }

    public void publishProgress(int score, int remainingPairs, int timeUsed, String status) {
        String roomId = getActiveRoomId();
        if (roomId == null || !canUseOnlineBattle()) return;
        firestore.collection("battles").document(roomId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;
                    String roomStatus = snapshot.getString("status");
                    if (STATUS_FINISHED.equals(roomStatus) || STATUS_ABANDONED.equals(roomStatus)) {
                        return;
                    }
                    String slot = getCurrentSlot(snapshot);
                    if (slot == null) return;
                    Map<String, Object> data = new HashMap<>();
                    data.put(slot + "Score", score);
                    data.put(slot + "RemainingPairs", remainingPairs);
                    data.put(slot + "TimeUsed", Math.max(0, timeUsed));
                    data.put(slot + "UpdatedAt", System.currentTimeMillis());
                    data.put(slot + "Status",
                            status == null || status.trim().isEmpty()
                                    ? PLAYER_PLAYING
                                    : status);
                    firestore.collection("battles").document(roomId).update(data);
                });
    }

    public void finishBattle(int score, int remainingPairs, int timeUsed, String result) {
        String roomId = getActiveRoomId();
        if (roomId == null || !canUseOnlineBattle()) return;
        DocumentReference roomRef = firestore.collection("battles").document(roomId);
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(roomRef);
            if (!snapshot.exists()) return null;
            String slot = getCurrentSlot(snapshot);
            if (slot == null) return null;

            String roomStatus = snapshot.getString("status");
            if (STATUS_FINISHED.equals(roomStatus) || STATUS_ABANDONED.equals(roomStatus)) {
                return null;
            }

            String opponentSlot = "player1".equals(slot) ? "player2" : "player1";
            String opponentUid = snapshot.getString(opponentSlot + "Uid");
            String opponentStatus = snapshot.getString(opponentSlot + "Status");
            long now = System.currentTimeMillis();
            Map<String, Object> updates = new HashMap<>();
            updates.put(slot + "Score", score);
            updates.put(slot + "RemainingPairs", remainingPairs);
            updates.put(slot + "TimeUsed", Math.max(0, timeUsed));
            updates.put(slot + "Status", normalizeTerminalStatus(result, remainingPairs));
            updates.put(slot + "UpdatedAt", now);

            if ("win".equals(result) || remainingPairs <= 0) {
                updates.put("status", STATUS_FINISHED);
                updates.put("winnerUid", authRepository.getCurrentUid());
                updates.put("finishedAt", now);
            } else if (isTerminalPlayerStatus(opponentStatus)) {
                int opponentScore = getInt(snapshot, opponentSlot + "Score", 0);
                int opponentRemaining = getInt(snapshot, opponentSlot + "RemainingPairs",
                        GameEngine.PAIRS_COUNT);
                int opponentTimeUsed = getInt(snapshot, opponentSlot + "TimeUsed", 0);
                updates.put("status", STATUS_FINISHED);
                updates.put("winnerUid", resolveWinner(
                        authRepository.getCurrentUid(),
                        score,
                        remainingPairs,
                        timeUsed,
                        opponentUid,
                        opponentScore,
                        opponentRemaining,
                        opponentTimeUsed));
                updates.put("finishedAt", now);
            }

            transaction.update(roomRef, updates);
            return null;
        });
    }

    public void leaveBattle() {
        leaveBattle(null);
    }

    public void leaveBattle(RepositoryCallback<Void> callback) {
        String roomId = getActiveRoomId();
        if (roomId == null) {
            clearActiveBattle();
            notifyLeaveSuccess(callback);
            return;
        }
        if (!canUseOnlineBattle()) {
            clearActiveBattle();
            notifyLeaveSuccess(callback);
            return;
        }
        DocumentReference roomRef = firestore.collection("battles").document(roomId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(roomRef);
                    if (!snapshot.exists()) return null;
                    String slot = getCurrentSlot(snapshot);
                    if (slot == null) return null;

                    String opponentSlot = "player1".equals(slot) ? "player2" : "player1";
                    String opponentUid = snapshot.getString(opponentSlot + "Uid");
                    String opponentStatus = snapshot.getString(opponentSlot + "Status");
                    long now = System.currentTimeMillis();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put(slot + "Status", PLAYER_LEFT);
                    updates.put(slot + "UpdatedAt", now);

                    if (hasText(opponentUid) && !PLAYER_LEFT.equals(opponentStatus)) {
                        updates.put("status", STATUS_FINISHED);
                        updates.put("winnerUid", opponentUid);
                    } else {
                        updates.put("status", STATUS_ABANDONED);
                        updates.put("winnerUid", "");
                    }
                    updates.put("finishedAt", now);
                    transaction.update(roomRef, updates);
                    return null;
                })
                .addOnSuccessListener(ignored -> {
                    clearActiveBattle();
                    notifyLeaveSuccess(callback);
                })
                .addOnFailureListener(error -> {
                    if (callback != null) callback.onError(error);
                });
    }

    private void rememberRoom(String roomId, long seed, int difficulty) {
        prefs.edit()
                .putString(KEY_ROOM, roomId)
                .putLong(KEY_SEED, seed)
                .putInt(KEY_DIFFICULTY, difficulty)
                .apply();
    }

    private String getCurrentSlot(DocumentSnapshot snapshot) {
        String currentUid = authRepository.getCurrentUid();
        if (currentUid.equals(snapshot.getString("player1Uid"))) {
            return "player1";
        }
        if (currentUid.equals(snapshot.getString("player2Uid"))) {
            return "player2";
        }
        return null;
    }

    private String normalizeTerminalStatus(String result, int remainingPairs) {
        if ("win".equals(result) || remainingPairs <= 0) {
            return PLAYER_FINISHED;
        }
        if ("time_up".equals(result)) {
            return PLAYER_TIME_UP;
        }
        if (PLAYER_LEFT.equals(result)) {
            return PLAYER_LEFT;
        }
        return PLAYER_FAILED;
    }

    private boolean isTerminalPlayerStatus(String status) {
        return PLAYER_FINISHED.equals(status)
                || PLAYER_TIME_UP.equals(status)
                || PLAYER_FAILED.equals(status)
                || PLAYER_LEFT.equals(status);
    }

    private String resolveWinner(String currentUid, int currentScore, int currentRemaining,
                                 int currentTimeUsed, String opponentUid, int opponentScore,
                                 int opponentRemaining, int opponentTimeUsed) {
        if (currentRemaining <= 0 && opponentRemaining > 0) return currentUid;
        if (opponentRemaining <= 0 && currentRemaining > 0) return opponentUid;
        if (currentScore > opponentScore) return currentUid;
        if (opponentScore > currentScore) return opponentUid;
        if (currentTimeUsed > 0 && opponentTimeUsed > 0) {
            if (currentTimeUsed < opponentTimeUsed) return currentUid;
            if (opponentTimeUsed < currentTimeUsed) return opponentUid;
        }
        return WINNER_DRAW;
    }

    private int getInt(DocumentSnapshot snapshot, String key, int fallback) {
        Long value = snapshot.getLong(key);
        return value == null ? fallback : value.intValue();
    }

    private long getLong(DocumentSnapshot snapshot, String key, long fallback) {
        Long value = snapshot.getLong(key);
        return value == null ? fallback : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void notifyLeaveSuccess(RepositoryCallback<Void> callback) {
        if (callback != null) callback.onSuccess(null);
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

    private static class RoomInfo {
        final String roomId;
        final long seed;
        final int difficulty;

        RoomInfo(String roomId, long seed, int difficulty) {
            this.roomId = roomId;
            this.seed = seed;
            this.difficulty = difficulty;
        }
    }
}
