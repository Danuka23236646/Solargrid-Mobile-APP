package com.example.smartsolarmicrogrid.ui.prosumer.booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.models.EnergyReservation;
import com.example.smartsolarmicrogrid.network.ApiClient;
import com.example.smartsolarmicrogrid.network.dto.QrPayloadResponse;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingDetailsActivity extends AppCompatActivity {

    private MaterialToolbar toolbarDetails;
    private CardView cardLockWarning;
    private TextView tvDetailsBookingId, tvDetailsProsumerNic, tvDetailsStation, tvDetailsScheduledTime, tvDetailsKwh, tvDetailsType, tvDetailsStatus;
    private Button btnGenerateQrCode, btnModifyBooking, btnCancelBooking;

    private DatabaseHelper dbHelper;
    private EnergyReservation booking;
    private int bookingId;
    private boolean isLockedUnder12Hours = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        dbHelper = DatabaseHelper.getInstance(this);

        toolbarDetails = findViewById(R.id.toolbarDetails);
        cardLockWarning = findViewById(R.id.cardLockWarning);
        tvDetailsBookingId = findViewById(R.id.tvDetailsBookingId);
        tvDetailsProsumerNic = findViewById(R.id.tvDetailsProsumerNic);
        tvDetailsStation = findViewById(R.id.tvDetailsStation);
        tvDetailsScheduledTime = findViewById(R.id.tvDetailsScheduledTime);
        tvDetailsKwh = findViewById(R.id.tvDetailsKwh);
        tvDetailsType = findViewById(R.id.tvDetailsType);
        tvDetailsStatus = findViewById(R.id.tvDetailsStatus);
        btnGenerateQrCode = findViewById(R.id.btnGenerateQrCode);
        btnModifyBooking = findViewById(R.id.btnModifyBooking);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);

        toolbarDetails.setNavigationOnClickListener(v -> finish());

        bookingId = getIntent().getIntExtra("BOOKING_ID", -1);
        loadBookingDetails();

        btnGenerateQrCode.setOnClickListener(v -> fetchAndOpenQrCode());

        btnModifyBooking.setOnClickListener(v -> {
            if (isLockedUnder12Hours) {
                show12HourLockAlertDialog();
            } else {
                Intent intent = new Intent(BookingDetailsActivity.this, CreateBookingActivity.class);
                intent.putExtra("EDIT_BOOKING_ID", bookingId);
                startActivity(intent);
            }
        });

        btnCancelBooking.setOnClickListener(v -> {
            if (isLockedUnder12Hours) {
                show12HourLockAlertDialog();
            } else {
                showCancelConfirmationDialog();
            }
        });
    }

    private void fetchAndOpenQrCode() {
        if (booking == null) return;

        // Attempt generating QR payload from ASP.NET Core API /api/reservations/{id}/qr
        ApiClient.getApiService(this).generateQrPayload(String.valueOf(booking.getId())).enqueue(new Callback<QrPayloadResponse>() {
            @Override
            public void onResponse(Call<QrPayloadResponse> call, Response<QrPayloadResponse> response) {
                String payload = booking.getQrData();
                if (response.isSuccessful() && response.body() != null && response.body().getQrPayload() != null) {
                    payload = response.body().getQrPayload();
                    booking.setQrData(payload);
                    dbHelper.updateBooking(booking);
                }
                launchQrActivity(payload);
            }

            @Override
            public void onFailure(Call<QrPayloadResponse> call, Throwable t) {
                launchQrActivity(booking.getQrData());
            }
        });
    }

    private void launchQrActivity(String qrData) {
        Intent intent = new Intent(BookingDetailsActivity.this, QrCodeActivity.class);
        intent.putExtra("QR_DATA", qrData != null ? qrData : "SUNGRID:RESERVATION:" + booking.getId());
        intent.putExtra("BOOKING_ID", booking.getId());
        startActivity(intent);
    }

    private void loadBookingDetails() {
        if (bookingId <= 0) return;

        booking = dbHelper.getBookingById(bookingId);
        if (booking == null) {
            Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDetailsBookingId.setText("Booking #" + booking.getId());
        tvDetailsProsumerNic.setText("Prosumer NIC: " + booking.getProsumerNic());
        tvDetailsStation.setText("Microgrid Station: " + (booking.getNodeName() != null ? booking.getNodeName() : "Station #" + booking.getNodeId()));
        tvDetailsScheduledTime.setText("Scheduled Time: " + booking.getScheduledTime());
        tvDetailsKwh.setText("Energy Volume: " + booking.getKwh() + " kWh");
        tvDetailsType.setText("Type: " + booking.getType());
        tvDetailsStatus.setText("Status: " + booking.getStatus());

        check12HourCancellationUpdateLock();
    }

    private void check12HourCancellationUpdateLock() {
        if (booking == null || "CANCELLED".equalsIgnoreCase(booking.getStatus()) || "COMPLETED".equalsIgnoreCase(booking.getStatus())) {
            btnModifyBooking.setEnabled(false);
            btnCancelBooking.setEnabled(false);
            return;
        }

        String scheduledTimeStr = booking.getScheduledTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        try {
            Date scheduledDate = sdf.parse(scheduledTimeStr);
            if (scheduledDate != null) {
                long scheduledTimeMillis = scheduledDate.getTime();
                long currentTimeMillis = System.currentTimeMillis();
                long remainingMillis = scheduledTimeMillis - currentTimeMillis;
                long twelveHoursMillis = 12 * 60 * 60 * 1000L;

                if (remainingMillis < twelveHoursMillis) {
                    isLockedUnder12Hours = true;
                    cardLockWarning.setVisibility(View.VISIBLE);

                    btnModifyBooking.setAlpha(0.5f);
                    btnCancelBooking.setAlpha(0.5f);
                } else {
                    isLockedUnder12Hours = false;
                    cardLockWarning.setVisibility(View.GONE);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void show12HourLockAlertDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Action Locked (12-Hour Rule)")
                .setMessage("Reservations scheduled within 12 hours cannot be modified or cancelled. Your energy transfer is locked for dispatch preparation.")
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Reservation")
                .setMessage("Are you sure you want to cancel this energy transfer reservation (#" + bookingId + ")?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelReservation())
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelReservation() {
        dbHelper.cancelBooking(bookingId);
        booking.setStatus("CANCELLED");

        Intent intent = new Intent(BookingDetailsActivity.this, BookingSummaryActivity.class);
        intent.putExtra("BOOKING", booking);
        intent.putExtra("SUMMARY_ACTION", "CANCELLED");
        startActivity(intent);
        finish();
    }
}
