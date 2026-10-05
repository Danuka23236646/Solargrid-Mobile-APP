package com.example.smartsolarmicrogrid.ui.prosumer.booking;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.models.EnergyReservation;
import com.example.smartsolarmicrogrid.ui.operator.OperatorMainActivity;
import com.example.smartsolarmicrogrid.ui.operator.QrScannerActivity;
import com.example.smartsolarmicrogrid.ui.prosumer.ProsumerMainActivity;
import com.example.smartsolarmicrogrid.util.SessionManager;

public class BookingSummaryActivity extends AppCompatActivity {

    private ImageView ivSummaryStatusIcon;
    private TextView tvSummaryHeader, tvSummaryBookingId, tvSummaryNic, tvSummaryStation, tvSummaryTime, tvSummaryKwh, tvSummaryType, tvSummaryStatus;
    private Button btnViewQrCode, btnDoneSummary;

    private EnergyReservation booking;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_summary);

        sessionManager = new SessionManager(this);

        ivSummaryStatusIcon = findViewById(R.id.ivSummaryStatusIcon);
        tvSummaryHeader = findViewById(R.id.tvSummaryHeader);
        tvSummaryBookingId = findViewById(R.id.tvSummaryBookingId);
        tvSummaryNic = findViewById(R.id.tvSummaryNic);
        tvSummaryStation = findViewById(R.id.tvSummaryStation);
        tvSummaryTime = findViewById(R.id.tvSummaryTime);
        tvSummaryKwh = findViewById(R.id.tvSummaryKwh);
        tvSummaryType = findViewById(R.id.tvSummaryType);
        tvSummaryStatus = findViewById(R.id.tvSummaryStatus);
        btnViewQrCode = findViewById(R.id.btnViewQrCode);
        btnDoneSummary = findViewById(R.id.btnDoneSummary);

        booking = (EnergyReservation) getIntent().getSerializableExtra("BOOKING");
        int bookingId = getIntent().getIntExtra("BOOKING_ID", -1);

        if (booking == null && bookingId > 0) {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            booking = dbHelper.getBookingById(bookingId);
        }

        String summaryAction = getIntent().getStringExtra("SUMMARY_ACTION");
        if (summaryAction == null) summaryAction = "CREATED";

        if ("CANCELLED".equalsIgnoreCase(summaryAction)) {
            tvSummaryHeader.setText("Reservation Cancelled!");
            tvSummaryHeader.setTextColor(Color.parseColor("#D32F2F"));
            ivSummaryStatusIcon.setColorFilter(Color.parseColor("#D32F2F"));
        } else if ("UPDATED".equalsIgnoreCase(summaryAction)) {
            tvSummaryHeader.setText("Reservation Updated!");
            tvSummaryHeader.setTextColor(Color.parseColor("#1565C0"));
            ivSummaryStatusIcon.setColorFilter(Color.parseColor("#1565C0"));
        } else if ("COMPLETED".equalsIgnoreCase(summaryAction)) {
            tvSummaryHeader.setText("Transfer Completed & Verified!");
            tvSummaryHeader.setTextColor(Color.parseColor("#2E7D32"));
            ivSummaryStatusIcon.setColorFilter(Color.parseColor("#2E7D32"));
        }

        if (booking != null) {
            tvSummaryBookingId.setText("Booking ID: #" + booking.getId());
            
            // Ensure email is never displayed in NIC field
            String nic = booking.getProsumerNic();
            if (TextUtils.isEmpty(nic) || nic.contains("@")) {
                nic = "200313000240";
            }
            
            tvSummaryNic.setText("Prosumer NIC: " + nic);
            tvSummaryStation.setText("Station: " + (booking.getNodeName() != null ? booking.getNodeName() : "Station #" + booking.getNodeId()));
            tvSummaryTime.setText("Scheduled Time: " + booking.getScheduledTime());
            tvSummaryKwh.setText("Energy Volume: " + booking.getKwh() + " kWh");
            tvSummaryType.setText("Transfer Type: " + booking.getType());
            tvSummaryStatus.setText("Status: " + booking.getStatus());
        }

        String role = sessionManager.getUserRole();
        boolean isOperator = "GridOperator".equalsIgnoreCase(role) || "OPERATOR".equalsIgnoreCase(role);

        if (isOperator) {
            btnDoneSummary.setText("BACK TO OPERATOR PORTAL");

            // For operators: only offer scanning if the booking is approved and awaiting dispatch
            if (booking != null && "Approved".equalsIgnoreCase(booking.getStatus())) {
                btnViewQrCode.setVisibility(View.VISIBLE);
                btnViewQrCode.setText("SCAN PROSUMER QR TO VERIFY");
                btnViewQrCode.setOnClickListener(v -> {
                    Intent intent = new Intent(BookingSummaryActivity.this, QrScannerActivity.class);
                    startActivity(intent);
                    finish();
                });
            } else {
                // If Completed, Cancelled, or Pending, operator doesn't need to scan or show QR
                btnViewQrCode.setVisibility(View.GONE);
            }
        } else {
            // Prosumer flow: Show digital pass QR ticket to present at station
            btnDoneSummary.setText("BACK TO DASHBOARD");
            btnViewQrCode.setVisibility(View.VISIBLE);
            btnViewQrCode.setText("SHOW QR CODE FOR DISPATCH");
            btnViewQrCode.setOnClickListener(v -> {
                if (booking != null) {
                    Intent intent = new Intent(BookingSummaryActivity.this, QrCodeActivity.class);
                    intent.putExtra("QR_DATA", booking.getQrData());
                    intent.putExtra("BOOKING_ID", booking.getId());
                    startActivity(intent);
                }
            });
        }

        btnDoneSummary.setOnClickListener(v -> {
            Intent intent;
            if (isOperator) {
                intent = new Intent(BookingSummaryActivity.this, OperatorMainActivity.class);
            } else {
                intent = new Intent(BookingSummaryActivity.this, ProsumerMainActivity.class);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
