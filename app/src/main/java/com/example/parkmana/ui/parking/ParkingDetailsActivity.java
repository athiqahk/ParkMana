package com.example.parkmana.ui.parking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.parkmana.R;
import com.example.parkmana.ui.navigation.NavigationActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ParkingDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_PARKING_ITEM = "parking_item";
    public static final String EXTRA_USER_LAT = "user_lat";
    public static final String EXTRA_USER_LNG = "user_lng";

    private static final int MAX_PHOTO_WIDTH = 800;
    private static final int JPEG_QUALITY = 60;
    private static final int MAX_FIRESTORE_BYTES = 900_000; // stay under 1 MiB doc limit

    private ParkingItem parking;
    private double userLatitude;
    private double userLongitude;
    private Uri pendingPhotoUri;
    private boolean isFavourite;

    private ImageView photoPreview;
    private TextView uploadStatus;
    private Button favouriteButton;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) launchCamera();
                        else Toast.makeText(this,
                                "Camera permission is required to add a photo.",
                                Toast.LENGTH_LONG).show();
                    });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success && pendingPhotoUri != null) {
                            photoPreview.setImageURI(pendingPhotoUri);
                            photoPreview.setVisibility(View.VISIBLE);
                            savePhotoToFirestore(pendingPhotoUri);
                        } else {
                            uploadStatus.setText("Photo capture cancelled");
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_details);

        if (!readParking()) {
            Toast.makeText(this, "Parking details are unavailable.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        userLatitude = getIntent().getDoubleExtra(EXTRA_USER_LAT, 0d);
        userLongitude = getIntent().getDoubleExtra(EXTRA_USER_LNG, 0d);

        bindViews();
        displayParking();
        initializeActions();
        loadFavouriteState();
        loadExistingPhoto();
    }

    private boolean readParking() {
        Serializable value = getIntent().getSerializableExtra(EXTRA_PARKING_ITEM);
        if (value instanceof ParkingItem) {
            parking = (ParkingItem) value;
            return true;
        }
        return false;
    }

    private void bindViews() {
        photoPreview = findViewById(R.id.parkingPhotoPreview);
        uploadStatus = findViewById(R.id.parkingPhotoStatus);
        favouriteButton = findViewById(R.id.parkingFavouriteButton);
    }

    private void displayParking() {
        ((TextView) findViewById(R.id.parkingDetailsName)).setText(parking.getName());
        ((TextView) findViewById(R.id.parkingDetailsAddress)).setText(
                isBlank(parking.getAddress()) ? "Address not provided" : parking.getAddress());
        ((TextView) findViewById(R.id.parkingDetailsDistance)).setText(
                formatDistance(parking.getDistanceMeters()));

        TextView rating = findViewById(R.id.parkingDetailsRating);
        if (parking.getRating() >= 0) {
            rating.setText(String.format(Locale.getDefault(),
                    "Rating %.1f (%d)", parking.getRating(), parking.getRatingCount()));
        } else {
            rating.setText("Rating unavailable");
        }

        TextView hours = findViewById(R.id.parkingDetailsHours);
        if (parking.getOpenNow() == null) hours.setText("Opening hours unknown");
        else hours.setText(parking.getOpenNow() ? "Open now" : "Closed now");
    }

    private void initializeActions() {
        findViewById(R.id.parkingDetailsBack).setOnClickListener(view -> finish());
        findViewById(R.id.parkingAddPhotoButton).setOnClickListener(view -> requestCamera());
        favouriteButton.setOnClickListener(view -> toggleFavourite());
        findViewById(R.id.parkingDetailsNavigateButton).setOnClickListener(
                view -> openNavigation());
    }

    // =====================================================
    // Camera + per-user photo (users/{uid}/parking_photos/{parkingId})
    // =====================================================

    private void requestCamera() {
        if (currentUser() == null) {
            Toast.makeText(this, "Please sign in to add a photo.", Toast.LENGTH_LONG).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (directory == null) directory = getFilesDir();
            File photo = File.createTempFile("parking_", ".jpg", directory);
            pendingPhotoUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", photo);
            takePictureLauncher.launch(pendingPhotoUri);
        } catch (IOException error) {
            Toast.makeText(this, "Unable to create a photo file.", Toast.LENGTH_LONG).show();
        }
    }

    private void savePhotoToFirestore(Uri photoUri) {
        FirebaseUser user = currentUser();
        if (user == null) {
            uploadStatus.setText("Please sign in to save a photo.");
            return;
        }

        uploadStatus.setText("Saving photo...");
        try {
            String base64Image = compressToBase64(photoUri);
            if (base64Image == null) {
                uploadStatus.setText("Photo is too large even after compression.");
                return;
            }

            Map<String, Object> photoData = new HashMap<>();
            photoData.put("parkingId", parkingId());
            photoData.put("parkingName", parking.getName());
            photoData.put("parkingLatitude", parking.getLatitude());
            photoData.put("parkingLongitude", parking.getLongitude());
            photoData.put("imageBase64", base64Image);
            photoData.put("createdAt", FieldValue.serverTimestamp());

            photoDocument(user).set(photoData)
                    .addOnSuccessListener(unused ->
                            uploadStatus.setText("Your photo is saved for this parking"))
                    .addOnFailureListener(error ->
                            uploadStatus.setText("Save failed: " + readableMessage(error)));
        } catch (IOException error) {
            uploadStatus.setText("Could not read the photo: " + readableMessage(error));
        }
    }

    private void loadExistingPhoto() {
        FirebaseUser user = currentUser();
        if (user == null) return;

        photoDocument(user).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;
                    String base64 = document.getString("imageBase64");
                    if (base64 == null) return;
                    byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                    photoPreview.setImageBitmap(
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                    photoPreview.setVisibility(View.VISIBLE);
                    uploadStatus.setText("Your photo for this parking");
                });
    }

    private DocumentReference photoDocument(FirebaseUser user) {
        return FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("parking_photos").document(parkingId());
    }

    private String compressToBase64(Uri photoUri) throws IOException {
        Bitmap original;
        try (InputStream input = getContentResolver().openInputStream(photoUri)) {
            original = BitmapFactory.decodeStream(input);
        }
        if (original == null) throw new IOException("Could not decode the photo");

        Bitmap scaled = original;
        if (original.getWidth() > MAX_PHOTO_WIDTH) {
            int height = (int) (original.getHeight()
                    * ((float) MAX_PHOTO_WIDTH / original.getWidth()));
            scaled = Bitmap.createScaledBitmap(original, MAX_PHOTO_WIDTH, height, true);
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream);
        byte[] bytes = stream.toByteArray();

        // Base64 adds ~33% overhead; check the encoded size fits in a Firestore doc.
        if (bytes.length * 4L / 3L > MAX_FIRESTORE_BYTES) return null;

        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // =====================================================
    // Per-user favourites (users/{uid}/favourites/{parkingId})
    // =====================================================

    private void loadFavouriteState() {
        updateFavouriteButton();
        FirebaseUser user = currentUser();
        if (user == null) return;

        favouriteDocument(user).get()
                .addOnSuccessListener(document -> {
                    isFavourite = document.exists();
                    updateFavouriteButton();
                });
    }

    private void toggleFavourite() {
        FirebaseUser user = currentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to save favourites.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        favouriteButton.setEnabled(false);
        if (isFavourite) {
            favouriteDocument(user).delete()
                    .addOnSuccessListener(unused -> {
                        isFavourite = false;
                        favouriteButton.setEnabled(true);
                        updateFavouriteButton();
                    })
                    .addOnFailureListener(error -> {
                        favouriteButton.setEnabled(true);
                        Toast.makeText(this, "Could not remove favourite.",
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("parkingId", parkingId());
            data.put("name", parking.getName());
            data.put("address", parking.getAddress());
            data.put("latitude", parking.getLatitude());
            data.put("longitude", parking.getLongitude());
            data.put("savedAt", FieldValue.serverTimestamp());

            favouriteDocument(user).set(data)
                    .addOnSuccessListener(unused -> {
                        isFavourite = true;
                        favouriteButton.setEnabled(true);
                        updateFavouriteButton();
                    })
                    .addOnFailureListener(error -> {
                        favouriteButton.setEnabled(true);
                        Toast.makeText(this, "Could not save favourite.",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private DocumentReference favouriteDocument(FirebaseUser user) {
        return FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("favourites").document(parkingId());
    }

    private void updateFavouriteButton() {
        favouriteButton.setText(isFavourite ? "Remove Favourite" : "Add to Favourite");
    }

    // =====================================================
    // Helpers
    // =====================================================

    private FirebaseUser currentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    private String parkingId() {
        return String.format(Locale.US, "%.6f_%.6f",
                        parking.getLatitude(), parking.getLongitude())
                .replace('-', 'm').replace('.', '_');
    }

    private void openNavigation() {
        Intent intent = new Intent(this, NavigationActivity.class);
        intent.putExtra("user_lat", userLatitude);
        intent.putExtra("user_lng", userLongitude);
        intent.putExtra("parking_lat", parking.getLatitude());
        intent.putExtra("parking_lng", parking.getLongitude());
        intent.putExtra("parking_name", parking.getName());
        startActivity(intent);
    }

    private String formatDistance(int meters) {
        if (meters < 1000) return meters + " m away";
        return String.format(Locale.getDefault(), "%.1f km away", meters / 1000f);
    }

    private String readableMessage(Exception error) {
        return isBlank(error.getMessage()) ? "please try again" : error.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}