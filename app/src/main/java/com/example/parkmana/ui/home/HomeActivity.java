package com.example.parkmana.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.parkmana.R;
import com.example.parkmana.ui.navigation.NavigationActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

/**
 * PAGE 1:
 * Shows user's current location and searches nearby parking using Google Places API.
 */
public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_CODE = 100;

    private static final String GOOGLE_API_KEY = "AIzaSyAk5ER-8KcF8YS7tV7PgZG9Q6dp7nDk1JA";

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    private TextView parkingCountText;
    private TextView parkingNameText;
    private TextView parkingInfoText;
    private Button btnNavigate;

    private LatLng userLocation;
    private LatLng selectedParkingLocation;
    private String selectedParkingName = "Nearby Parking";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        parkingCountText = findViewById(R.id.parkingCountText);
        parkingNameText = findViewById(R.id.parkingNameText);
        parkingInfoText = findViewById(R.id.parkingInfoText);
        btnNavigate = findViewById(R.id.btnNavigate);

        btnNavigate.setEnabled(false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.homeMap);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnNavigate.setOnClickListener(v -> {
            if (userLocation == null || selectedParkingLocation == null) {
                Toast.makeText(this, "Parking location not ready yet", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        enableUserLocation();
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE
            );
            return;
        }

        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "Please turn on GPS and reopen the app", Toast.LENGTH_LONG).show();
                return;
            }

            userLocation = new LatLng(location.getLatitude(), location.getLongitude());

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16f));

            findNearbyParking(userLocation);
        });
    }

    private void findNearbyParking(LatLng location) {
        parkingCountText.setText("Searching parking near you...");
        parkingNameText.setText("Please wait...");
        parkingInfoText.setText("Finding nearest parking...");

        new Thread(() -> {
            try {
                String urlString =
                        "https://maps.googleapis.com/maps/api/place/nearbysearch/json?"
                                + "location=" + location.latitude + "," + location.longitude
                                + "&radius=2000"
                                + "&type=parking"
                                + "&key=" + GOOGLE_API_KEY;

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(connection.getInputStream()));

                StringBuilder json = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }

                reader.close();

                JSONObject jsonObject = new JSONObject(json.toString());
                String status = jsonObject.getString("status");

                if (!status.equals("OK")) {
                    runOnUiThread(() -> {
                        parkingCountText.setText("No parking found nearby");
                        parkingNameText.setText("Try another location");
                        parkingInfoText.setText("Places API status: " + status);
                        Toast.makeText(this, "No parking found: " + status, Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                JSONArray results = jsonObject.getJSONArray("results");

                JSONObject firstParking = results.getJSONObject(0);

                selectedParkingName = firstParking.getString("name");

                JSONObject geometry = firstParking.getJSONObject("geometry");
                JSONObject parkingLoc = geometry.getJSONObject("location");

                double parkingLat = parkingLoc.getDouble("lat");
                double parkingLng = parkingLoc.getDouble("lng");

                selectedParkingLocation = new LatLng(parkingLat, parkingLng);

                float[] distance = new float[1];

                Location.distanceBetween(
                        location.latitude,
                        location.longitude,
                        parkingLat,
                        parkingLng,
                        distance
                );

                int distanceMeter = Math.round(distance[0]);
                int parkingCount = results.length();

                runOnUiThread(() -> {
                    googleMap.clear();

                    googleMap.addMarker(new MarkerOptions()
                            .position(selectedParkingLocation)
                            .title(selectedParkingName)
                            .snippet(distanceMeter + " m away")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f));

                    parkingCountText.setText(parkingCount + " parking spots found near you");
                    parkingNameText.setText(selectedParkingName);
                    parkingInfoText.setText(distanceMeter + " m away • nearby parking available");

                    btnNavigate.setEnabled(true);
                });

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() -> {
                    parkingCountText.setText("Parking search failed");
                    parkingNameText.setText("Error");
                    parkingInfoText.setText(e.getMessage());
                    Toast.makeText(this, "Parking API error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        }
    }
}