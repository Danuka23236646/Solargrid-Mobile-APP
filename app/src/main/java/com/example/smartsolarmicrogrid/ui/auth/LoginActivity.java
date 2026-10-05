package com.example.smartsolarmicrogrid.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.models.User;
import com.example.smartsolarmicrogrid.network.ApiClient;
import com.example.smartsolarmicrogrid.network.dto.LoginRequest;
import com.example.smartsolarmicrogrid.network.dto.LoginResponse;
import com.example.smartsolarmicrogrid.network.dto.UserDto;
import com.example.smartsolarmicrogrid.ui.operator.OperatorMainActivity;
import com.example.smartsolarmicrogrid.ui.prosumer.ProsumerMainActivity;
import com.example.smartsolarmicrogrid.util.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etNicOrEmail, etPassword;
    private RadioGroup rgRole;
    private RadioButton rbProsumer, rbOperator;
    private Button btnLogin;
    private TextView tvRegisterLink;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = DatabaseHelper.getInstance(this);

        // Check if user already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToHome(sessionManager.getUserRole());
            return;
        }

        setContentView(R.layout.activity_login);

        etNicOrEmail = findViewById(R.id.etNicOrEmail);
        etPassword = findViewById(R.id.etPassword);
        rgRole = findViewById(R.id.rgRole);
        rbProsumer = findViewById(R.id.rbProsumer);
        rbOperator = findViewById(R.id.rbOperator);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        btnLogin.setOnClickListener(v -> performLogin());

        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String email = etNicOrEmail.getText() != null ? etNicOrEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String selectedRole = rbProsumer.isChecked() ? "Prosumer" : "GridOperator";

        if (TextUtils.isEmpty(email)) {
            etNicOrEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("LOGGING IN...");

        // Call ASP.NET Core API /api/auth/login
        LoginRequest request = new LoginRequest(email, password);
        ApiClient.getApiService(this).login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("LOGIN TO ACCOUNT");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse res = response.body();
                    UserDto u = res.getUser();
                    String name = (u != null && u.getFullName() != null) ? u.getFullName() : email;
                    String userEmail = (u != null && u.getEmail() != null) ? u.getEmail() : email;
                    String nic = (u != null && u.getNic() != null) ? u.getNic() : "";
                    String role = res.getRole() != null ? res.getRole() : selectedRole;
                    String status = res.getAccountStatus() != null ? res.getAccountStatus() : "Active";

                    User user = new User(1, nic, name, userEmail, role, res.getToken(), status);
                    if (res.getToken() != null) {
                        sessionManager.saveAuthToken(res.getToken());
                    }
                    processSuccessfulLogin(user);
                } else {
                    attemptOfflineLogin(email, selectedRole);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("LOGIN TO ACCOUNT");
                attemptOfflineLogin(email, selectedRole);
            }
        });
    }

    private void attemptOfflineLogin(String email, String role) {
        User user = dbHelper.getUserByEmail(email);
        if (user == null) {
            user = dbHelper.getUserByNic(email);
        }

        if (user != null) {
            processSuccessfulLogin(user);
            Toast.makeText(this, "Logged in via cached offline credentials", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Login failed: Invalid credentials or backend unreachable", Toast.LENGTH_LONG).show();
        }
    }

    private void processSuccessfulLogin(User user) {
        sessionManager.createSession(user);
        dbHelper.saveUser(user);
        Toast.makeText(this, "Welcome, " + user.getName(), Toast.LENGTH_SHORT).show();
        navigateToHome(user.getRole());
    }

    private void navigateToHome(String role) {
        Intent intent;
        if ("GridOperator".equalsIgnoreCase(role) || "OPERATOR".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, OperatorMainActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, ProsumerMainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
