package com.example.parkmana.ui.onboarding;

/** Holds the content for a single onboarding page. */
public class OnboardingItem {

    private final int imageRes;
    private final String step;
    private final String title;
    private final String description;

    public OnboardingItem(int imageRes, String step, String title, String description) {
        this.imageRes = imageRes;
        this.step = step;
        this.title = title;
        this.description = description;
    }

    public int getImageRes() { return imageRes; }
    public String getStep() { return step; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}