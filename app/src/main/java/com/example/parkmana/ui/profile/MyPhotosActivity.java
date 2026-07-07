package com.example.parkmana.ui.profile;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkmana.R;
import com.example.parkmana.ui.BottomNavHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows the signed-in user's own posted parking updates — a private view
 * of their own uploads, filtered from the shared parking_reports collection
 * by uploaderUid. Other users' reports never appear here even though the
 * collection itself is readable by anyone (that's what makes them visible
 * on ParkingDetailsActivity for everyone).
 *
 * NOTE: whereEqualTo("uploaderUid", ...) + orderBy("createdAt", ...) is a
 * *different* field combination than the parkingId + createdAt query used
 * in ParkingDetailsActivity, so it needs its own separate Firestore
 * composite index. If this screen shows "Could not load photos", check
 * Logcat (tag "MyPhotos") for a FAILED_PRECONDITION error containing a
 * direct link to create that index — or create it manually in Firebase
 * Console → Firestore → Indexes: collection "parking_reports",
 * fields uploaderUid (Ascending) + createdAt (Descending).
 */
public class MyPhotosActivity extends AppCompatActivity {

    private static final String TAG = "MyPhotos";
    private static final String REPORTS_COLLECTION = "parking_reports";

    private PhotoAdapter adapter;
    private TextView emptyText;
    private TextView countText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_photos);
        BottomNavHelper.setup(this, BottomNavHelper.TAB_PROFILE);

        emptyText = findViewById(R.id.photosEmptyText);
        countText = findViewById(R.id.photosCount);
        findViewById(R.id.photosBack).setOnClickListener(view -> finish());

        RecyclerView recyclerView = findViewById(R.id.photosRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PhotoAdapter(this::showEditDescriptionDialog, this::confirmDelete);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPhotos();
    }

    private void loadPhotos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to view your photos.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(REPORTS_COLLECTION)
                .whereEqualTo("uploaderUid", user.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ParkingPhoto> items = new ArrayList<>();
                    snapshot.forEach(document -> {
                        ParkingPhoto photo = new ParkingPhoto();
                        photo.id = document.getId();
                        photo.parkingName = document.getString("parkingName");
                        photo.description = document.getString("description");
                        photo.imageBase64 = document.getString("imageBase64");
                        items.add(photo);
                    });
                    showItems(items);
                })
                .addOnFailureListener(error -> {
                    // Surfacing the real error (usually FAILED_PRECONDITION
                    // with an index-creation link) instead of hiding it
                    // behind a generic message.
                    Log.e(TAG, "Loading parking_reports by uploaderUid failed", error);
                    Toast.makeText(this,
                            "Could not load photos: " + readableMessage(error),
                            Toast.LENGTH_LONG).show();
                    emptyText.setText("Could not load photos: " + readableMessage(error));
                    emptyText.setVisibility(View.VISIBLE);
                });
    }

    private void showItems(List<ParkingPhoto> items) {
        adapter.submitList(items);
        countText.setText(items.size() + " parking photos");
        emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showEditDescriptionDialog(ParkingPhoto photo) {
        EditText input = new EditText(this);
        input.setHint("Description");
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(photo.description == null ? "" : photo.description);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);
        layout.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Edit description")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) return;

                    Map<String, Object> update = new HashMap<>();
                    update.put("description", input.getText().toString().trim());

                    FirebaseFirestore.getInstance()
                            .collection(REPORTS_COLLECTION).document(photo.id)
                            .update(update)
                            .addOnSuccessListener(unused -> loadPhotos())
                            .addOnFailureListener(error -> Toast.makeText(this,
                                    "Could not update description.",
                                    Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(ParkingPhoto photo) {
        new AlertDialog.Builder(this)
                .setTitle("Delete photo")
                .setMessage("Delete your photo for \""
                        + (photo.parkingName == null ? "this parking" : photo.parkingName)
                        + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) return;

                    FirebaseFirestore.getInstance()
                            .collection(REPORTS_COLLECTION).document(photo.id)
                            .delete()
                            .addOnSuccessListener(unused -> loadPhotos())
                            .addOnFailureListener(error -> Toast.makeText(this,
                                    "Could not delete photo.",
                                    Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String readableMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? "please try again" : message;
    }
}