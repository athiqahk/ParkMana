package com.example.parkmana.ui.navigation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;

import android.content.Intent;
import android.net.Uri;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.parkmana.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;


public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;

    private LatLng userLocation;
    private LatLng parkingLocation;

    private String parkingName;

    private TextView topParkingName;
    private TextView parkingNameText;
    private TextView distanceText;
    private TextView timeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        topParkingName = findViewById(R.id.topParkingName);
        parkingNameText = findViewById(R.id.parkingNameText);
        distanceText = findViewById(R.id.distanceText);
        timeText = findViewById(R.id.timeText);

        Button startNavigationButton =
                findViewById(R.id.startNavigationButton);

        double userLat = getIntent().getDoubleExtra("user_lat", 0);
        double userLng = getIntent().getDoubleExtra("user_lng", 0);

        double parkingLat = getIntent().getDoubleExtra("parking_lat", 0);
        double parkingLng = getIntent().getDoubleExtra("parking_lng", 0);

        parkingName = getIntent().getStringExtra("parking_name");

        userLocation = new LatLng(userLat, userLng);
        parkingLocation = new LatLng(parkingLat, parkingLng);

        topParkingName.setText(parkingName);
        parkingNameText.setText(parkingName);

        startNavigationButton.setOnClickListener(v -> {

            Uri gmmIntentUri = Uri.parse(
                    "google.navigation:q="
                            + parkingLocation.latitude + ","
                            + parkingLocation.longitude);

            Intent mapIntent = new Intent(
                    Intent.ACTION_VIEW,
                    gmmIntentUri);

            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            }

        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.navigationMap);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        googleMap = map;

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            googleMap.setMyLocationEnabled(true);
        }

        googleMap.addMarker(
                new MarkerOptions()
                        .position(userLocation)
                        .title("You")
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE))
        );

        googleMap.addMarker(
                new MarkerOptions()
                        .position(parkingLocation)
                        .title(parkingName)
        );

        googleMap.addPolyline(
                new PolylineOptions()
                        .add(userLocation)
                        .add(parkingLocation)
                        .width(12)
        );

        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(userLocation, 15));

        float[] result = new float[1];

        Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                parkingLocation.latitude,
                parkingLocation.longitude,
                result
        );

        int meter = (int) result[0];

        distanceText.setText(meter + " m away");

        int minute = (int) Math.ceil(meter / 80.0);

        timeText.setText(minute + " min");
    }
}