package com.example.smartsolarmicrogrid.ui.prosumer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

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

public class BookingsFragment extends Fragment {

    private RecyclerView rvAllBookings;
    private Button btnNewBooking, btnEmptyNewBooking;
    private LinearLayout layoutEmptyBookings;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private BookingsAdapter bookingsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookings, container, false);

        if (getContext() != null) {
            dbHelper = DatabaseHelper.getInstance(getContext());
            sessionManager = new SessionManager(getContext());
        }

        rvAllBookings = view.findViewById(R.id.rvAllBookings);
        btnNewBooking = view.findViewById(R.id.btnNewBooking);
        btnEmptyNewBooking = view.findViewById(R.id.btnEmptyNewBooking);
        layoutEmptyBookings = view.findViewById(R.id.layoutEmptyBookings);

        rvAllBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        bookingsAdapter = new BookingsAdapter(booking -> {
            Intent intent = new Intent(getActivity(), BookingDetailsActivity.class);
            intent.putExtra("BOOKING_ID", booking.getId());
            startActivity(intent);
        });
        rvAllBookings.setAdapter(bookingsAdapter);

        View.OnClickListener openCreateBooking = v -> {
            Intent intent = new Intent(getActivity(), CreateBookingActivity.class);
            startActivity(intent);
        };

        btnNewBooking.setOnClickListener(openCreateBooking);
        if (btnEmptyNewBooking != null) {
            btnEmptyNewBooking.setOnClickListener(openCreateBooking);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBookings();
        fetchLiveHistoryFromApi();
    }

    private void loadBookings() {
        if (dbHelper == null || sessionManager == null) return;
        List<EnergyReservation> list = dbHelper.getAllBookings();
        updateBookingsUI(list);
    }

    private void fetchLiveHistoryFromApi() {
        if (getContext() == null || sessionManager == null) return;

        ApiClient.getApiService(getContext()).getMyReservationHistory().enqueue(new Callback<List<ReservationDto>>() {
            @Override
            public void onResponse(Call<List<ReservationDto>> call, Response<List<ReservationDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ReservationDto> dtos = response.body();
                    if (!dtos.isEmpty()) {
                        dbHelper.clearBookingsCache(); // Clear stale local cache
                    }

                    List<EnergyReservation> apiBookings = new ArrayList<>();
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
                                dto.getStationName() != null ? dto.getStationName() : "Station " + dto.getStationId(),
                                time,
                                kwh,
                                dto.getTransferType() != null ? dto.getTransferType() : "EnergyDropOff",
                                dto.getStatus() != null ? dto.getStatus() : "CONFIRMED",
                                dto.getQrPayload() != null ? dto.getQrPayload() : "SUNGRID:RESERVATION:" + bookingId
                        );

                        dbHelper.insertBooking(b);
                        apiBookings.add(b);
                    }

                    if (!apiBookings.isEmpty()) {
                        // Robust multi-format date parser sorting (Newest scheduled date first)
                        Collections.sort(apiBookings, (b1, b2) -> {
                            Date d1 = parseAnyDate(b1.getScheduledTime());
                            Date d2 = parseAnyDate(b2.getScheduledTime());
                            if (d1 != null && d2 != null) {
                                return d2.compareTo(d1); // Newest scheduled date first
                            }
                            return 0;
                        });

                        updateBookingsUI(apiBookings);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ReservationDto>> call, Throwable t) {
                // Fallback local SQLite
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

    private void updateBookingsUI(List<EnergyReservation> list) {
        if (list == null || list.isEmpty()) {
            layoutEmptyBookings.setVisibility(View.VISIBLE);
            rvAllBookings.setVisibility(View.GONE);
        } else {
            layoutEmptyBookings.setVisibility(View.GONE);
            rvAllBookings.setVisibility(View.VISIBLE);
            bookingsAdapter.setBookings(list);
        }
    }
}
