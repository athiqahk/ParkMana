package com.example.parkmana.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.parkmana.databinding.ActivityForgotPasswordBinding;
import com.google.android.material.textfield.TextInputEditText;

/**
 * ForgotPasswordActivity — lets a user request a password-reset email.
 *
 * Reuses AuthViewModel.forgotPassword(), which sends the reset email via
 * Firebase. On success we tell the user to check their inbox and close.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        observeViewModel();
        setupClicks();
    }

    private void setupClicks() {
        binding.btnBack.setOnClickListener(v -> finish());   // back to Login

        binding.btnSend.setOnClickListener(v -> {
            String email = text(binding.emailInput);
            binding.emailLayout.setError(null);

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailLayout.setError("Enter a valid email address");
                return;
            }
            viewModel.forgotPassword(email);
        });
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSend.setEnabled(!isLoading);
        });

        // Success: Firebase sends "infoMessage" -> tell user and close the screen.
        viewModel.getInfoMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                viewModel.clearInfo();
                finish();   // return to Login after sending
            }
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}