package com.example.parkmana.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthRepository — the data layer for authentication.
 *
 * The ONLY class that talks directly to Firebase Auth (and Firestore for the
 * user's profile). The ViewModel and screens always go through here.
 */
public class AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String message);
    }

    /** Signs in with an email and password. */
    public void login(@NonNull String email, @NonNull String password,
                      @NonNull AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /** Creates a new account, saves the display name, and stores the user in Firestore. */
    public void register(@NonNull String name, @NonNull String email, @NonNull String password,
                         @NonNull AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError("Account created but the user could not be loaded.");
                        return;
                    }

                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    user.updateProfile(profile);

                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("email", email);
                    firestore.collection("users").document(user.getUid()).set(data);

                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Signs into Firebase using a Google ID token (obtained via Credential Manager).
     * Also stores the user in Firestore if this is their first sign-in.
     */
    public void signInWithGoogle(@NonNull String idToken, @NonNull AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        // Save/refresh the user's record in Firestore.
                        Map<String, Object> data = new HashMap<>();
                        data.put("name", user.getDisplayName());
                        data.put("email", user.getEmail());
                        firestore.collection("users").document(user.getUid())
                                .set(data);
                    }
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /** Sends a password-reset email. */
    public void sendPasswordReset(@NonNull String email, @NonNull AuthCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
}