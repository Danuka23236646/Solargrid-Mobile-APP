package com.example.smartsolarmicrogrid.ui.operator;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.adapters.BookingsAdapter;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.models.EnergyReservation;
import com.example.smartsolarmicrogrid.models.User;
import com.example.smartsolarmicrogrid.network.ApiClient;
import com.example.smartsolarmicrogrid.network.dto.ReservationDto;
import com.example.smartsolarmicrogrid.network.dto.UserDto;
import com.example.smartsolarmicrogrid.ui.auth.LoginActivity;
import com.example.smartsolarmicrogrid.ui.prosumer.booking.BookingSummaryActivity;
import com.example.smartsolarmicrogrid.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OperatorMainActivity extends AppCompatActivity {

    private TextView tvOperatorInfo;
    private Button btnScanQrCode, btnOperatorLogout;
    private RecyclerView rvOperatorBookings;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private BookingsAdapter bookingsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_main);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        tvOperatorInfo = findViewById(R.id.tvOperatorInfo);
        btnScanQrCode = findViewById(R.id.btnScanQrCode);
        btnOperatorLogout = findViewById(R.id.btnOperatorLogout);
        rvOperatorBookings = findViewById(R.id.rvOperatorBookings);

        updateOperatorHeaderUI();

        rvOperatorBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingsAdapter = new BookingsAdapter(booking -> {
            Intent intent = new Intent(OperatorMainActivity.this, BookingSummaryActivity.class);
            intent.putExtra("BOOKING", booking);
            startActivity(intent);
        });
        rvOperatorBookings.setAdapter(bookingsAdapter);

        btnScanQrCode.setOnClickListener(v -> {
            Intent intent = new Intent(OperatorMainActivity.this, QrScannerActivity.class);
            startActivity(intent);
        });

        btnOperatorLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Intent intent = new Intent(OperatorMainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void updateOperatorHeaderUI() {
        String email = sessionManager.getUserEmail();
        String nic = sessionManager.getUserNic();

        StringBuilder sb = new StringBuilder("Logged in: ");
        sb.append(email.isEmpty() ? "operator@sungrid.com" : email);
        if (!TextUtils.isEmpty(nic)) {
            sb.append("\nNIC: ").append(nic);
        }
        tvOperatorInfo.setText(sb.toString());
    }

    private void fetchLiveOperatorProfile() {
        ApiClient.getApiService(this).getMyUserProfile().enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserDto dto = response.body();
                    User u = new User(1, dto.getNic(), dto.getFullName(), dto.getEmail(), "GridOperator", sessionManager.getAuthToken(), "Active");
                    sessionManager.createSession(u);
                    dbHelper.saveUser(u);
                    updateOperatorHeaderUI();
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                // Keep current
            }
        });
    }

    private void fetchLiveOperatorBookings() {
        ApiClient.getApiService(this).getMyReservationHistory().enqueue(new Callback<List<ReservationDto>>() {
            @Override
            public void onResponse(Call<List<ReservationDto>> call, Response<List<ReservationDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ReservationDto> dtos = response.body();
                    List<EnergyReservation> liveBookings = new ArrayList<>();
                    int idx = 1;

                    for (ReservationDto dto : dtos) {
                        int bookingId = idx++;
                        try {
                            if (dto.getId() != null && dto.getId().matches("\\d+")) {
                                bookingId = Integer.parseInt(dto.getId());
                            }
                        } catch (Exception ignored) {}

                        EnergyReservation existing = dbHelper.getBookingById(bookingId);

                        String reservationProsumerNic = (!TextUtils.isEmpty(dto.getProsumerNic())) 
                                ? dto.getProsumerNic() 
                                : (existing != null ? existing.getProsumerNic() : "200313000240");

                        double kwh = (existing != null && existing.getKwh() > 0)
                                ? existing.getKwh()
                                : (dto.getEnergyAmountKwh() > 0 ? dto.getEnergyAmountKwh() : 0.0);

                        String time = (existing != null && !TextUtils.isEmpty(existing.getScheduledTime()))
                                ? existing.getScheduledTime()
                                : (!TextUtils.isEmpty(dto.getScheduledTime()) ? dto.getScheduledTime() : "2026-09-29 14:00");

                        EnergyReservation b = new EnergyReservation(
                                bookingId,
                                reservationProsumerNic,
                                1,
                                dto.getStationName() != null ? dto.getStationName() : "Microgrid Station",
                                time,
                                kwh,
                                dto.getTransferType() != null ? dto.getTransferType() : "EnergyDropOff",
                                dto.getStatus() != null ? dto.getStatus() : "CONFIRMED",
                                dto.getQrPayload() != null ? dto.getQrPayload() : "SUNGRID:RESERVATION:" + bookingId
                        );

                        dbHelper.insertBooking(b);
                        liveBookings.add(b);
                    }

                    if (!liveBookings.isEmpty()) {
                        bookingsAdapter.setBookings(liveBookings);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ReservationDto>> call, Throwable t) {
                // Fallback local SQLite
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOperatorHeaderUI();
        fetchLiveOperatorProfile();
        loadAllBookings();
        fetchLiveOperatorBookings();
    }

    private void loadAllBookings() {
        List<EnergyReservation> allBookings = dbHelper.getAllBookings();
        bookingsAdapter.setBookings(allBookings);
    }
}
