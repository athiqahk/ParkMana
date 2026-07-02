package com.example.parkmana.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.parkmana.R;
import com.example.parkmana.ui.auth.LoginActivity;
import com.example.parkmana.ui.favourites.FavouritesActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameText;
    private TextView emailText;
    private TextView favouriteCountText;
    private TextView photoCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to view your profile.",
                    Toast.LENGTH_LONG).show();
            goToLogin();
            return;
        }

        bindViews();
        displayUser(user);
        initializeActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) loadCounts(user);
    }

    private void bindViews() {
        nameText = findViewById(R.id.profileName);
        emailText = findViewById(R.id.profileEmail);
        favouriteCountText = findViewById(R.id.profileFavouriteCount);
        photoCountText = findViewById(R.id.profilePhotoCount);
    }

    private void displayUser(FirebaseUser user) {
        String displayName = user.getDisplayName();
        nameText.setText(displayName == null || displayName.trim().isEmpty()
                ? "ParkMana User" : displayName);
        emailText.setText(user.getEmail() == null ? "No email" : user.getEmail());
    }

    private void loadCounts(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(user.getUid())
                .collection("favourites").get()
                .addOnSuccessListener(snapshot ->
                        favouriteCountText.setText(String.valueOf(snapshot.size())));

        db.collection("users").document(user.getUid())
                .collection("parking_photos").get()
                .addOnSuccessListener(snapshot ->
                        photoCountText.setText(String.valueOf(snapshot.size())));
    }

    private void initializeActions() {
        findViewById(R.id.profileBack).setOnClickListener(view -> finish());

        findViewById(R.id.profileFavouritesButton).setOnClickListener(view ->
                startActivity(new Intent(this, FavouritesActivity.class)));

        findViewById(R.id.profileSignOutButton).setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Signed out.", Toast.LENGTH_SHORT).show();
            goToLogin();
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}