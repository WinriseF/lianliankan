package com.example.lianliankan.repository;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.lianliankan.model.RankRecord;
import com.example.lianliankan.dao.RankDatabaseHelper;
import com.example.lianliankan.provider.RankContract;
import com.example.lianliankan.provider.RankContract.RankEntry;
import com.example.lianliankan.util.NetworkUtil;
import com.example.lianliankan.util.PreferenceUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RankingRepository {

    public interface RankingListener {
        void onRankingsChanged(List<RankRecord> records, boolean fromCloud);

        void onError(Exception error);
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;
    private final AuthRepository authRepository;
    private final RankDatabaseHelper dbHelper;

    public RankingRepository(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseAvailability.firestore(appContext);
        authRepository = new AuthRepository(appContext);
        dbHelper = new RankDatabaseHelper(appContext);
    }

    public void submitResult(String result, int score, int timeUsed, int difficulty,
                             RepositoryCallback<Void> callback) {
        AppExecutors.io().execute(() ->
                submitResultInternal(result, score, timeUsed, difficulty, callback));
    }

    private void submitResultInternal(String result, int score, int timeUsed, int difficulty,
                                      RepositoryCallback<Void> callback) {
        String uid = authRepository.getCurrentUid();
        String playerName = authRepository.getDisplayName();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        boolean canSync = firestore != null
                && authRepository.isLoggedIn()
                && NetworkUtil.isNetworkAvailable(appContext);
        if (canSync) {
            flushPendingRankings();
        }

        ContentValues values = new ContentValues();
        values.put(RankEntry.COLUMN_UID, uid);
        values.put(RankEntry.COLUMN_PLAYER_NAME, playerName);
        values.put(RankEntry.COLUMN_SCORE, score);
        values.put(RankEntry.COLUMN_TIME_USED, timeUsed);
        values.put(RankEntry.COLUMN_DIFFICULTY, difficulty);
        values.put(RankEntry.COLUMN_TIMESTAMP, timestamp);
        values.put(RankEntry.COLUMN_SYNCED, canSync && "win".equals(result) ? 1 : 0);
        appContext.getContentResolver().insert(RankContract.RankEntry.CONTENT_URI, values);

        if (!"win".equals(result)) {
            notifySuccess(callback);
            return;
        }

        if (!canSync) {
            enqueuePendingRank(uid, playerName, score, timeUsed, difficulty, timestamp);
            notifySuccess(callback);
            return;
        }

        RankRecord record = new RankRecord(null, uid, playerName, score,
                timeUsed, difficulty, System.currentTimeMillis(), true);
        firestore.collection("rankings")
                .add(record.toCloudMap())
                .addOnSuccessListener(doc -> {
                    notifySuccess(callback);
                })
                .addOnFailureListener(e -> {
                    enqueuePendingRank(uid, playerName, score, timeUsed, difficulty, timestamp);
                    notifyError(callback, e);
                });
    }

    public ListenerRegistration listenTopRankings(int difficulty, RankingListener listener) {
        if (firestore == null || !authRepository.isLoggedIn() || !NetworkUtil.isNetworkAvailable(appContext)) {
            loadLocalRankings(difficulty, listener);
            return null;
        }
        flushPendingRankings();
        Query query = firestore.collection("rankings")
                .whereEqualTo("difficulty", difficulty)
                .limit(50);
        return query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                loadLocalRankings(difficulty, listener);
                return;
            }
            List<RankRecord> records = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    RankRecord record = doc.toObject(RankRecord.class);
                    if (record != null) {
                        record.id = doc.getId();
                        record.synced = true;
                        records.add(record);
                    }
                }
            }
            sortRankings(records);
            listener.onRankingsChanged(records, true);
        });
    }

    private void loadLocalRankings(int difficulty, RankingListener listener) {
        AppExecutors.io().execute(() -> {
            List<RankRecord> records = new ArrayList<>();
            ContentResolver resolver = appContext.getContentResolver();
            Cursor cursor = resolver.query(
                    RankContract.RankEntry.CONTENT_URI,
                    null,
                    RankEntry.COLUMN_DIFFICULTY + "=?",
                    new String[]{String.valueOf(difficulty)},
                    RankEntry.COLUMN_SCORE + " DESC, " +
                            RankEntry.COLUMN_TIME_USED + " ASC, " +
                            RankEntry.COLUMN_TIMESTAMP + " DESC");
            try {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        RankRecord record = new RankRecord();
                        record.id = String.valueOf(getLong(cursor, RankEntry._ID));
                        record.uid = getString(cursor, RankEntry.COLUMN_UID);
                        record.playerName = getString(cursor, RankEntry.COLUMN_PLAYER_NAME);
                        record.score = getInt(cursor, RankEntry.COLUMN_SCORE);
                        record.timeUsed = getInt(cursor, RankEntry.COLUMN_TIME_USED);
                        record.difficulty = getInt(cursor, RankEntry.COLUMN_DIFFICULTY);
                        record.synced = getInt(cursor, RankEntry.COLUMN_SYNCED) == 1;
                        records.add(record);
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
            sortRankings(records);
            AppExecutors.main(() -> listener.onRankingsChanged(records, false));
        });
    }

    private void sortRankings(List<RankRecord> records) {
        Collections.sort(records, (a, b) -> {
            int scoreCompare = Integer.compare(b.score, a.score);
            if (scoreCompare != 0) return scoreCompare;
            int timeCompare = Integer.compare(a.timeUsed, b.timeUsed);
            if (timeCompare != 0) return timeCompare;
            return Long.compare(b.createdAt, a.createdAt);
        });
        if (records.size() > 50) {
            records.subList(50, records.size()).clear();
        }
    }

    public void clearLocalRankings(RepositoryCallback<Void> callback) {
        AppExecutors.io().execute(() -> {
            try {
                appContext.getContentResolver().delete(RankContract.RankEntry.CONTENT_URI, null, null);
                AppExecutors.main(() -> callback.onSuccess(null));
            } catch (Exception e) {
                AppExecutors.main(() -> callback.onError(e));
            }
        });
    }

    private void enqueuePendingRank(String uid, String playerName, int score,
                                    int timeUsed, int difficulty, String timestamp) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("uid", uid);
            payload.put("playerName", playerName);
            payload.put("score", score);
            payload.put("timeUsed", timeUsed);
            payload.put("difficulty", difficulty);
            payload.put("timestamp", timestamp);
            dbHelper.enqueueSync("ranking", payload.toString());
        } catch (Exception ignored) {
        }
    }

    private void flushPendingRankings() {
        if (firestore == null || !authRepository.isLoggedIn()
                || !NetworkUtil.isNetworkAvailable(appContext)) {
            return;
        }
        AppExecutors.io().execute(() -> {
            Cursor cursor = dbHelper.querySyncQueue("ranking");
            try {
                while (cursor != null && cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(RankDatabaseHelper.COLUMN_QUEUE_ID));
                    String payload = cursor.getString(cursor.getColumnIndexOrThrow(RankDatabaseHelper.COLUMN_QUEUE_PAYLOAD));
                    JSONObject json = new JSONObject(payload);
                    RankRecord record = new RankRecord(
                            null,
                            json.optString("uid", authRepository.getCurrentUid()),
                            json.optString("playerName", PreferenceUtil.getPlayerName(appContext)),
                            json.optInt("score", 0),
                            json.optInt("timeUsed", 0),
                            json.optInt("difficulty", 0),
                            System.currentTimeMillis(),
                            true);
                    firestore.collection("rankings")
                            .add(record.toCloudMap())
                            .addOnSuccessListener(doc -> dbHelper.deleteSyncItem(id));
                }
            } catch (Exception ignored) {
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    private int getInt(Cursor cursor, String key) {
        int index = cursor.getColumnIndex(key);
        return index >= 0 ? cursor.getInt(index) : 0;
    }

    private long getLong(Cursor cursor, String key) {
        int index = cursor.getColumnIndex(key);
        return index >= 0 ? cursor.getLong(index) : 0;
    }

    private String getString(Cursor cursor, String key) {
        int index = cursor.getColumnIndex(key);
        return index >= 0 ? cursor.getString(index) : "";
    }

    private void notifySuccess(RepositoryCallback<Void> callback) {
        if (callback != null) {
            AppExecutors.main(() -> callback.onSuccess(null));
        }
    }

    private void notifyError(RepositoryCallback<Void> callback, Exception error) {
        if (callback != null) {
            AppExecutors.main(() -> callback.onError(error));
        }
    }
}
