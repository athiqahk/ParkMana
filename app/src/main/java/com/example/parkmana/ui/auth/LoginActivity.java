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
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.lifecycle.ViewModelProvider;

import com.example.parkmana.R;
import com.example.parkmana.databinding.ActivityLoginBinding;
import com.example.parkmana.ui.home.HomeActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executors;

/**
 * LoginActivity — the View in MVVM. Observes AuthViewModel and reacts; never
 * calls Firebase directly. Google Sign-In uses Credential Manager to obtain a
 * Google ID token, which is then handed to the ViewModel.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        credentialManager = CredentialManager.create(this);

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

        binding.btnGoogle.setOnClickListener(v -> startGoogleSignIn());
    }

    // ---------- Email / password ----------

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

    // ---------- Google Sign-In (Credential Manager) ----------

    private void startGoogleSignIn() {
        // Build a request for a Google ID token, tied to our Web client ID.
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)   // show all Google accounts, not just previously used
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        binding.progressBar.setVisibility(View.VISIBLE);

        credentialManager.getCredentialAsync(
                this,
                request,
                null,                               // no cancellation signal
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        // Callback runs on a background thread — hop to the UI thread.
                        runOnUiThread(() -> handleGoogleResponse(response));
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this,
                                    "Google sign-in cancelled or failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void handleGoogleResponse(GetCredentialResponse response) {
        try {
            // The returned credential should be a Google ID token credential.
            if (response.getCredential() instanceof androidx.credentials.CustomCredential
                    && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(response.getCredential().getType())) {

                GoogleIdTokenCredential googleCredential =
                        GoogleIdTokenCredential.createFrom(response.getCredential().getData());

                String idToken = googleCredential.getIdToken();
                // Hand the token to the ViewModel, which signs into Firebase.
                viewModel.signInWithGoogle(idToken);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Unexpected credential type", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Could not read Google credential: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------- Observe ViewModel ----------

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!isLoading);
        });

        viewModel.getLoginSuccess().observe(this, user -> {
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

        viewModel.getInfoMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                viewModel.clearInfo();
            }
        });
    }

    // ---------- Helpers ----------

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

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}