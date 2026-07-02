package com.example.parkmana.ui.favourites;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkmana.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class FavouritesActivity extends AppCompatActivity {

    private FavouriteAdapter adapter;
    private TextView emptyText;
    private TextView countText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);

        emptyText = findViewById(R.id.favouritesEmptyText);
        countText = findViewById(R.id.favouritesCount);
        findViewById(R.id.favouritesBack).setOnClickListener(view -> finish());

        RecyclerView recyclerView = findViewById(R.id.favouritesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavouriteAdapter(this::removeFavourite);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavourites();
    }

    private void loadFavourites() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to view favourites.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("favourites")
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<FavouriteParking> items = new ArrayList<>();
                    snapshot.forEach(document -> {
                        FavouriteParking item = new FavouriteParking();
                        item.id = document.getId();
                        item.name = document.getString("name");
                        item.address = document.getString("address");
                        Double lat = document.getDouble("latitude");
                        Double lng = document.getDouble("longitude");
                        item.latitude = lat == null ? 0 : lat;
                        item.longitude = lng == null ? 0 : lng;
                        items.add(item);
                    });
                    showItems(items);
                })
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Could not load favourites.", Toast.LENGTH_SHORT).show());
    }

    private void showItems(List<FavouriteParking> items) {
        adapter.submitList(items);
        countText.setText(items.size() + " favourite parking spots");
        emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void removeFavourite(FavouriteParking item) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("favourites").document(item.id)
                .delete()
                .addOnSuccessListener(unused -> loadFavourites())
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Could not remove favourite.", Toast.LENGTH_SHORT).show());
    }
}