package com.example.parkmana.ui.parking;

/**
 * A single community-submitted update about a parking location — visible
 * to every user (unlike the old private per-user photo), so drivers can
 * warn each other about blocked entrances, wrong hours, etc.
 */
public class ParkingReport {

    private final String id;
    private final String uploaderName;
    private final String description;
    private final String imageBase64;
    private final long createdAtMillis;

    public ParkingReport(String id, String uploaderName, String description,
                         String imageBase64, long createdAtMillis) {
        this.id = id;
        this.uploaderName = uploaderName;
        this.description = description;
        this.imageBase64 = imageBase64;
        this.createdAtMillis = createdAtMillis;
    }

    public String getId() {
        return id;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public String getDescription() {
        return description;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}