package com.example.parkmana.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import android.widget.Toast;

import com.example.parkmana.R;
import com.example.parkmana.ui.favourites.FavouritesActivity;
import com.example.parkmana.ui.home.HomeActivity;
import com.example.parkmana.ui.profile.ProfileActivity;

/**
 * Wires the shared bottom navigation (layout_bottom_nav.xml).
 * Call once in onCreate after setContentView:
 *   BottomNavHelper.setup(this, BottomNavHelper.TAB_HOME);
 */
public final class BottomNavHelper {

    public static final String TAB_HOME = "home";
    public static final String TAB_SAVED = "saved";
    public static final String TAB_ALERTS = "alerts";
    public static final String TAB_PROFILE = "profile";

    private static final int COLOR_ACTIVE = Color.parseColor("#FF4B3E");
    private static final int COLOR_INACTIVE = Color.parseColor("#505A66");

    private BottomNavHelper() {
    }

    public static void setup(Activity activity, String currentTab) {
        TextView home = activity.findViewById(R.id.menuHome);
        TextView saved = activity.findViewById(R.id.menuSaved);
        TextView alerts = activity.findViewById(R.id.menuAlerts);
        TextView profile = activity.findViewById(R.id.menuProfile);

        if (home == null) return; // footer not present in this layout

        highlight(home, TAB_HOME.equals(currentTab));
        highlight(saved, TAB_SAVED.equals(currentTab));
        highlight(alerts, TAB_ALERTS.equals(currentTab));
        highlight(profile, TAB_PROFILE.equals(currentTab));

        home.setOnClickListener(view -> {
            if (!TAB_HOME.equals(currentTab)) {
                Intent intent = new Intent(activity, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
            }
        });

        saved.setOnClickListener(view -> {
            if (!TAB_SAVED.equals(currentTab)) {
                activity.startActivity(
                        new Intent(activity, FavouritesActivity.class));
            }
        });

        alerts.setOnClickListener(view ->
                Toast.makeText(activity, "Alerts coming soon!",
                        Toast.LENGTH_SHORT).show());

        profile.setOnClickListener(view -> {
            if (!TAB_PROFILE.equals(currentTab)) {
                activity.startActivity(
                        new Intent(activity, ProfileActivity.class));
            }
        });
    }

    private static void highlight(TextView tab, boolean active) {
        int color = active ? COLOR_ACTIVE : COLOR_INACTIVE;
        tab.setTextColor(color);
        tab.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);

        // Tint any icon drawables (e.g. the Home house icon) to match
        for (Drawable drawable : tab.getCompoundDrawables()) {
            if (drawable != null) {
                drawable.mutate().setTint(color);
            }
        }
    }
}