package com.example.smartsolarmicrogrid.ui.prosumer.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.models.EnergyReservation;
import com.example.smartsolarmicrogrid.models.SolarStation;
import com.example.smartsolarmicrogrid.network.ApiClient;
import com.example.smartsolarmicrogrid.network.dto.CreateReservationRequest;
import com.example.smartsolarmicrogrid.network.dto.ReservationDto;
import com.example.smartsolarmicrogrid.network.dto.StationDto;
import com.example.smartsolarmicrogrid.ui.auth.LoginActivity;
import com.example.smartsolarmicrogrid.util.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateBookingActivity extends AppCompatActivity {

    private MaterialToolbar toolbarCreateBooking;
    private Spinner spStations;
    private RadioGroup rgType;
    private RadioButton rbSell, rbBuy;
    private TextInputEditText etKwh, etScheduledDate, etScheduledTime;
    private Button btnSubmitBooking;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private List<SolarStation> stationList = new ArrayList<>();
    private final Calendar selectedCalendar = Calendar.getInstance();

    private int editBookingId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_booking);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        toolbarCreateBooking = findViewById(R.id.toolbarCreateBooking);
        spStations = findViewById(R.id.spStations);
        rgType = findViewById(R.id.rgType);
        rbSell = findViewById(R.id.rbSell);
        rbBuy = findViewById(R.id.rbBuy);
        etKwh = findViewById(R.id.etKwh);
        etScheduledDate = findViewById(R.id.etScheduledDate);
        etScheduledTime = findViewById(R.id.etScheduledTime);
        btnSubmitBooking = findViewById(R.id.btnSubmitBooking);

        toolbarCreateBooking.setNavigationOnClickListener(v -> finish());

        editBookingId = getIntent().getIntExtra("EDIT_BOOKING_ID", -1);
        if (editBookingId > 0) {
            toolbarCreateBooking.setTitle("Modify Energy Reservation");
            btnSubmitBooking.setText("UPDATE RESERVATION");
        }

        setupStationsSpinner();
        fetchLiveStationsFromApi();
        setupDatePickerWith7DayHorizonRule();
        setupTimePicker();

        if (editBookingId > 0) {
            populateExistingBookingData();
        }

        btnSubmitBooking.setOnClickListener(v -> submitBooking());
    }

    private void setupStationsSpinner() {
        stationList = dbHelper.getAllStations();
        updateSpinnerUI();
    }

    private void fetchLiveStationsFromApi() {
        ApiClient.getApiService(this).getActiveStations().enqueue(new Callback<List<StationDto>>() {
            @Override
            public void onResponse(Call<List<StationDto>> call, Response<List<StationDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<StationDto> dtos = response.body();
                    List<SolarStation> fetchedStations = new ArrayList<>();
                    int idx = 1;
                    for (StationDto dto : dtos) {
                        int stationId = idx++;
                        try {
                            if (dto.getId() != null && dto.getId().matches("\\d+")) {
                                stationId = Integer.parseInt(dto.getId());
                            } else if (dto.getId() != null) {
                                stationId = Math.abs(dto.getId().hashCode());
                            }
                        } catch (Exception ignored) {}

                        SolarStation s = new SolarStation(
                                stationId,
                                dto.getName() != null ? dto.getName() : "Station " + dto.getId(),
                                dto.getAddress() != null ? dto.getAddress() : "Active Station",
                                dto.getLatitude(),
                                dto.getLongitude(),
                                dto.getCapacityKwh(),
                                dto.getAvailableSlots()
                        );
                        if (dto.getId() != null) {
                            s.setStringId(dto.getId()); // Store real MongoDB station ObjectId
                        }
                        if (dto.getCurrentStoredEnergyKwh() != null) {
                            s.setCurrentStoredEnergyKwh(dto.getCurrentStoredEnergyKwh());
                        }
                        if (dto.getAvailableIntakeKwh() != null) {
                            s.setAvailableIntakeKwh(dto.getAvailableIntakeKwh());
                        }
                        if (dto.getBatteryStoragePercentage() != null) {
                            s.setBatteryStoragePercentage(dto.getBatteryStoragePercentage());
                        }

                        boolean outOfStorage = dto.isOutOfStorage() || dto.getAvailableSlots() <= 0
                                || (dto.getAvailableIntakeKwh() != null && dto.getAvailableIntakeKwh() <= 0.0);
                        s.setOutOfStorage(outOfStorage);
                        fetchedStations.add(s);
                    }

                    if (!fetchedStations.isEmpty()) {
                        dbHelper.saveStations(fetchedStations);
                        stationList = fetchedStations;
                        updateSpinnerUI();
                        if (editBookingId > 0) {
                            populateExistingBookingData();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<StationDto>> call, Throwable t) {
                android.util.Log.e("CreateBooking", "Failed to fetch live stations: " + t.getMessage(), t);
            }
        });
    }

    private void updateSpinnerUI() {
        List<String> stationNames = new ArrayList<>();
        int selectedIndex = 0;
        int preselectedStationId = getIntent().getIntExtra("STATION_ID", -1);

        for (int i = 0; i < stationList.size(); i++) {
            SolarStation s = stationList.get(i);
            String label;
            if (s.isOutOfStorage()) {
                label = s.getName() + " [\u26D4 OUT OF STORAGE - BATTERY FULL]";
            } else {
                if (s.getAvailableIntakeKwh() > 0) {
                    label = s.getName() + " (" + s.getAvailableSlots() + " slots, " + String.format(Locale.getDefault(), "%.1f", s.getAvailableIntakeKwh()) + " kWh intake avail)";
                } else {
                    label = s.getName() + " (" + s.getAvailableSlots() + " slots available)";
                }
            }
            stationNames.add(label);
            if (s.getId() == preselectedStationId) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stationNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStations.setAdapter(adapter);
        if (!stationNames.isEmpty()) {
            spStations.setSelection(selectedIndex);
        }
    }

    private void populateExistingBookingData() {
        if (editBookingId <= 0) return;

        EnergyReservation booking = dbHelper.getBookingById(editBookingId);
        if (booking == null) return;

        // 1. Select matching station in Spinner
        for (int i = 0; i < stationList.size(); i++) {
            if (stationList.get(i).getId() == booking.getNodeId()) {
                spStations.setSelection(i);
                break;
            }
        }

        // 2. Select Transfer Type
        if ("Charging".equalsIgnoreCase(booking.getType()) || "BUY".equalsIgnoreCase(booking.getType())) {
            rbBuy.setChecked(true);
        } else {
            rbSell.setChecked(true);
        }

        // 3. Pre-fill Energy Amount (kWh)
        etKwh.setText(String.valueOf(booking.getKwh()));

        // 4. Pre-fill Scheduled Date & Time
        String scheduledTime = booking.getScheduledTime();
        if (!TextUtils.isEmpty(scheduledTime)) {
            String[] parts = scheduledTime.split(" ");
            if (parts.length >= 1) {
                etScheduledDate.setText(parts[0]);
            }
            if (parts.length >= 2) {
                etScheduledTime.setText(parts[1]);
            }
        }
    }

    private void setupDatePickerWith7DayHorizonRule() {
        etScheduledDate.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            Calendar maxDate = Calendar.getInstance();
            maxDate.add(Calendar.DAY_OF_MONTH, 7);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    CreateBookingActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        selectedCalendar.set(Calendar.YEAR, year);
                        selectedCalendar.set(Calendar.MONTH, month);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        etScheduledDate.setText(formattedDate);
                    },
                    today.get(Calendar.YEAR),
                    today.get(Calendar.MONTH),
                    today.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.getDatePicker().setMinDate(today.getTimeInMillis());
            datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
            datePickerDialog.show();
        });
    }

    private void setupTimePicker() {
        etScheduledTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    CreateBookingActivity.this,
                    (view, hourOfDay, minute) -> {
                        selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedCalendar.set(Calendar.MINUTE, minute);

                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        etScheduledTime.setText(formattedTime);
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });
    }

    private void submitBooking() {
        if (spStations.getSelectedItemPosition() < 0 || stationList.isEmpty()) {
            Toast.makeText(this, "Please select a valid microgrid solar station", Toast.LENGTH_SHORT).show();
            return;
        }

        String kwhStr = etKwh.getText() != null ? etKwh.getText().toString().trim() : "";
        String dateStr = etScheduledDate.getText() != null ? etScheduledDate.getText().toString().trim() : "";
        String timeStr = etScheduledTime.getText() != null ? etScheduledTime.getText().toString().trim() : "";

        if (TextUtils.isEmpty(kwhStr)) {
            etKwh.setError("Energy amount (kWh) is required");
            return;
        }
        if (TextUtils.isEmpty(dateStr)) {
            etScheduledDate.setError("Please select a date (7-Day Horizon)");
            return;
        }
        if (TextUtils.isEmpty(timeStr)) {
            etScheduledTime.setError("Please select a time");
            return;
        }

        double kwh = Double.parseDouble(kwhStr);
        SolarStation station = stationList.get(spStations.getSelectedItemPosition());
        boolean isSell = rbSell.isChecked();

        // Enforce Substation Storage Capacity constraint: Prosumers cannot SELL into full substations
        if (isSell && station.isOutOfStorage()) {
            new AlertDialog.Builder(this)
                    .setTitle("Substation Out of Storage \u26D4")
                    .setMessage("Substation '" + station.getName() + "' currently cannot accept energy drop-offs.\n\nPlease select another microgrid substation or switch transaction type to Charging (Buy).")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Enforce Substation Storage Headroom constraint: Requested volume cannot exceed remaining intake capacity
        if (isSell && station.getAvailableIntakeKwh() > 0 && kwh > station.getAvailableIntakeKwh()) {
            new AlertDialog.Builder(this)
                    .setTitle("Storage Headroom Exceeded \u26A0")
                    .setMessage("The requested drop-off of " + kwh + " kWh exceeds the remaining battery storage headroom (" + String.format(Locale.getDefault(), "%.1f", station.getAvailableIntakeKwh()) + " kWh) for substation '" + station.getName() + "'.\n\nPlease reduce the energy amount or select another substation.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String transferTypeApi = isSell ? "EnergyDropOff" : "Charging";
        String scheduledTimeCombined = dateStr + " " + timeStr;
        String prosumerNic = sessionManager.getUserNic();

        EnergyReservation booking = new EnergyReservation();
        if (editBookingId > 0) {
            booking.setId(editBookingId);
            booking.setStatus("PENDING");
        } else {
            booking.setId((int) (System.currentTimeMillis() % 100000));
            booking.setStatus("PENDING"); // Initial status pending operator approval
        }
        booking.setProsumerNic(prosumerNic);
        booking.setNodeId(station.getId());
        booking.setNodeName(station.getName());
        booking.setScheduledTime(scheduledTimeCombined);
        booking.setKwh(kwh);
        booking.setType(rbSell.isChecked() ? "SELL" : "BUY");

        String qrPayload = "SUNGRID:RESERVATION:" + booking.getId() + ":" + prosumerNic;
        booking.setQrData(qrPayload);

        btnSubmitBooking.setEnabled(false);
        btnSubmitBooking.setText("SUBMITTING TO API...");

        // Construct API request with real MongoDB station ObjectId
        CreateReservationRequest apiReq = new CreateReservationRequest(
                station.getStringId(), // Real MongoDB station ObjectId
                "slot-01",
                transferTypeApi,
                kwh,
                scheduledTimeCombined,
                "Microgrid Transfer Reservation [Booking #" + booking.getId() + "]"
        );

        if (editBookingId > 0) {
            // PUT /reservations/{id}
            ApiClient.getApiService(this).updateReservation(String.valueOf(editBookingId), apiReq).enqueue(new Callback<ReservationDto>() {
                @Override
                public void onResponse(Call<ReservationDto> call, Response<ReservationDto> response) {
                    btnSubmitBooking.setEnabled(true);
                    btnSubmitBooking.setText("UPDATE RESERVATION");

                    if (response.code() == 401) {
                        handleSessionExpired();
                        return;
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        ReservationDto dto = response.body();
                        booking.setKwh(dto.getEnergyAmountKwh() > 0 ? dto.getEnergyAmountKwh() : kwh);
                        booking.setScheduledTime(scheduledTimeCombined);
                        if (dto.getStatus() != null) {
                            booking.setStatus(dto.getStatus());
                        }
                        dbHelper.updateBooking(booking);
                        Toast.makeText(CreateBookingActivity.this, "Reservation updated successfully on MongoDB!", Toast.LENGTH_SHORT).show();
                        openSummaryScreen(booking);
                    } else {
                        String errMsg = "API Update Error Code: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                String bodyStr = response.errorBody().string();
                                if (!TextUtils.isEmpty(bodyStr)) {
                                    errMsg = bodyStr;
                                }
                            }
                        } catch (Exception ignored) {}

                        new AlertDialog.Builder(CreateBookingActivity.this)
                                .setTitle("Update Failed \u26D4")
                                .setMessage(errMsg)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }

                @Override
                public void onFailure(Call<ReservationDto> call, Throwable t) {
                    btnSubmitBooking.setEnabled(true);
                    btnSubmitBooking.setText("UPDATE RESERVATION");
                    Toast.makeText(CreateBookingActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // POST /reservations
            ApiClient.getApiService(this).createReservation(apiReq).enqueue(new Callback<ReservationDto>() {
                @Override
                public void onResponse(Call<ReservationDto> call, Response<ReservationDto> response) {
                    btnSubmitBooking.setEnabled(true);
                    btnSubmitBooking.setText("CONFIRM RESERVATION");

                    if (response.code() == 401) {
                        handleSessionExpired();
                        return;
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        ReservationDto dto = response.body();
                        if (dto.getId() != null && dto.getId().matches("\\d+")) {
                            try {
                                int realId = Integer.parseInt(dto.getId());
                                booking.setId(realId);
                            } catch (Exception ignored) {}
                        }
                        if (dto.getId() != null) {
                            String realQr = "SUNGRID:RESERVATION:" + dto.getId() + ":" + prosumerNic;
                            booking.setQrData(realQr);
                        }
                        if (dto.getQrPayload() != null) {
                            booking.setQrData(dto.getQrPayload());
                        }
                        if (dto.getStatus() != null) {
                            booking.setStatus(dto.getStatus());
                        } else {
                            booking.setStatus("PENDING");
                        }
                        booking.setScheduledTime(scheduledTimeCombined);
                        booking.setKwh(kwh);
                        dbHelper.insertBooking(booking);
                        Toast.makeText(CreateBookingActivity.this, "Reservation saved to MongoDB Atlas!", Toast.LENGTH_SHORT).show();
                        openSummaryScreen(booking);
                    } else {
                        String errMsg = "API Creation Error Code: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                String bodyStr = response.errorBody().string();
                                if (!TextUtils.isEmpty(bodyStr)) {
                                    errMsg = bodyStr;
                                }
                            }
                        } catch (Exception ignored) {}

                        new AlertDialog.Builder(CreateBookingActivity.this)
                                .setTitle("Booking Rejected \u26D4")
                                .setMessage(errMsg)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }

                @Override
                public void onFailure(Call<ReservationDto> call, Throwable t) {
                    btnSubmitBooking.setEnabled(true);
                    btnSubmitBooking.setText("CONFIRM RESERVATION");
                    Toast.makeText(CreateBookingActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleSessionExpired() {
        Toast.makeText(this, "Session Expired (HTTP 401). Please re-login to obtain a fresh JWT token.", Toast.LENGTH_LONG).show();
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openSummaryScreen(EnergyReservation booking) {
        Intent intent = new Intent(CreateBookingActivity.this, BookingSummaryActivity.class);
        intent.putExtra("BOOKING", booking);
        intent.putExtra("SUMMARY_ACTION", editBookingId > 0 ? "UPDATED" : "CREATED");
        startActivity(intent);
        finish();
    }
}