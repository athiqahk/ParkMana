package com.example.parkmana.ui.auth;

import android.app.Activity;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.example.parkmana.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Handles signing the current user out of Firebase Authentication.
 *
 * Usage (e.g. in ProfileActivity, on a "Sign Out" button):
 *   findViewById(R.id.profileSignOutButton).setOnClickListener(
 *       view -> SignOutHelper.confirmAndSignOut(this));
 */
public final class SignOutHelper {

    private SignOutHelper() {
    }

    /** Shows a confirmation dialog before signing out, so a stray tap can't log the user out. */
    public static void confirmAndSignOut(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Sign out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> signOut(activity))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Signs out immediately, no confirmation. Use this if you already confirm elsewhere. */
    public static void signOut(Activity activity) {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(activity, LoginActivity.class);
        // Clears the entire back stack so the user can't press "back" into
        // the app after signing out.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
