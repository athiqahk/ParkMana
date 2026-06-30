package com.example.parkmana.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.parkmana.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {

    private final AuthRepository repository = new AuthRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<FirebaseUser> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<FirebaseUser> registerSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> infoMessage = new MutableLiveData<>();

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<FirebaseUser> getLoginSuccess() { return loginSuccess; }
    public LiveData<FirebaseUser> getRegisterSuccess() { return registerSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getInfoMessage() { return infoMessage; }

    public void login(String email, String password) {
        loading.setValue(true);
        repository.login(email, password, new AuthRepository.AuthCallback() {
            @Override public void onSuccess(FirebaseUser user) {
                loading.setValue(false);
                loginSuccess.setValue(user);
            }
            @Override public void onError(String message) {
                loading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void register(String name, String email, String password) {
        loading.setValue(true);
        repository.register(name, email, password, new AuthRepository.AuthCallback() {
            @Override public void onSuccess(FirebaseUser user) {
                loading.setValue(false);
                registerSuccess.setValue(user);
            }
            @Override public void onError(String message) {
                loading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    /** Signs into Firebase with a Google ID token; result flows through loginSuccess. */
    public void signInWithGoogle(String idToken) {
        loading.setValue(true);
        repository.signInWithGoogle(idToken, new AuthRepository.AuthCallback() {
            @Override public void onSuccess(FirebaseUser user) {
                loading.setValue(false);
                loginSuccess.setValue(user);
            }
            @Override public void onError(String message) {
                loading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void forgotPassword(String email) {
        loading.setValue(true);
        repository.sendPasswordReset(email, new AuthRepository.AuthCallback() {
            @Override public void onSuccess(FirebaseUser user) {
                loading.setValue(false);
                infoMessage.setValue("Password reset link sent to " + email);
            }
            @Override public void onError(String message) {
                loading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void clearError() { errorMessage.setValue(null); }
    public void clearInfo() { infoMessage.setValue(null); }
}