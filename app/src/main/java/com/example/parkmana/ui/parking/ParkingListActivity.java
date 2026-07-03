package com.example.parkmana.ui.parking;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkmana.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ParkingListActivity extends AppCompatActivity {

    public static final String EXTRA_PARKING_ITEMS = "parking_items";
    public static final String EXTRA_USER_LAT = "user_lat";
    public static final String EXTRA_USER_LNG = "user_lng";

    private enum SortMode {
        NEAREST,
        TOP_RATED
    }

    private final ArrayList<ParkingItem> allParking = new ArrayList<>();

    private ParkingAdapter adapter;
    private TextView countText;
    private TextView emptyText;
    private TextView filterNearest;
    private TextView filterTopRated;
    private TextView filterOpenNow;

    private SortMode sortMode = SortMode.NEAREST;
    private boolean openNowOnly;
    private double userLatitude;
    private double userLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_list);

        readIntentData();
        bindViews();
        initializeList();
        initializeActions();
        applyFilters();
    }

    @SuppressWarnings("unchecked")
    private void readIntentData() {
        Serializable data = getIntent().getSerializableExtra(EXTRA_PARKING_ITEMS);
        if (data instanceof ArrayList<?>) {
            for (Object item : (ArrayList<?>) data) {
                if (item instanceof ParkingItem) {
                    allParking.add((ParkingItem) item);
                }
            }
        }

        userLatitude = getIntent().getDoubleExtra(EXTRA_USER_LAT, 0);
        userLongitude = getIntent().getDoubleExtra(EXTRA_USER_LNG, 0);
    }

    private void bindViews() {
        countText = findViewById(R.id.parkingListCount);
        emptyText = findViewById(R.id.parkingEmptyText);
        filterNearest = findViewById(R.id.filterNearest);
        filterTopRated = findViewById(R.id.filterTopRated);
        filterOpenNow = findViewById(R.id.filterOpenNow);
    }

    private void initializeList() {
        RecyclerView recyclerView = findViewById(R.id.parkingRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ParkingAdapter(this::openParkingDetails);
        recyclerView.setAdapter(adapter);
    }

    private void initializeActions() {
        View.OnClickListener closeListener = view -> finish();
        findViewById(R.id.parkingListBack).setOnClickListener(closeListener);
        findViewById(R.id.parkingListClose).setOnClickListener(closeListener);
        findViewById(R.id.parkingMapView).setOnClickListener(closeListener);
        findViewById(R.id.parkingListHome).setOnClickListener(closeListener);

        filterNearest.setOnClickListener(view -> {
            sortMode = SortMode.NEAREST;
            updateFilterStyles();
            applyFilters();
        });

        filterTopRated.setOnClickListener(view -> {
            sortMode = SortMode.TOP_RATED;
            updateFilterStyles();
            applyFilters();
        });

        filterOpenNow.setOnClickListener(view -> {
            openNowOnly = !openNowOnly;
            updateFilterStyles();
            applyFilters();
        });

        updateFilterStyles();
    }

    private void applyFilters() {
        List<ParkingItem> visibleItems = new ArrayList<>();

        for (ParkingItem item : allParking) {
            if (!openNowOnly || Boolean.TRUE.equals(item.getOpenNow())) {
                visibleItems.add(item);
            }
        }

        if (sortMode == SortMode.TOP_RATED) {
            visibleItems.sort(
                    Comparator.comparingDouble(ParkingItem::getRating).reversed()
                            .thenComparingInt(ParkingItem::getDistanceMeters)
            );
        } else {
            visibleItems.sort(Comparator.comparingInt(ParkingItem::getDistanceMeters));
        }

        adapter.submitList(visibleItems);
        countText.setText(visibleItems.size() + " parking locations found");
        emptyText.setVisibility(visibleItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateFilterStyles() {
        setChipSelected(filterNearest, sortMode == SortMode.NEAREST);
        setChipSelected(filterTopRated, sortMode == SortMode.TOP_RATED);
        setChipSelected(filterOpenNow, openNowOnly);
    }

    private void setChipSelected(TextView chip, boolean selected) {
        chip.setBackgroundResource(
                selected ? R.drawable.bg_chip_red : R.drawable.bg_chip_white
        );
        chip.setTextColor(selected ? Color.WHITE : Color.parseColor("#222222"));
    }

    private void openParkingDetails(ParkingItem parking) {
        Intent intent = new Intent(this, ParkingDetailsActivity.class);
        intent.putExtra(ParkingDetailsActivity.EXTRA_PARKING_ITEM, parking);
        intent.putExtra(ParkingDetailsActivity.EXTRA_USER_LAT, userLatitude);
        intent.putExtra(ParkingDetailsActivity.EXTRA_USER_LNG, userLongitude);
        startActivity(intent);
    }
}
