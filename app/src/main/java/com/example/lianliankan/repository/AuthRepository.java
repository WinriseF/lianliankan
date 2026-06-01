package com.example.lianliankan.repository;

import android.content.Context;

import com.example.lianliankan.util.PreferenceUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {

    private final Context appContext;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public AuthRepository(Context context) {
        appContext = context.getApplicationContext();
        auth = FirebaseAvailability.auth(appContext);
        firestore = FirebaseAvailability.firestore(appContext);
    }

    public FirebaseUser getCurrentUser() {
        return auth == null ? null : auth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public String getCurrentUid() {
        FirebaseUser user = getCurrentUser();
        return user == null ? "guest" : user.getUid();
    }

    public String getDisplayName() {
        FirebaseUser user = getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName();
        }
        return PreferenceUtil.getPlayerName(appContext);
    }

    public void login(String email, String password, RepositoryCallback<FirebaseUser> callback) {
        if (auth == null) {
            callback.onError(new IllegalStateException("Firebase is not configured"));
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        PreferenceUtil.savePlayerName(appContext, getDisplayName());
                    }
                    callback.onSuccess(user);
                })
                .addOnFailureListener(callback::onError);
    }

    public void register(String email, String password, String nickname,
                         RepositoryCallback<FirebaseUser> callback) {
        if (auth == null || firestore == null) {
            callback.onError(new IllegalStateException("Firebase is not configured"));
            return;
        }
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError(new IllegalStateException("Firebase user is empty"));
                        return;
                    }
                    String safeName = nickname == null || nickname.trim().isEmpty()
                            ? email
                            : nickname.trim();
                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(safeName)
                            .build();
                    user.updateProfile(profile)
                            .addOnSuccessListener(ignored -> {
                                PreferenceUtil.savePlayerName(appContext, safeName);
                                createUserDocument(user.getUid(), email, safeName, callback, user);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void logout() {
        if (auth != null) {
            auth.signOut();
        }
    }

    private void createUserDocument(String uid, String email, String nickname,
                                    RepositoryCallback<FirebaseUser> callback,
                                    FirebaseUser user) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("email", email);
        data.put("nickname", nickname);
        data.put("gamesPlayed", 0);
        data.put("bestScore", 0);
        data.put("updatedAt", System.currentTimeMillis());
        firestore.collection("users")
                .document(uid)
                .set(data)
                .addOnSuccessListener(ignored -> callback.onSuccess(user))
                .addOnFailureListener(error -> callback.onSuccess(user));
    }
}
