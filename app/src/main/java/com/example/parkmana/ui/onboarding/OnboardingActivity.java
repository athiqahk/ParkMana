package com.example.parkmana.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.parkmana.R;
import com.example.parkmana.databinding.ActivityOnboardingBinding;
import com.example.parkmana.ui.auth.LoginActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * OnboardingActivity — the 3-page intro shown to users who aren't signed in.
 *
 * Swipes between pages with a ViewPager2, draws its own dot indicators, and
 * sends the user to Login when they tap Skip or finish the last page.
 */
public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;
    private View[] indicators;
    private List<OnboardingItem> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        buildPages();
        setupViewPager();
        setupIndicators();
        setupClicks();
    }

    /** Defines the content of the three onboarding pages. */
    private void buildPages() {
        items = new ArrayList<>();
        items.add(new OnboardingItem(
                R.drawable.onboarding_find,
                "Step 1 of 3",
                "Find Parking Near You",
                "Locate nearby parking spaces instantly using GPS — see live availability the moment you open the app."));
        items.add(new OnboardingItem(
                R.drawable.onboarding_navigate,
                "Step 2 of 3",
                "Navigate Easily",
                "Get clear turn-by-turn directions straight to your selected parking location, the moment you pick it."));
        items.add(new OnboardingItem(
                R.drawable.onboarding_alert,
                "Step 3 of 3",
                "Stay Updated",
                "Receive parking availability alerts and updates so you never circle the block looking for a spot again."));
    }

    private void setupViewPager() {
        binding.viewPager.setAdapter(new OnboardingAdapter(items));

        // A gentle fade as pages slide — gives the "smooth" feel.
        binding.viewPager.setPageTransformer((page, position) ->
                page.setAlpha(0.25f + (1 - Math.abs(position)) * 0.75f));

        // Keep the dots and the button label in sync with the current page.
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setCurrentIndicator(position);
                boolean lastPage = (position == items.size() - 1);
                binding.btnNext.setText(lastPage ? "Get Started" : "Next");
            }
        });
    }

    /** Creates one dot per page inside the indicator container. */
    private void setupIndicators() {
        indicators = new View[items.size()];
        for (int i = 0; i < indicators.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(dp(8), dp(8));
            params.setMargins(0, 0, dp(6), 0);   // gap between dots
            dot.setLayoutParams(params);
            binding.indicatorContainer.addView(dot);
            indicators[i] = dot;
        }
        setCurrentIndicator(0);
    }

    /** Highlights the dot for the given position and shrinks the rest. */
    private void setCurrentIndicator(int position) {
        for (int i = 0; i < indicators.length; i++) {
            View dot = indicators[i];
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) dot.getLayoutParams();
            if (i == position) {
                params.width = dp(24);
                dot.setBackgroundResource(R.drawable.bg_indicator_active);
            } else {
                params.width = dp(8);
                dot.setBackgroundResource(R.drawable.bg_indicator_inactive);
            }
            dot.setLayoutParams(params);
        }
    }

    private void setupClicks() {
        // Skip always jumps straight to Login.
        binding.tvSkip.setOnClickListener(v -> goToLogin());

        // Next advances one page, or finishes onboarding on the last page.
        binding.btnNext.setOnClickListener(v -> {
            int current = binding.viewPager.getCurrentItem();
            if (current < items.size() - 1) {
                binding.viewPager.setCurrentItem(current + 1);   // smooth-scrolls
            } else {
                goToLogin();
            }
        });
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();   // don't let Back return to onboarding
    }

    /** Converts dp into pixels for views we create in code. */
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}