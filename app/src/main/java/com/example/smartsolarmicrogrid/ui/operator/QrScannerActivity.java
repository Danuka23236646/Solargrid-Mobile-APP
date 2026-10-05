package com.example.smartsolarmicrogrid.ui.operator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.models.EnergyReservation;
import com.example.smartsolarmicrogrid.network.ApiClient;
import com.example.smartsolarmicrogrid.network.dto.CompleteQrRequest;
import com.example.smartsolarmicrogrid.network.dto.VerifyQrRequest;
import com.example.smartsolarmicrogrid.ui.prosumer.booking.BookingSummaryActivity;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONObject;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 101;

    private DecoratedBarcodeView barcodeScannerView;
    private Button btnManualInput;

    private DatabaseHelper dbHelper;
    private boolean isScanned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        dbHelper = DatabaseHelper.getInstance(this);

        barcodeScannerView = findViewById(R.id.barcodeScannerView);
        btnManualInput = findViewById(R.id.btnManualInput);

        checkCameraPermission();

        btnManualInput.setOnClickListener(v -> showManualInputDialog());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            startScanning();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startScanning() {
        if (barcodeScannerView == null) return;
        barcodeScannerView.resume();
        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && result.getText() != null && !isScanned) {
                    isScanned = true;
                    processScannedQrCode(result.getText().trim());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeScannerView != null && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeScannerView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScannerView != null) {
            barcodeScannerView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeScannerView != null) {
            barcodeScannerView.pause();
        }
    }

    private void showManualInputDialog() {
        final EditText input = new EditText(this);
        input.setHint("e.g. RES-20260925-XXXXXX or Booking ID");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Manual Reservation Verification")
                .setMessage("Enter the Reservation Reference code, MongoDB ID, or Prosumer NIC:")
                .setView(input)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(code)) {
                        isScanned = true;
                        processScannedQrCode(code);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    isScanned = false;
                })
                .show();
    }

    private void processScannedQrCode(String qrData) {
        Toast.makeText(this, "Verifying Reservation with SunGrid...", Toast.LENGTH_SHORT).show();

        VerifyQrRequest verifyReq = new VerifyQrRequest(qrData);
        ApiClient.getApiService(this).verifyQrPayload(verifyReq).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonStr = response.body().string();
                        JSONObject obj = new JSONObject(jsonStr);

                        boolean canComplete = obj.optBoolean("canComplete", false);
                        String message = obj.optString("message", "");
                        String resId = obj.optString("reservationId", "");
                        String resRef = obj.optString("reservationReference", "RES-REFERENCE");
                        String prosumerName = obj.optString("prosumerName", "Prosumer");
                        String prosumerNic = obj.optString("prosumerNic", "");
                        String stationName = obj.optString("stationName", "Microgrid Station");
                        double kwh = obj.optDouble("expectedEnergyAmountKwh", 0.0);
                        String transferType = obj.optString("transferType", "SELL");
                        String status = obj.optString("reservationStatus", "Pending");

                        if (!canComplete) {
                            new AlertDialog.Builder(QrScannerActivity.this)
                                    .setTitle("Cannot Dispatch Energy \u26D4")
                                    .setMessage(!TextUtils.isEmpty(message)
                                            ? message
                                            : "Reservation status is '" + status + "'. Operator must approve the reservation on the web portal before energy can be dispatched.")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        isScanned = false; // Allow rescanning
                                    })
                                    .setCancelable(false)
                                    .show();
                            return;
                        }

                        // Show operator confirmation modal before committing dispatch
                        new AlertDialog.Builder(QrScannerActivity.this)
                                .setTitle("Confirm Energy Dispatch \u26A1")
                                .setMessage("Prosumer: " + prosumerName + " (NIC: " + prosumerNic + ")\n"
                                        + "Station: " + stationName + "\n"
                                        + "Volume: " + kwh + " kWh (" + transferType + ")\n"
                                        + "Status: APPROVED \u2705\n\n"
                                        + "Do you confirm physical bay connection and want to dispatch energy?")
                                .setPositiveButton("CONFIRM & DISPATCH", (dialog, which) -> {
                                    completeTransferApi(qrData, resId, resRef, prosumerNic, stationName, kwh, transferType);
                                })
                                .setNegativeButton("CANCEL", (dialog, which) -> {
                                    isScanned = false;
                                })
                                .setCancelable(false)
                                .show();

                    } catch (Exception e) {
                        e.printStackTrace();
                        showScanError("Response Parsing Error: " + e.getMessage());
                    }
                } else {
                    String errorMsg = "Verification failed (HTTP " + response.code() + ")";
                    try {
                        if (response.errorBody() != null) {
                            String errBody = response.errorBody().string();
                            if (!TextUtils.isEmpty(errBody)) {
                                errorMsg = errBody;
                            }
                        }
                    } catch (Exception ignored) {}
                    showScanError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showScanError("Network Connection Error: " + t.getMessage());
            }
        });
    }

    private void completeTransferApi(String qrData, String resId, String resRef, String prosumerNic, String stationName, double kwh, String transferType) {
        CompleteQrRequest completeReq = new CompleteQrRequest(qrData, resId, kwh > 0 ? kwh : 25.0, "Verified and completed by operator via mobile scanner");

        ApiClient.getApiService(this).completeQrTransfer(completeReq).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    int parsedBookingId = -1;
                    try {
                        if (resId != null && resId.matches("\\d+")) {
                            parsedBookingId = Integer.parseInt(resId);
                        }
                    } catch (Exception ignored) {}

                    if (parsedBookingId > 0) {
                        dbHelper.updateBookingStatus(parsedBookingId, "COMPLETED");
                    }

                    int syntheticId = parsedBookingId > 0 ? parsedBookingId : Math.abs(resRef.hashCode() % 100000);
                    EnergyReservation completedBooking = new EnergyReservation(
                            syntheticId,
                            prosumerNic,
                            1,
                            stationName,
                            "Completed Just Now",
                            kwh,
                            transferType,
                            "COMPLETED",
                            qrData
                    );

                    new AlertDialog.Builder(QrScannerActivity.this)
                            .setTitle("Energy Transfer Completed! \u2705")
                            .setMessage("Energy transfer has been verified and marked COMPLETED in MongoDB Atlas!\n\n"
                                    + "Reference: " + resRef + "\n"
                                    + "Station: " + stationName + "\n"
                                    + "Energy Volume: " + kwh + " kWh (" + transferType + ")\n"
                                    + "Prosumer NIC: " + prosumerNic)
                            .setPositiveButton("View Summary", (dialog, which) -> {
                                Intent intent = new Intent(QrScannerActivity.this, BookingSummaryActivity.class);
                                intent.putExtra("BOOKING", completedBooking);
                                intent.putExtra("SUMMARY_ACTION", "COMPLETED");
                                startActivity(intent);
                                finish();
                            })
                            .setCancelable(false)
                            .show();

                } else {
                    String errorMsg = "Transfer Completion Failed (HTTP " + response.code() + ")";
                    try {
                        if (response.errorBody() != null) {
                            String errBody = response.errorBody().string();
                            if (!TextUtils.isEmpty(errBody)) {
                                errorMsg = errBody;
                            }
                        }
                    } catch (Exception ignored) {}
                    showScanError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showScanError("Network Error during Completion: " + t.getMessage());
            }
        });
    }

    private void showScanError(String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle("QR Scan Failed \u26D4")
                .setMessage(errorMessage)
                .setPositiveButton("Try Again", (dialog, which) -> {
                    isScanned = false; // Reset to allow scanning another QR code
                })
                .setCancelable(false)
                .show();
    }
}