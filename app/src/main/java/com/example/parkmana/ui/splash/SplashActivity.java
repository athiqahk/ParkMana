package com.example.parkmana.ui.splash;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.parkmana.databinding.ActivitySplashBinding;
import com.example.parkmana.ui.home.HomeActivity;
import com.example.parkmana.ui.onboarding.OnboardingActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * SplashActivity — the app's launcher screen.
 *
 * Shows the ParkMana logo with a short "loading" animation for ~2 seconds,
 * then decides where to send the user:
 *   - already signed in  -> HomeActivity
 *   - not signed in       -> OnboardingActivity
 */
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    private static final long SPLASH_DELAY_MS = 2000L;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        animateDots();
        scheduleNavigation();
    }

    private void animateDots() {
        View[] dots = { binding.dot1, binding.dot2, binding.dot3 };
        for (int i = 0; i < dots.length; i++) {
            ObjectAnimator animator =
                    ObjectAnimator.ofFloat(dots[i], View.ALPHA, 1f, 0.3f, 1f);
            animator.setDuration(900);
            animator.setStartDelay(i * 200L);
            animator.setRepeatCount(ObjectAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.start();
        }
    }

    private void scheduleNavigation() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> destination = (auth.getCurrentUser() != null)
                    ? HomeActivity.class
                    : OnboardingActivity.class;
            startActivity(new Intent(this, destination));
            finish();
        }, SPLASH_DELAY_MS);
    }
}