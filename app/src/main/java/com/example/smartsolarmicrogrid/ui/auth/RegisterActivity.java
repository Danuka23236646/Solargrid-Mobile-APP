package com.example.smartsolarmicrogrid.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.models.User;
import com.example.smartsolarmicrogrid.network.ApiClient;
import com.example.smartsolarmicrogrid.network.dto.LoginResponse;
import com.example.smartsolarmicrogrid.network.dto.RegisterProsumerRequest;
import com.example.smartsolarmicrogrid.network.dto.UserDto;
import com.example.smartsolarmicrogrid.ui.prosumer.ProsumerMainActivity;
import com.example.smartsolarmicrogrid.util.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etRegNic, etRegName, etRegEmail, etRegPassword, etRegConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        etRegNic = findViewById(R.id.etRegNic);
        etRegName = findViewById(R.id.etRegName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnRegister.setOnClickListener(v -> performRegistration());

        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void performRegistration() {
        String nic = etRegNic.getText() != null ? etRegNic.getText().toString().trim() : "";
        String name = etRegName.getText() != null ? etRegName.getText().toString().trim() : "";
        String email = etRegEmail.getText() != null ? etRegEmail.getText().toString().trim() : "";
        String password = etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";
        String confirmPassword = etRegConfirmPassword.getText() != null ? etRegConfirmPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nic)) {
            etRegNic.setError("NIC is required (Primary Key)");
            return;
        }
        if (TextUtils.isEmpty(name)) {
            etRegName.setError("Full name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etRegEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etRegPassword.setError("Password is required");
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            etRegConfirmPassword.setError("Confirm password is required");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etRegConfirmPassword.setError("Passwords do not match");
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("REGISTERING...");

        RegisterProsumerRequest req = new RegisterProsumerRequest(
                email,
                password,
                confirmPassword,
                name,
                nic,
                "0771234567",
                "Colombo, Sri Lanka"
        );

        // Call API POST /api/auth/register/prosumer
        ApiClient.getApiService(this).registerProsumer(req).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnRegister.setEnabled(true);
                btnRegister.setText("REGISTER ACCOUNT");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse res = response.body();
                    UserDto u = res.getUser();
                    String fullName = (u != null && u.getFullName() != null) ? u.getFullName() : name;
                    String userEmail = (u != null && u.getEmail() != null) ? u.getEmail() : email;
                    String userNic = (u != null && u.getNic() != null) ? u.getNic() : nic;
                    String status = res.getAccountStatus() != null ? res.getAccountStatus() : "Active";

                    User newUser = new User(1, userNic, fullName, userEmail, "Prosumer", res.getToken(), status);
                    dbHelper.saveUser(newUser);
                    if (res.getToken() != null) {
                        sessionManager.saveAuthToken(res.getToken());
                    }
                    Toast.makeText(RegisterActivity.this, "Registered on MongoDB successfully!", Toast.LENGTH_SHORT).show();
                    completeRegistration(newUser);
                } else {
                    String parsedError = parseApiError(response);
                    Log.e("RegisterActivity", "Backend API Validation Error: " + parsedError);
                    showValidationErrorDialog(parsedError);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnRegister.setEnabled(true);
                btnRegister.setText("REGISTER ACCOUNT");

                Log.e("RegisterActivity", "Network Failure", t);
                Toast.makeText(RegisterActivity.this, "Cannot connect to Backend API: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String parseApiError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String rawJson = response.errorBody().string();
                JSONObject json = new JSONObject(rawJson);

                if (json.has("errors")) {
                    JSONObject errors = json.getJSONObject("errors");
                    StringBuilder sb = new StringBuilder();
                    Iterator<String> keys = errors.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        JSONArray arr = errors.getJSONArray(key);
                        for (int i = 0; i < arr.length(); i++) {
                            sb.append("• ").append(arr.getString(i)).append("\n");
                        }
                    }
                    if (sb.length() > 0) return sb.toString().trim();
                }

                if (json.has("title")) {
                    return json.getString("title");
                }
                if (json.has("message")) {
                    return json.getString("message");
                }
                return rawJson;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "HTTP " + response.code() + " Model Validation Error";
    }

    private void showValidationErrorDialog(String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle("Registration Failed (ASP.NET Validation)")
                .setMessage("Your registration request was rejected by the backend for the following reasons:\n\n" + errorMessage)
                .setPositiveButton("Fix Fields", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void completeRegistration(User user) {
        sessionManager.createSession(user);
        Intent intent = new Intent(RegisterActivity.this, ProsumerMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
