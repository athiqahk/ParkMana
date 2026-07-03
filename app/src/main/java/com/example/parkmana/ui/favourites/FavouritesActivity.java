package com.example.parkmana.ui.favourites;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Locale;

public class FavouritesActivity extends AppCompatActivity {

    private FavouriteAdapter adapter;
    private TextView emptyText;
    private TextView countText;
    private EditText searchInput;

    /** Full unfiltered list; the adapter shows the filtered view of it. */
    private final List<FavouriteParking> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);
        BottomNavHelper.setup(this, BottomNavHelper.TAB_SAVED);

        emptyText = findViewById(R.id.favouritesEmptyText);
        countText = findViewById(R.id.favouritesCount);
        searchInput = findViewById(R.id.favouritesSearch);
        findViewById(R.id.favouritesBack).setOnClickListener(view -> finish());

        RecyclerView recyclerView = findViewById(R.id.favouritesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavouriteAdapter(this::removeFavourite);
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                applySearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
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
                    allItems.clear();
                    snapshot.forEach(document -> {
                        FavouriteParking item = new FavouriteParking();
                        item.id = document.getId();
                        item.name = document.getString("name");
                        item.address = document.getString("address");
                        Double lat = document.getDouble("latitude");
                        Double lng = document.getDouble("longitude");
                        item.latitude = lat == null ? 0 : lat;
                        item.longitude = lng == null ? 0 : lng;
                        allItems.add(item);
                    });
                    applySearch(searchInput.getText().toString());
                })
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Could not load favourites.", Toast.LENGTH_SHORT).show());
    }

    private void applySearch(String query) {
        String needle = query == null
                ? "" : query.trim().toLowerCase(Locale.getDefault());

        List<FavouriteParking> visible = new ArrayList<>();
        for (FavouriteParking item : allItems) {
            if (needle.isEmpty() || matches(item, needle)) {
                visible.add(item);
            }
        }

        adapter.submitList(visible);
        countText.setText(visible.size() + " favourite parking spots");
        emptyText.setText(allItems.isEmpty()
                ? "No favourites yet.\nTap 'Add to Favourite' on any parking to save it here."
                : "No favourites match your search.");
        emptyText.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean matches(FavouriteParking item, String needle) {
        return (item.name != null
                && item.name.toLowerCase(Locale.getDefault()).contains(needle))
                || (item.address != null
                && item.address.toLowerCase(Locale.getDefault()).contains(needle));
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
