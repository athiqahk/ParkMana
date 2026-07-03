package com.example.parkmana.ui.profile;

import android.os.Bundle;
import android.text.InputType;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPhotosActivity extends AppCompatActivity {

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
                .collection("users").document(user.getUid())
                .collection("parking_photos")
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
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Could not load photos.", Toast.LENGTH_SHORT).show());
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
                            .collection("users").document(user.getUid())
                            .collection("parking_photos").document(photo.id)
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
                            .collection("users").document(user.getUid())
                            .collection("parking_photos").document(photo.id)
                            .delete()
                            .addOnSuccessListener(unused -> loadPhotos())
                            .addOnFailureListener(error -> Toast.makeText(this,
                                    "Could not delete photo.",
                                    Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
