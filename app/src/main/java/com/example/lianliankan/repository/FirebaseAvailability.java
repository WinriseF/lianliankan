package com.example.lianliankan.repository;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public final class FirebaseAvailability {

    private FirebaseAvailability() {
    }

    public static boolean isConfigured(Context context) {
        return !FirebaseApp.getApps(context.getApplicationContext()).isEmpty();
    }

    public static FirebaseAuth auth(Context context) {
        return isConfigured(context) ? FirebaseAuth.getInstance() : null;
    }

    public static FirebaseFirestore firestore(Context context) {
        return isConfigured(context) ? FirebaseFirestore.getInstance() : null;
    }
}
