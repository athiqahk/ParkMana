package com.example.parkmana.ui.parking;

/**
 * Lightweight view-model for a single Google review shown on the
 * Parking Details screen.
 */
public class ParkingReview {

    private final String authorName;
    private final String authorPhotoUrl;
    private final float rating;
    private final String relativeTime;
    private final String text;

    public ParkingReview(String authorName, String authorPhotoUrl, float rating,
                         String relativeTime, String text) {
        this.authorName = authorName;
        this.authorPhotoUrl = authorPhotoUrl;
        this.rating = rating;
        this.relativeTime = relativeTime;
        this.text = text;
    }

    public String getAuthorName() {
        return authorName;
    }

    public float getRating() {
        return rating;
    }

    public String getRelativeTime() {
        return relativeTime;
    }

    public String getText() {
        return text;
    }
}