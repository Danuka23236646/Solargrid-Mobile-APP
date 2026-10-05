package com.example.smartsolarmicrogrid.ui.prosumer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.network.ApiClient;
import com.example.smartsolarmicrogrid.network.dto.UserDto;
import com.example.smartsolarmicrogrid.ui.auth.LoginActivity;
import com.example.smartsolarmicrogrid.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileNic, tvProfileStatus, tvProfileEmail, tvProfileRole;
    private Button btnRequestDeactivation, btnLogout;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        if (getContext() != null) {
            dbHelper = DatabaseHelper.getInstance(getContext());
            sessionManager = new SessionManager(getContext());
        }

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileNic = view.findViewById(R.id.tvProfileNic);
        tvProfileStatus = view.findViewById(R.id.tvProfileStatus);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileRole = view.findViewById(R.id.tvProfileRole);
        btnRequestDeactivation = view.findViewById(R.id.btnRequestDeactivation);
        btnLogout = view.findViewById(R.id.btnLogout);

        updateProfileUI();

        btnRequestDeactivation.setOnClickListener(v -> showDeactivationConfirmationDialog());

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchLiveProfileFromApi();
    }

    private void updateProfileUI() {
        if (sessionManager == null) return;

        tvProfileName.setText(sessionManager.getUserName().isEmpty() ? "Prosumer Account" : sessionManager.getUserName());
        tvProfileNic.setText("NIC (Primary Key): " + (sessionManager.getUserNic().isEmpty() ? "200313000240" : sessionManager.getUserNic()));
        tvProfileStatus.setText("Account Status: " + sessionManager.getUserStatus());
        tvProfileEmail.setText("Email: " + sessionManager.getUserEmail());
        tvProfileRole.setText("Role: " + sessionManager.getUserRole());

        if ("Pending Deactivation".equalsIgnoreCase(sessionManager.getUserStatus())) {
            btnRequestDeactivation.setEnabled(false);
            btnRequestDeactivation.setText("Deactivation Request Pending");
        }
    }

    private void fetchLiveProfileFromApi() {
        if (getContext() == null || sessionManager == null) return;

        // Call GET /users/me
        ApiClient.getApiService(getContext()).getMyUserProfile().enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserDto dto = response.body();
                    if (!TextUtils.isEmpty(dto.getFullName())) {
                        tvProfileName.setText(dto.getFullName());
                    }
                    if (!TextUtils.isEmpty(dto.getNic())) {
                        tvProfileNic.setText("NIC (Primary Key): " + dto.getNic());
                    }
                    if (!TextUtils.isEmpty(dto.getEmail())) {
                        tvProfileEmail.setText("Email: " + dto.getEmail());
                    }
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                // Keep local
            }
        });
    }

    private void showDeactivationConfirmationDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Request Account Deactivation")
                .setMessage("Are you sure you want to submit a request to deactivate your Prosumer account (" + sessionManager.getUserEmail() + ")? This will freeze future energy trading bookings.")
                .setPositiveButton("Submit Request", (dialog, which) -> requestDeactivation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestDeactivation() {
        String nic = sessionManager.getUserNic();
        String newStatus = "Pending Deactivation";

        dbHelper.updateUserStatus(nic, newStatus);
        sessionManager.setUserStatus(newStatus);
        updateProfileUI();

        if (getContext() != null) {
            Toast.makeText(getContext(), "Deactivation request submitted", Toast.LENGTH_SHORT).show();
        }
    }
}
