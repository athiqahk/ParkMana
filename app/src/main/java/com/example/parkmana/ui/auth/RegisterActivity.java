package com.example.parkmana.ui.auth;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.parkmana.R;
import com.example.parkmana.databinding.ActivityRegisterBinding;
import com.example.parkmana.ui.home.HomeActivity;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupTermsText();
        setupLoginLink();
        observeViewModel();
        setupClicks();
    }

    private void setupClicks() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String name = text(binding.nameInput);
        String email = text(binding.emailInput);
        String password = text(binding.passwordInput);
        String confirm = text(binding.confirmInput);

        // Clear old errors.
        binding.nameLayout.setError(null);
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.confirmLayout.setError(null);

        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError("Enter your name");
            valid = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError("Enter a valid email address");
            valid = false;
        }
        if (password.length() < 6) {
            binding.passwordLayout.setError("Password must be at least 6 characters");
            valid = false;
        }
        if (!confirm.equals(password)) {
            binding.confirmLayout.setError("Passwords don't match");
            valid = false;
        }
        if (!binding.checkboxTerms.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms and Privacy Policy",
                    Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (!valid) return;

        viewModel.register(name, email, password);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!isLoading);
        });

        viewModel.getRegisterSuccess().observe(this, user -> {
            if (user != null) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }

    private void setupTermsText() {
        String full = "I agree to ParkMana's Terms of Service and Privacy Policy.";
        SpannableString span = new SpannableString(full);
        highlight(span, full, "Terms of Service");
        highlight(span, full, "Privacy Policy");
        binding.tvTerms.setText(span);
    }

    private void setupLoginLink() {
        String full = "Already have an account? Log in";
        SpannableString span = new SpannableString(full);
        highlight(span, full, "Log in");
        binding.tvLogin.setText(span);
        binding.tvLogin.setOnClickListener(v -> finish());  // returns to Login
    }

    /** Colours a substring orange + bold inside the spannable text. */
    private void highlight(SpannableString span, String full, String part) {
        int start = full.indexOf(part);
        if (start < 0) return;
        int end = start + part.length();
        span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.park_orange)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}