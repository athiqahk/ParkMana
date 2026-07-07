package com.example.parkmana.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.parkmana.BuildConfig;
import com.example.parkmana.R;
import com.example.parkmana.ui.BottomNavHelper;
import com.example.parkmana.ui.navigation.NavigationActivity;
import com.example.parkmana.ui.parking.ParkingDetailsActivity;
import com.example.parkmana.ui.parking.ParkingItem;
import com.example.parkmana.ui.parking.ParkingListActivity;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PAGE 1:
 * Shows user's current location and searches nearby parking using Google Places API.
 */
public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_CODE = 100;

    private static final String GOOGLE_API_KEY = BuildConfig.MAPS_API_KEY;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    private PlacesClient placesClient;

    private AutoCompleteTextView searchBar;

    private List<AutocompletePrediction> predictionList = new ArrayList<>();

    private TextView parkingCountText;
    private TextView parkingNameText;
    private TextView parkingInfoText;
    private ImageView parkingImageView;
    private TextView parkingImagePlaceholder;
    private Button btnNavigate;
    private final ArrayList<ParkingItem> parkingItems = new ArrayList<>();

    /** Google photo reference per parking name, filled by nearby search. */
    private final Map<String, String> photoReferences = new HashMap<>();
    /** Rating per parking name, filled by nearby search. */
    private final Map<String, Double> parkingRatings = new HashMap<>();

    private LatLng userLocation;
    private LatLng selectedParkingLocation;
    private String selectedParkingName = "Nearby Parking";
    private ParkingItem selectedParkingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //========================
        // Views
        //========================
        searchBar = findViewById(R.id.searchBar);

        parkingCountText = findViewById(R.id.parkingCountText);
        parkingNameText = findViewById(R.id.parkingNameText);
        parkingInfoText = findViewById(R.id.parkingInfoText);
        parkingImageView = findViewById(R.id.parkingImageView);
        parkingImagePlaceholder = findViewById(R.id.parkingImagePlaceholder);

        btnNavigate = findViewById(R.id.btnNavigate);

        btnNavigate.setEnabled(false);
        btnNavigate.setVisibility(View.INVISIBLE);

        //========================
        // Places SDK
        //========================
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), GOOGLE_API_KEY);
        }

        placesClient = Places.createClient(this);

        //========================
        // GPS
        //========================
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        //========================
        // Google Map
        //========================
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.homeMap);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        //========================
        // Search
        //========================
        initializeSearch();

        findViewById(R.id.seeAllParking).setOnClickListener(v ->
                openParkingList());


        findViewById(R.id.btnLocateMe).setOnClickListener(v ->
                recenterOnCurrentLocation());

        findViewById(R.id.parkingCard).setOnClickListener(v -> openSelectedParkingDetails());

        //========================
        // Bottom menu (shared footer)
        //========================
        BottomNavHelper.setup(this, BottomNavHelper.TAB_HOME);

        //========================
        // Navigate Button
        //========================
        btnNavigate.setOnClickListener(v -> {

            if (selectedParkingLocation == null) {
                Toast.makeText(
                        HomeActivity.this,
                        "Please select a parking location.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent intent = new Intent(HomeActivity.this, NavigationActivity.class);
            intent.putExtra("user_lat", userLocation.latitude);
            intent.putExtra("user_lng", userLocation.longitude);
            intent.putExtra("parking_lat", selectedParkingLocation.latitude);
            intent.putExtra("parking_lng", selectedParkingLocation.longitude);
            intent.putExtra("parking_name", selectedParkingName);
            startActivity(intent);
        });
    }

    private void initializeSearch() {

        searchBar.addTextChangedListener(new android.text.TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() < 2)
                    return;

                FindAutocompletePredictionsRequest request =
                        FindAutocompletePredictionsRequest.builder()
                                .setQuery(s.toString())
                                .build();

                placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener(response -> {

                            predictionList.clear();

                            ArrayList<String> suggestions = new ArrayList<>();

                            for (AutocompletePrediction prediction :
                                    response.getAutocompletePredictions()) {
                                predictionList.add(prediction);
                                suggestions.add(prediction.getFullText(null).toString());
                            }

                            ArrayAdapter<String> adapter =
                                    new ArrayAdapter<>(
                                            HomeActivity.this,
                                            android.R.layout.simple_dropdown_item_1line,
                                            suggestions);

                            searchBar.setAdapter(adapter);
                            searchBar.showDropDown();
                        });
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        searchBar.setOnItemClickListener((parent, view, position, id) -> {

            AutocompletePrediction prediction = predictionList.get(position);

            FetchPlaceRequest request =
                    FetchPlaceRequest.newInstance(
                            prediction.getPlaceId(),
                            Arrays.asList(
                                    Place.Field.NAME,
                                    Place.Field.LAT_LNG
                            ));

            placesClient.fetchPlace(request)
                    .addOnSuccessListener(response -> {

                        Place place = response.getPlace();

                        if (place.getLatLng() == null)
                            return;

                        googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                        place.getLatLng(), 16f));

                        // Search nearby parking around the searched place
                        findNearbyParking(place.getLatLng());
                    });
        });
    }

    private void openSelectedParkingDetails() {
        if (selectedParkingItem == null || userLocation == null) {
            Toast.makeText(this, "Tap a parking pin first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ParkingDetailsActivity.class);
        intent.putExtra(ParkingDetailsActivity.EXTRA_PARKING_ITEM, selectedParkingItem);
        intent.putExtra(ParkingDetailsActivity.EXTRA_USER_LAT, userLocation.latitude);
        intent.putExtra(ParkingDetailsActivity.EXTRA_USER_LNG, userLocation.longitude);
        startActivity(intent);
    }

    private void openParkingList() {
        if (parkingItems.isEmpty() || userLocation == null) {
            Toast.makeText(this,
                    "Parking locations are still loading.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ParkingListActivity.class);
        intent.putExtra(
                ParkingListActivity.EXTRA_PARKING_ITEMS,
                new ArrayList<>(parkingItems));
        intent.putExtra(ParkingListActivity.EXTRA_USER_LAT, userLocation.latitude);
        intent.putExtra(ParkingListActivity.EXTRA_USER_LNG, userLocation.longitude);
        startActivity(intent);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        googleMap.setOnMarkerClickListener(marker -> {

            if ("You".equals(marker.getTitle()))
                return false;

            selectedParkingName = marker.getTitle();
            selectedParkingLocation = marker.getPosition();
            selectedParkingItem = findParkingItemByName(selectedParkingName);

            parkingNameText.setText(selectedParkingName);
            parkingInfoText.setText(buildInfoLine(
                    marker.getSnippet(), parkingRatings.get(selectedParkingName)));

            loadParkingPhoto(selectedParkingName);

            btnNavigate.setEnabled(true);
            btnNavigate.setVisibility(View.VISIBLE);

            return false;
        });

        enableUserLocation();
    }

    private void enableUserLocation() {

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
            return;
        }

        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if (location == null)
                        return;

                    userLocation = new LatLng(
                            location.getLatitude(),
                            location.getLongitude());

                    googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(userLocation, 16f));

                    findNearbyParking(userLocation);
                });
    }

    private void recenterOnCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {

                    if (location != null) {
                        userLocation = new LatLng(
                                location.getLatitude(),
                                location.getLongitude());

                        googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(userLocation, 16f));
                    } else if (userLocation != null) {
                        googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(userLocation, 16f));
                    } else {
                        Toast.makeText(this,
                                "Current location is not available yet.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(error ->
                        Toast.makeText(this,
                                "Unable to get current location.",
                                Toast.LENGTH_SHORT).show());
    }

    private void findNearbyParking(LatLng location) {

        parkingCountText.setText("Searching parking...");
        parkingNameText.setText("Please wait...");
        parkingInfoText.setText("");
        selectedParkingLocation = null;
        selectedParkingItem = null;
        btnNavigate.setEnabled(false);
        btnNavigate.setVisibility(View.INVISIBLE);
        parkingItems.clear();
        photoReferences.clear();
        parkingRatings.clear();

        new Thread(() -> {

            try {

                String urlString =
                        "https://maps.googleapis.com/maps/api/place/nearbysearch/json?"
                                + "location=" + location.latitude + "," + location.longitude
                                + "&radius=2000"
                                + "&type=parking"
                                + "&key=" + GOOGLE_API_KEY;

                URL url = new URL(urlString);

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();
                connection.connect();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));

                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
                reader.close();

                JSONObject object = new JSONObject(json.toString());

                String status = object.getString("status");

                if (!status.equals("OK")) {
                    runOnUiThread(() -> {
                        parkingCountText.setText("No parking found");
                        parkingNameText.setText("-");
                        parkingInfoText.setText(status);
                        btnNavigate.setEnabled(false);
                        btnNavigate.setVisibility(View.INVISIBLE);
                    });
                    return;
                }

                JSONArray results = object.getJSONArray("results");
                ArrayList<ParkingItem> foundParkingItems = new ArrayList<>();

                runOnUiThread(() -> googleMap.clear());

                // User marker
                runOnUiThread(() -> googleMap.addMarker(
                        new MarkerOptions()
                                .position(location)
                                .title("You")
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_AZURE))));

                // Add all parking markers
                for (int i = 0; i < results.length(); i++) {

                    JSONObject parking = results.getJSONObject(i);

                    String name = parking.getString("name");
                    String placeId = parking.optString("place_id", null);

                    JSONObject loc = parking.getJSONObject("geometry")
                            .getJSONObject("location");

                    double lat = loc.getDouble("lat");
                    double lng = loc.getDouble("lng");

                    LatLng parkingLocation = new LatLng(lat, lng);

                    float[] distance = new float[1];
                    Location.distanceBetween(
                            location.latitude, location.longitude,
                            lat, lng, distance);

                    int meter = (int) distance[0];

                    double rating = parking.optDouble("rating", -1d);
                    int ratingCount = parking.optInt("user_ratings_total", 0);
                    String address = parking.optString("vicinity", "");

                    Boolean openNow = null;
                    JSONObject openingHours = parking.optJSONObject("opening_hours");
                    if (openingHours != null && openingHours.has("open_now")) {
                        openNow = openingHours.optBoolean("open_now");
                    }

                    // Photo reference from the Places API (if the place has photos)
                    String photoReference = null;
                    JSONArray photos = parking.optJSONArray("photos");
                    if (photos != null && photos.length() > 0) {
                        photoReference = photos.getJSONObject(0)
                                .optString("photo_reference", null);
                    }
                    if (photoReference != null) {
                        photoReferences.put(name, photoReference);
                    }
                    if (rating >= 0) {
                        parkingRatings.put(name, rating);
                    }

                    ParkingItem item = new ParkingItem(
                            name, address, lat, lng, meter,
                            rating, ratingCount, openNow);
                    item.setPhotoReference(photoReference);
                    item.setPlaceId(placeId);
                    foundParkingItems.add(item);

                    runOnUiThread(() -> googleMap.addMarker(
                            new MarkerOptions()
                                    .position(parkingLocation)
                                    .title(name)
                                    .snippet(meter + " m away")));
                }

                runOnUiThread(() -> {
                    parkingItems.clear();
                    parkingItems.addAll(foundParkingItems);
                });

                int parkingCount = results.length();

                // No auto-selection: show a hint until the user taps a marker
                runOnUiThread(() -> {
                    parkingCountText.setText(
                            parkingCount + " parking locations found");
                    parkingNameText.setText("Tap a parking pin to view details");
                    parkingInfoText.setText("");
                    parkingImageView.setVisibility(View.GONE);
                    parkingImagePlaceholder.setVisibility(View.VISIBLE);
                    btnNavigate.setEnabled(false);
                    btnNavigate.setVisibility(View.INVISIBLE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this,
                                e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }

        }).start();
    }

    /** Finds the ParkingItem matching a marker's title, for opening full details. */
    private ParkingItem findParkingItemByName(String name) {
        if (name == null) return null;
        for (ParkingItem item : parkingItems) {
            if (name.equals(item.getName())) return item;
        }
        return null;
    }

    /** Combines distance and rating into one line, e.g. "230 m away  ·  ★ 4.5". */
    private String buildInfoLine(String distanceText, Double rating) {
        if (distanceText == null) distanceText = "";
        if (rating == null || rating < 0) return distanceText;
        return distanceText + "  ·  ★ "
                + String.format(Locale.getDefault(), "%.1f", rating);
    }

    /** Loads the parking's Google photo into the card, or shows the P badge. */
    private void loadParkingPhoto(String parkingName) {
        String reference = photoReferences.get(parkingName);

        if (reference == null) {
            // No photo on Google for this place — show the P placeholder
            parkingImageView.setVisibility(View.GONE);
            parkingImagePlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        parkingImagePlaceholder.setVisibility(View.GONE);
        parkingImageView.setVisibility(View.VISIBLE);

        String photoUrl =
                "https://maps.googleapis.com/maps/api/place/photo?"
                        + "maxwidth=200"
                        + "&photo_reference=" + reference
                        + "&key=" + GOOGLE_API_KEY;

        Glide.with(this)
                .load(photoUrl)
                .centerCrop()
                .into(parkingImageView);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        }
    }
}