package com.example.smartsolarmicrogrid.ui.prosumer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.adapters.BookingsAdapter;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.models.EnergyReservation;
import com.example.smartsolarmicrogrid.network.ApiClient;
import com.example.smartsolarmicrogrid.network.dto.DashboardSummaryDto;
import com.example.smartsolarmicrogrid.network.dto.ReservationDto;
import com.example.smartsolarmicrogrid.ui.prosumer.booking.BookingDetailsActivity;
import com.example.smartsolarmicrogrid.ui.prosumer.booking.CreateBookingActivity;
import com.example.smartsolarmicrogrid.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private TextView tvWelcomeUser, tvNicLabel, tvTotalEnergy, tvActiveBookingsCount;
    private Button btnCreateNewBooking, btnViewStationsMap;
    private LinearLayout layoutEmptyDashboard;
    private RecyclerView rvRecentBookings;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private BookingsAdapter bookingsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        if (getContext() != null) {
            dbHelper = DatabaseHelper.getInstance(getContext());
            sessionManager = new SessionManager(getContext());
        }

        tvWelcomeUser = view.findViewById(R.id.tvWelcomeUser);
        tvNicLabel = view.findViewById(R.id.tvNicLabel);
        tvTotalEnergy = view.findViewById(R.id.tvTotalEnergy);
        tvActiveBookingsCount = view.findViewById(R.id.tvActiveBookingsCount);
        btnCreateNewBooking = view.findViewById(R.id.btnCreateNewBooking);
        btnViewStationsMap = view.findViewById(R.id.btnViewStationsMap);
        layoutEmptyDashboard = view.findViewById(R.id.layoutEmptyDashboard);
        rvRecentBookings = view.findViewById(R.id.rvRecentBookings);

        rvRecentBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        bookingsAdapter = new BookingsAdapter(booking -> {
            Intent intent = new Intent(getActivity(), BookingDetailsActivity.class);
            intent.putExtra("BOOKING_ID", booking.getId());
            startActivity(intent);
        });
        rvRecentBookings.setAdapter(bookingsAdapter);

        btnCreateNewBooking.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateBookingActivity.class);
            startActivity(intent);
        });

        btnViewStationsMap.setOnClickListener(v -> {
            if (getActivity() instanceof ProsumerMainActivity) {
                ((ProsumerMainActivity) getActivity()).switchToMapTab();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardLocalData();
        fetchLiveDashboardFromApi();
    }

    private void loadDashboardLocalData() {
        if (sessionManager == null || dbHelper == null) return;

        String name = sessionManager.getUserName();
        String nic = sessionManager.getUserNic();

        tvWelcomeUser.setText("Welcome, " + (name.isEmpty() ? "Prosumer" : name) + "!");
        tvNicLabel.setText("NIC (Primary Key): " + (nic.isEmpty() ? sessionManager.getUserEmail() : nic));

        List<EnergyReservation> bookings = dbHelper.getBookingsForProsumer(nic);
        updateRecentBookingsList(bookings);

        double localSold = 0.0;
        double localBought = 0.0;
        int localActive = 0;
        for (EnergyReservation r : bookings) {
            if ("Completed".equalsIgnoreCase(r.getStatus())) {
                if ("EnergyDropOff".equalsIgnoreCase(r.getType()) || "Sell".equalsIgnoreCase(r.getType())) {
                    localSold += r.getKwh();
                } else if ("Charging".equalsIgnoreCase(r.getType()) || "Buy".equalsIgnoreCase(r.getType())) {
                    localBought += r.getKwh();
                }
            } else if ("Pending".equalsIgnoreCase(r.getStatus()) || "Approved".equalsIgnoreCase(r.getStatus())) {
                localActive++;
            }
        }
        tvTotalEnergy.setText(String.format(Locale.getDefault(), "Sold: %.1f kWh\nBought: %.1f kWh", localSold, localBought));
        tvActiveBookingsCount.setText(localActive + " Active");
    }

    private void fetchLiveDashboardFromApi() {
        if (getContext() == null || sessionManager == null) return;

        // 1. Fetch GET /reservations/me/dashboard summary
        ApiClient.getApiService(getContext()).getMyDashboardSummary().enqueue(new Callback<DashboardSummaryDto>() {
            @Override
            public void onResponse(Call<DashboardSummaryDto> call, Response<DashboardSummaryDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DashboardSummaryDto summary = response.body();
                    tvTotalEnergy.setText(String.format(Locale.getDefault(), "Sold: %.1f kWh\nBought: %.1f kWh", summary.getTotalEnergySoldKwh(), summary.getTotalEnergyBoughtKwh()));
                    tvActiveBookingsCount.setText(summary.getTotalActive() + " Active");
                }
            }

            @Override
            public void onFailure(Call<DashboardSummaryDto> call, Throwable t) {
                // Keep local calculated counts
            }
        });

        // 2. Fetch GET /reservations/me/history
        ApiClient.getApiService(getContext()).getMyReservationHistory().enqueue(new Callback<List<ReservationDto>>() {
            @Override
            public void onResponse(Call<List<ReservationDto>> call, Response<List<ReservationDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ReservationDto> dtos = response.body();
                    if (!dtos.isEmpty()) {
                        dbHelper.clearBookingsCache(); // Clear stale local cache
                    }

                    List<EnergyReservation> liveBookings = new ArrayList<>();
                    int idx = 1;
                    String nowFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

                    String userNic = sessionManager.getUserNic();
                    if (TextUtils.isEmpty(userNic)) {
                        userNic = sessionManager.getUserEmail();
                    }

                    for (ReservationDto dto : dtos) {
                        int bookingId = idx++;
                        try {
                            if (dto.getId() != null) {
                                if (dto.getId().matches("\\d+")) {
                                    bookingId = Integer.parseInt(dto.getId());
                                } else {
                                    bookingId = Math.abs(dto.getId().hashCode()) % 100000;
                                    if (bookingId == 0) bookingId = idx;
                                }
                            }
                        } catch (Exception ignored) {}

                        // Prioritize API scheduled time (slotStartTimeUtc) over creation time
                        String time = (!TextUtils.isEmpty(dto.getScheduledTime())) 
                                ? dto.getScheduledTime() 
                                : nowFormatted;

                        double kwh = dto.getEnergyAmountKwh() > 0 ? dto.getEnergyAmountKwh() : 0.0;

                        EnergyReservation b = new EnergyReservation(
                                bookingId,
                                userNic, // Guarantees 100% match with local SQLite filter
                                1,
                                dto.getStationName() != null ? dto.getStationName() : "Microgrid Node",
                                time,
                                kwh,
                                dto.getTransferType() != null ? dto.getTransferType() : "EnergyDropOff",
                                dto.getStatus() != null ? dto.getStatus() : "Pending",
                                dto.getQrPayload() != null ? dto.getQrPayload() : "SUNGRID:RESERVATION:" + bookingId
                        );

                        dbHelper.insertBooking(b);
                        liveBookings.add(b);
                    }

                    if (!liveBookings.isEmpty()) {
                        // Robust multi-format date parser sorting (Newest scheduled date first)
                        Collections.sort(liveBookings, (b1, b2) -> {
                            Date d1 = parseAnyDate(b1.getScheduledTime());
                            Date d2 = parseAnyDate(b2.getScheduledTime());
                            if (d1 != null && d2 != null) {
                                return d2.compareTo(d1); // Newest scheduled date first
                            }
                            return 0;
                        });

                        updateRecentBookingsList(liveBookings);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ReservationDto>> call, Throwable t) {
                // Keep local
            }
        });
    }

    private Date parseAnyDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        String[] patterns = {
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf.parse(dateStr);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void updateRecentBookingsList(List<EnergyReservation> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            layoutEmptyDashboard.setVisibility(View.VISIBLE);
            rvRecentBookings.setVisibility(View.GONE);
        } else {
            layoutEmptyDashboard.setVisibility(View.GONE);
            rvRecentBookings.setVisibility(View.VISIBLE);

            // Display strictly the TOP 2 LATEST reservations created
            List<EnergyReservation> topTwoLatest = new ArrayList<>();
            int count = Math.min(2, bookings.size());
            for (int i = 0; i < count; i++) {
                topTwoLatest.add(bookings.get(i));
            }
            bookingsAdapter.setBookings(topTwoLatest);
        }
    }
}
