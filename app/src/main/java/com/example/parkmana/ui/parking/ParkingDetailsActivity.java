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
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkmana.BuildConfig;
import com.example.parkmana.R;
import com.example.parkmana.ui.navigation.NavigationActivity;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.Review;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParkingDetailsActivity extends AppCompatActivity {

    private static final String TAG = "ParkingDetails";

    public static final String EXTRA_PARKING_ITEM = "parking_item";
    public static final String EXTRA_USER_LAT = "user_lat";
    public static final String EXTRA_USER_LNG = "user_lng";

    private static final int MAX_PHOTO_WIDTH = 800;
    private static final int JPEG_QUALITY = 60;
    private static final int MAX_FIRESTORE_BYTES = 900_000; // stay under 1 MiB doc limit
    private static final String REPORTS_COLLECTION = "parking_reports";

    private ParkingItem parking;
    private double userLatitude;
    private double userLongitude;
    private Uri pendingPhotoUri;
    private boolean isFavourite;

    private ImageView photoPreview;
    private TextView uploadStatus;
    private EditText photoDescription;
    private ImageButton favouriteButton;

    private RecyclerView reviewsRecyclerView;
    private TextView reviewsCountLabel;
    private TextView reviewsEmptyLabel;
    private ReviewsAdapter reviewsAdapter;
    private PlacesClient placesClient;

    private RecyclerView reportsRecyclerView;
    private TextView reportsEmptyLabel;
    private ParkingReportsAdapter reportsAdapter;

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
                            postReport(pendingPhotoUri);
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
        setupReviews();
        loadReviews();
        setupReports();
        refreshReports();
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
        photoDescription = findViewById(R.id.parkingPhotoDescription);
        favouriteButton = findViewById(R.id.parkingFavouriteButton);

        reviewsRecyclerView = findViewById(R.id.parkingReviewsRecyclerView);
        reviewsCountLabel = findViewById(R.id.reviewsCountLabel);
        reviewsEmptyLabel = findViewById(R.id.reviewsEmptyLabel);

        reportsRecyclerView = findViewById(R.id.parkingReportsRecyclerView);
        reportsEmptyLabel = findViewById(R.id.reportsEmptyLabel);
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
    // Google reviews for the currently opened parking location — shown
    // entirely in-app via the RecyclerView below. Note: Google's Places
    // API caps review data at a maximum of 5 "most relevant" reviews per
    // place, with no pagination for more — that's a platform limit, so
    // this list is the full extent of what's available through the API.
    // =====================================================

    private void setupReviews() {
        reviewsAdapter = new ReviewsAdapter(new ArrayList<>());
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(reviewsAdapter);
        reviewsRecyclerView.setNestedScrollingEnabled(false);
        reviewsRecyclerView.setVisibility(View.GONE);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }
        placesClient = Places.createClient(this);
    }

    private void loadReviews() {
        String placeId = parking.getPlaceId();
        if (isBlank(placeId)) {
            showReviewsMessage("Reviews unavailable for this location");
            return;
        }

        reviewsEmptyLabel.setText("Loading reviews...");
        reviewsEmptyLabel.setVisibility(View.VISIBLE);

        List<Place.Field> fields = Arrays.asList(
                Place.Field.RATING, Place.Field.USER_RATINGS_TOTAL, Place.Field.REVIEWS);
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, fields);

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();

                    Integer totalRatings = place.getUserRatingsTotal();
                    reviewsCountLabel.setText(
                            totalRatings != null ? "(" + totalRatings + ")" : "");

                    List<Review> placeReviews = place.getReviews();
                    if (placeReviews == null || placeReviews.isEmpty()) {
                        showReviewsMessage("No reviews yet");
                        return;
                    }

                    List<ParkingReview> items = new ArrayList<>();
                    for (Review review : placeReviews) {
                        String authorName = (review.getAuthorAttribution() != null
                                && !isBlank(review.getAuthorAttribution().getName()))
                                ? review.getAuthorAttribution().getName()
                                : "Anonymous";
                        String photoUrl = review.getAuthorAttribution() != null
                                && review.getAuthorAttribution().getPhotoUri() != null
                                ? review.getAuthorAttribution().getPhotoUri().toString()
                                : null;
                        float rating = review.getRating() != null
                                ? review.getRating().floatValue() : 0f;
                        String relativeTime = review.getRelativePublishTimeDescription();
                        String text = review.getText();

                        items.add(new ParkingReview(authorName, photoUrl, rating, relativeTime, text));
                    }

                    reviewsEmptyLabel.setVisibility(View.GONE);
                    reviewsRecyclerView.setVisibility(View.VISIBLE);
                    reviewsAdapter.submit(items);
                })
                .addOnFailureListener(error -> {
                    // Logged so a genuine failure (API not enabled, quota,
                    // bad key restriction, etc.) is visible in Logcat instead
                    // of looking identical to "the place just has no reviews".
                    Log.e(TAG, "fetchPlace(REVIEWS) failed for placeId=" + placeId, error);
                    showReviewsMessage("Could not load reviews: " + readableMessage(error));
                });
    }

    private void showReviewsMessage(String message) {
        reviewsEmptyLabel.setText(message);
        reviewsEmptyLabel.setVisibility(View.VISIBLE);
        reviewsRecyclerView.setVisibility(View.GONE);
    }

    // =====================================================
    // Community updates: shared across every user (parking_reports collection)
    // =====================================================

    private void setupReports() {
        reportsAdapter = new ParkingReportsAdapter(new ArrayList<>());
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportsRecyclerView.setAdapter(reportsAdapter);
        reportsRecyclerView.setNestedScrollingEnabled(false);
        reportsRecyclerView.setVisibility(View.GONE);
    }

    private void refreshReports() {
        reportsEmptyLabel.setText("Loading updates...");
        reportsEmptyLabel.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection(REPORTS_COLLECTION)
                .whereEqualTo("parkingId", parkingId())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ParkingReport> items = new ArrayList<>();
                    snapshot.forEach(document -> {
                        String uploaderName = document.getString("uploaderName");
                        String description = document.getString("description");
                        String photoBase64 = document.getString("imageBase64");
                        Timestamp timestamp = document.getTimestamp("createdAt");
                        long millis = timestamp != null
                                ? timestamp.toDate().getTime() : 0L;

                        items.add(new ParkingReport(document.getId(),
                                isBlank(uploaderName) ? "A ParkMana user" : uploaderName,
                                description, photoBase64, millis));
                    });

                    if (items.isEmpty()) {
                        reportsEmptyLabel.setText(
                                "No updates yet. Be the first to help other drivers.");
                        reportsEmptyLabel.setVisibility(View.VISIBLE);
                        reportsRecyclerView.setVisibility(View.GONE);
                    } else {
                        reportsEmptyLabel.setVisibility(View.GONE);
                        reportsRecyclerView.setVisibility(View.VISIBLE);
                        reportsAdapter.submit(items);
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Loading parking_reports failed for parkingId="
                            + parkingId(), error);
                    reportsEmptyLabel.setText(
                            "Could not load updates: " + readableMessage(error));
                    reportsEmptyLabel.setVisibility(View.VISIBLE);
                    reportsRecyclerView.setVisibility(View.GONE);
                });
    }

    // =====================================================
    // Camera + posting a shared update
    // =====================================================

    private void requestCamera() {
        if (currentUser() == null) {
            Toast.makeText(this, "Please sign in to post an update.", Toast.LENGTH_LONG).show();
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

    /**
     * Posts a shared update visible to every user who opens this parking
     * spot (not a private per-user photo). The uploader's display name and
     * server timestamp are stored alongside it so others can see who
     * reported it and when. The same document is what MyPhotosActivity
     * queries (filtered by uploaderUid) to show a user's own private
     * history of what they've posted.
     */
    private void postReport(Uri photoUri) {
        FirebaseUser user = currentUser();
        if (user == null) {
            uploadStatus.setText("Please sign in to post an update.");
            return;
        }

        uploadStatus.setText("Posting update...");
        try {
            String base64Image = compressToBase64(photoUri);
            if (base64Image == null) {
                uploadStatus.setText("Photo is too large even after compression.");
                return;
            }

            String uploaderName = isBlank(user.getDisplayName())
                    ? "A ParkMana user" : user.getDisplayName();

            Map<String, Object> reportData = new HashMap<>();
            reportData.put("parkingId", parkingId());
            reportData.put("parkingName", parking.getName());
            reportData.put("uploaderUid", user.getUid());
            reportData.put("uploaderName", uploaderName);
            reportData.put("description", photoDescription.getText().toString().trim());
            reportData.put("imageBase64", base64Image);
            reportData.put("createdAt", FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance()
                    .collection(REPORTS_COLLECTION)
                    .add(reportData)
                    .addOnSuccessListener(ref -> {
                        uploadStatus.setText("Update posted — thanks for helping other drivers!");
                        photoDescription.setText("");
                        photoPreview.setVisibility(View.GONE);
                        refreshReports();
                    })
                    .addOnFailureListener(error -> {
                        Log.e(TAG, "Posting parking_reports failed", error);
                        uploadStatus.setText("Could not post update: " + readableMessage(error));
                    });
        } catch (IOException error) {
            uploadStatus.setText("Could not read the photo: " + readableMessage(error));
        }
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

    /**
     * Swaps between the outline heart (not saved) and the solid red heart
     * (saved) — two separate drawables, no runtime tinting, so the shape
     * never gets stretched or double-drawn.
     */
    private void updateFavouriteButton() {
        favouriteButton.setImageResource(
                isFavourite ? R.drawable.ic_favourite_filled : R.drawable.ic_favourite_outline);
        favouriteButton.setContentDescription(
                isFavourite ? "Remove from favourites" : "Add to favourites");
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