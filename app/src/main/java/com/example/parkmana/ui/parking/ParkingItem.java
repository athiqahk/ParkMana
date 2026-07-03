package com.example.parkmana.ui.parking;

import java.io.Serializable;

/**
 * Parking place model passed between activities.
 * NOTE: constructor and getters match the existing usage in HomeActivity,
 * ParkingListActivity and ParkingDetailsActivity. photoReference is new —
 * it carries the Google Places photo reference for list thumbnails.
 */
public class ParkingItem implements Serializable {

    private final String name;
    private final String address;
    private final double latitude;
    private final double longitude;
    private final int distanceMeters;
    private final double rating;
    private final int ratingCount;
    private final Boolean openNow;
    private String photoReference;

    public ParkingItem(String name,
                       String address,
                       double latitude,
                       double longitude,
                       int distanceMeters,
                       double rating,
                       int ratingCount,
                       Boolean openNow) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.openNow = openNow;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public double getRating() {
        return rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public Boolean getOpenNow() {
        return openNow;
    }

    public String getPhotoReference() {
        return photoReference;
    }

    public void setPhotoReference(String photoReference) {
        this.photoReference = photoReference;
    }
}