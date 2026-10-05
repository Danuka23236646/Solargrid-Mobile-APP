package com.example.smartsolarmicrogrid.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.models.EnergyReservation;

import java.util.ArrayList;
import java.util.List;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.BookingViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(EnergyReservation booking);
    }

    private List<EnergyReservation> bookingsList = new ArrayList<>();
    private OnBookingClickListener clickListener;

    public BookingsAdapter(OnBookingClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setBookings(List<EnergyReservation> bookings) {
        this.bookingsList = bookings != null ? bookings : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        EnergyReservation booking = bookingsList.get(position);
        holder.tvBookingId.setText("Booking #" + booking.getId());
        holder.tvStationName.setText(booking.getNodeName() != null ? booking.getNodeName() : "Station #" + booking.getNodeId());
        holder.tvScheduledTime.setText("📅 " + booking.getScheduledTime());
        holder.tvEnergyKwh.setText("⚡ " + booking.getKwh() + " kWh");

        String rawType = booking.getType();
        String displayType = "SELL";
        if ("Charging".equalsIgnoreCase(rawType) || "BUY".equalsIgnoreCase(rawType)) {
            displayType = "BUY";
        } else if ("EnergyDropOff".equalsIgnoreCase(rawType) || "SELL".equalsIgnoreCase(rawType)) {
            displayType = "SELL";
        } else if (rawType != null) {
            displayType = rawType;
        }

        holder.tvTransferType.setText("Type: " + displayType);

        String rawStatus = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "PENDING";
        String status = rawStatus;

        if (rawStatus.contains("PENDING")) {
            status = "PENDING";
            holder.tvBookingStatus.setBackgroundColor(Color.parseColor("#EF6C00")); // Orange
        } else if ("APPROVED".equals(rawStatus) || "CONFIRMED".equals(rawStatus)) {
            status = "APPROVED";
            holder.tvBookingStatus.setBackgroundColor(Color.parseColor("#1976D2")); // Blue
        } else if ("COMPLETED".equals(rawStatus)) {
            status = "COMPLETED";
            holder.tvBookingStatus.setBackgroundColor(Color.parseColor("#388E3C")); // Green
        } else if ("CANCELLED".equals(rawStatus)) {
            status = "CANCELLED";
            holder.tvBookingStatus.setBackgroundColor(Color.parseColor("#D32F2F")); // Red
        } else {
            status = "PENDING";
            holder.tvBookingStatus.setBackgroundColor(Color.parseColor("#EF6C00")); // Orange default
        }

        holder.tvBookingStatus.setText(status);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onBookingClick(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookingsList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId, tvBookingStatus, tvStationName, tvScheduledTime, tvEnergyKwh, tvTransferType;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvBookingStatus = itemView.findViewById(R.id.tvBookingStatus);
            tvStationName = itemView.findViewById(R.id.tvStationName);
            tvScheduledTime = itemView.findViewById(R.id.tvScheduledTime);
            tvEnergyKwh = itemView.findViewById(R.id.tvEnergyKwh);
            tvTransferType = itemView.findViewById(R.id.tvTransferType);
        }
    }
}
