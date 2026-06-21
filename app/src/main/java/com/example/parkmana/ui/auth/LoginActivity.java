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
import com.example.parkmana.databinding.ActivityLoginBinding;
import com.example.parkmana.ui.home.HomeActivity;
import com.google.android.material.textfield.TextInputEditText;

/**
 * LoginActivity — the View in MVVM. It observes AuthViewModel and reacts;
 * it never calls Firebase directly.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupCreateAccountLink();
        observeViewModel();
        setupClicks();
    }

    private void setupClicks() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvForgotPassword.setOnClickListener(v -> {
            String email = text(binding.emailInput);
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailLayout.setError("Enter your email first to reset the password");
                return;
            }
            binding.emailLayout.setError(null);
            viewModel.forgotPassword(email);
        });

        // Google Sign-In is wired in a later (optional) step.
        binding.btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Google Sign-In coming soon", Toast.LENGTH_SHORT).show());
    }

    private void attemptLogin() {
        String email = text(binding.emailInput);
        String password = text(binding.passwordInput);

        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);

        boolean valid = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError("Enter a valid email address");
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError("Enter your password");
            valid = false;
        }
        if (!valid) return;

        viewModel.login(email, password);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!isLoading);
        });

        viewModel.getLoginSuccess().observe(this, user -> {
            if (user != null) {
                Intent intent = new Intent(this, HomeActivity.class);
                // Make Home the new root and wipe the auth screens from the stack.
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

        viewModel.getInfoMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                viewModel.clearInfo();
            }
        });
    }

    /** Builds "New to ParkMana? Create account" with the link part in orange. */
    private void setupCreateAccountLink() {
        String full = "New to ParkMana? Create account";
        SpannableString span = new SpannableString(full);
        int start = full.indexOf("Create account");
        span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.park_orange)),
                start, full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new StyleSpan(Typeface.BOLD),
                start, full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.tvCreateAccount.setText(span);

        binding.tvCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    /** Reads trimmed text from a Material text field. */
    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}