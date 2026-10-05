package com.example.smartsolarmicrogrid.ui.prosumer.booking;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.models.EnergyReservation;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QrCodeActivity extends AppCompatActivity {

    private MaterialToolbar toolbarQrCode;
    private ImageView ivQrCode;
    private TextView tvQrBookingRef, tvQrMaskedNic, tvQrDetails;
    private Button btnCloseQr;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        dbHelper = DatabaseHelper.getInstance(this);

        toolbarQrCode = findViewById(R.id.toolbarQrCode);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvQrBookingRef = findViewById(R.id.tvQrBookingRef);
        tvQrMaskedNic = findViewById(R.id.tvQrMaskedNic);
        tvQrDetails = findViewById(R.id.tvQrDetails);
        btnCloseQr = findViewById(R.id.btnCloseQr);

        toolbarQrCode.setNavigationOnClickListener(v -> finish());

        String qrData = getIntent().getStringExtra("QR_DATA");
        int bookingId = getIntent().getIntExtra("BOOKING_ID", -1);

        EnergyReservation booking = null;
        if (bookingId > 0) {
            booking = dbHelper.getBookingById(bookingId);
        }

        if (booking != null) {
            tvQrBookingRef.setText("Ref: #" + booking.getId());
            tvQrMaskedNic.setText("Prosumer NIC: " + maskNic(booking.getProsumerNic()));
            tvQrDetails.setText("Volume: " + booking.getKwh() + " kWh (" + booking.getType() + ")");
            if (TextUtils.isEmpty(qrData)) {
                qrData = booking.getQrData();
            }
        } else {
            tvQrBookingRef.setText("Ref: #" + bookingId);
            tvQrMaskedNic.setText("Prosumer NIC: " + maskNic(qrData));
            tvQrDetails.setText("Transaction Transfer Token");
        }

        if (TextUtils.isEmpty(qrData)) {
            qrData = "SUNGRID:RESERVATION:" + bookingId;
        }

        generateQrBitmap(qrData);

        btnCloseQr.setOnClickListener(v -> finish());
    }

    /**
     * Mask NIC number for security against over-the-shoulder snooping.
     */
    private String maskNic(String nic) {
        if (TextUtils.isEmpty(nic) || nic.length() < 6) return "*****";
        return nic.substring(0, Math.min(4, nic.length())) + "****";
    }

    private void generateQrBitmap(String data) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 600, 600);
            ivQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR Code", Toast.LENGTH_SHORT).show();
        }
    }
}
