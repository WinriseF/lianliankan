package com.example.lianliankan.repository;

import android.content.Context;

import com.example.lianliankan.dao.RankDatabaseHelper;
import com.example.lianliankan.model.SavedGameState;
import com.example.lianliankan.util.NetworkUtil;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class GameStateRepository {

    private final Context appContext;
    private final RankDatabaseHelper dbHelper;
    private final AuthRepository authRepository;
    private final FirebaseFirestore firestore;

    public GameStateRepository(Context context) {
        appContext = context.getApplicationContext();
        dbHelper = new RankDatabaseHelper(appContext);
        authRepository = new AuthRepository(appContext);
        firestore = FirebaseAvailability.firestore(appContext);
    }

    public String currentUid() {
        return authRepository.getCurrentUid();
    }

    public void save(SavedGameState state) {
        AppExecutors.io().execute(() -> {
            try {
                dbHelper.saveGameState(state);
                syncCloudIfPossible(state);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void load(RepositoryCallback<SavedGameState> callback) {
        AppExecutors.io().execute(() -> {
            try {
                SavedGameState state = dbHelper.loadGameState();
                if (state != null || firestore == null
                        || !authRepository.isLoggedIn()
                        || !NetworkUtil.isNetworkAvailable(appContext)) {
                    AppExecutors.main(() -> callback.onSuccess(state));
                    return;
                }
                loadCloudState(callback);
            } catch (SecurityException e) {
                AppExecutors.main(() -> callback.onError(e));
            } catch (Exception e) {
                AppExecutors.main(() -> callback.onError(e));
            }
        });
    }

    private void loadCloudState(RepositoryCallback<SavedGameState> callback) {
        firestore.collection("saves")
                .document(authRepository.getCurrentUid())
                .collection("current")
                .document("default")
                .get()
                .addOnSuccessListener(snapshot -> {
                    try {
                        String payload = snapshot.getString("payload");
                        SavedGameState state = payload == null ? null : SavedGameState.fromJson(payload);
                        if (state == null) {
                            AppExecutors.main(() -> callback.onSuccess(null));
                            return;
                        }
                        AppExecutors.io().execute(() -> {
                            try {
                                dbHelper.saveGameState(state);
                                AppExecutors.main(() -> callback.onSuccess(state));
                            } catch (Exception e) {
                                AppExecutors.main(() -> callback.onError(e));
                            }
                        });
                    } catch (Exception e) {
                        AppExecutors.main(() -> callback.onError(e));
                    }
                })
                .addOnFailureListener(e -> AppExecutors.main(() -> callback.onError(e)));
    }

    public void clear() {
        AppExecutors.io().execute(() -> {
            dbHelper.clearGameState();
            if (firestore != null && authRepository.isLoggedIn() && NetworkUtil.isNetworkAvailable(appContext)) {
                firestore.collection("saves")
                        .document(authRepository.getCurrentUid())
                        .collection("current")
                        .document("default")
                        .delete();
            }
        });
    }

    public void syncCloudIfPossible(SavedGameState state) throws JSONException {
        if (firestore == null || !authRepository.isLoggedIn() || !NetworkUtil.isNetworkAvailable(appContext)) {
            dbHelper.enqueueSync("game_save", state.toJson());
            return;
        }
        firestore.collection("saves")
                .document(authRepository.getCurrentUid())
                .collection("current")
                .document("default")
                .set(toCloudPayload(state))
                .addOnSuccessListener(ignored ->
                        AppExecutors.io().execute(() -> dbHelper.clearSyncQueue("game_save")))
                .addOnFailureListener(e -> {
                    try {
                        dbHelper.enqueueSync("game_save", state.toJson());
                    } catch (JSONException ignored) {
                    }
                });
    }

    private Map<String, Object> toCloudPayload(SavedGameState state) throws JSONException {
        Map<String, Object> data = new HashMap<>(state.toCloudMap());
        data.put("payload", state.toJson());
        return data;
    }
}
