package com.example.parkmana.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.parkmana.R;
import com.example.parkmana.ui.BottomNavHelper;
import com.example.parkmana.ui.auth.SignOutHelper;
import com.example.parkmana.ui.auth.LoginActivity;
import com.example.parkmana.ui.favourites.FavouritesActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int MAX_IMAGE_WIDTH = 400;
    private static final int JPEG_QUALITY = 70;

    private ImageView profileImage;
    private TextView nameText;
    private TextView emailText;
    private TextView favouriteCountText;
    private TextView photoCountText;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) saveProfileImage(uri);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        BottomNavHelper.setup(this, BottomNavHelper.TAB_PROFILE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to view your profile.",
                    Toast.LENGTH_LONG).show();
            goToLogin();
            return;
        }

        bindViews();
        displayUser(user);
        loadProfileImage(user);
        initializeActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) loadCounts(user);
    }

    private void bindViews() {
        profileImage = findViewById(R.id.profileImage);
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

        db.collection("parking_reports")
                .whereEqualTo("uploaderUid", user.getUid())
                .get()
                .addOnSuccessListener(snapshot ->
                        photoCountText.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(error ->
                        photoCountText.setText("0"));
    }

    private void initializeActions() {
        findViewById(R.id.profileBack).setOnClickListener(view -> finish());

        findViewById(R.id.profileChangePhoto).setOnClickListener(view ->
                pickImageLauncher.launch("image/*"));

        findViewById(R.id.profileRemovePhoto).setOnClickListener(view ->
                removeProfileImage());

        findViewById(R.id.profileEditName).setOnClickListener(view ->
                showEditNameDialog());

        findViewById(R.id.profileEditEmail).setOnClickListener(view ->
                showEditEmailDialog());

        findViewById(R.id.profileChangePasswordButton).setOnClickListener(view ->
                showChangePasswordDialog());

        findViewById(R.id.profileFavouritesButton).setOnClickListener(view ->
                startActivity(new Intent(this, FavouritesActivity.class)));

        findViewById(R.id.profileMyPhotosButton).setOnClickListener(view ->
                startActivity(new Intent(this, MyPhotosActivity.class)));

        findViewById(R.id.profileSignOutButton).setOnClickListener(view ->
                SignOutHelper.confirmAndSignOut(this));
    }

    // =====================================================
    // Profile picture (stored as Base64 in users/{uid})
    // =====================================================

    private void saveProfileImage(Uri imageUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        try {
            String base64 = compressToBase64(imageUri);
            if (base64 == null) {
                Toast.makeText(this, "Image is too large.", Toast.LENGTH_LONG).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("profileImageBase64", base64);

            FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid())
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        showBase64Image(base64);
                        Toast.makeText(this, "Profile photo updated.",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(error ->
                            Toast.makeText(this, "Could not save profile photo.",
                                    Toast.LENGTH_SHORT).show());
        } catch (IOException error) {
            Toast.makeText(this, "Could not read the image.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void removeProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put("profileImageBase64", FieldValue.delete());

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .set(update, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    profileImage.setImageResource(android.R.drawable.sym_def_app_icon);
                    Toast.makeText(this, "Profile photo removed.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfileImage(FirebaseUser user) {
        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    String base64 = document.getString("profileImageBase64");
                    if (base64 != null) showBase64Image(base64);
                });
    }

    private void showBase64Image(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        profileImage.setImageBitmap(
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
    }

    private String compressToBase64(Uri imageUri) throws IOException {
        Bitmap original;
        try (InputStream input = getContentResolver().openInputStream(imageUri)) {
            original = BitmapFactory.decodeStream(input);
        }
        if (original == null) throw new IOException("Could not decode image");

        Bitmap scaled = original;
        if (original.getWidth() > MAX_IMAGE_WIDTH) {
            int height = (int) (original.getHeight()
                    * ((float) MAX_IMAGE_WIDTH / original.getWidth()));
            scaled = Bitmap.createScaledBitmap(original, MAX_IMAGE_WIDTH, height, true);
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream);
        byte[] bytes = stream.toByteArray();

        if (bytes.length * 4L / 3L > 900_000) return null;
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // =====================================================
    // Edit username
    // =====================================================

    private void showEditNameDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        EditText input = makeInput("Username", InputType.TYPE_CLASS_TEXT);
        input.setText(user.getDisplayName());

        new AlertDialog.Builder(this)
                .setTitle("Edit username")
                .setView(wrapInputs(input))
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Username cannot be empty.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    UserProfileChangeRequest request =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(newName)
                                    .build();
                    user.updateProfile(request)
                            .addOnSuccessListener(unused -> {
                                nameText.setText(newName);
                                // Keep a copy in Firestore too
                                Map<String, Object> data = new HashMap<>();
                                data.put("username", newName);
                                FirebaseFirestore.getInstance()
                                        .collection("users").document(user.getUid())
                                        .set(data, SetOptions.merge());
                                Toast.makeText(this, "Username updated.",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(error ->
                                    Toast.makeText(this, "Could not update username.",
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =====================================================
    // Edit email (requires current password + email verification)
    // =====================================================

    private void showEditEmailDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        EditText newEmailInput = makeInput("New email",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText passwordInput = makeInput("Current password",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Change email")
                .setMessage("A verification link will be sent to the new email. "
                        + "The change applies after you click it.")
                .setView(wrapInputs(newEmailInput, passwordInput))
                .setPositiveButton("Send verification", (dialog, which) -> {
                    String newEmail = newEmailInput.getText().toString().trim();
                    String password = passwordInput.getText().toString();

                    if (newEmail.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this, "Please fill in both fields.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AuthCredential credential = EmailAuthProvider
                            .getCredential(user.getEmail(), password);

                    user.reauthenticate(credential)
                            .addOnSuccessListener(unused ->
                                    user.verifyBeforeUpdateEmail(newEmail)
                                            .addOnSuccessListener(v ->
                                                    Toast.makeText(this,
                                                            "Verification email sent to "
                                                                    + newEmail
                                                                    + ". Check the inbox.",
                                                            Toast.LENGTH_LONG).show())
                                            .addOnFailureListener(error ->
                                                    Toast.makeText(this,
                                                            "Could not update email: "
                                                                    + error.getMessage(),
                                                            Toast.LENGTH_LONG).show()))
                            .addOnFailureListener(error ->
                                    Toast.makeText(this,
                                            "Current password is incorrect.",
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =====================================================
    // Change password (requires current password)
    // =====================================================

    private void showChangePasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        EditText currentInput = makeInput("Current password",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText newInput = makeInput("New password (min 6 characters)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Change password")
                .setView(wrapInputs(currentInput, newInput))
                .setPositiveButton("Update", (dialog, which) -> {
                    String current = currentInput.getText().toString();
                    String updated = newInput.getText().toString();

                    if (updated.length() < 6) {
                        Toast.makeText(this,
                                "New password must be at least 6 characters.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AuthCredential credential = EmailAuthProvider
                            .getCredential(user.getEmail(), current);

                    user.reauthenticate(credential)
                            .addOnSuccessListener(unused ->
                                    user.updatePassword(updated)
                                            .addOnSuccessListener(v ->
                                                    Toast.makeText(this,
                                                            "Password updated.",
                                                            Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(error ->
                                                    Toast.makeText(this,
                                                            "Could not update password: "
                                                                    + error.getMessage(),
                                                            Toast.LENGTH_LONG).show()))
                            .addOnFailureListener(error ->
                                    Toast.makeText(this,
                                            "Current password is incorrect.",
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =====================================================
    // Dialog helpers
    // =====================================================

    private EditText makeInput(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(inputType);
        return input;
    }

    private LinearLayout wrapInputs(EditText... inputs) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);
        for (EditText input : inputs) layout.addView(input);
        return layout;
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}