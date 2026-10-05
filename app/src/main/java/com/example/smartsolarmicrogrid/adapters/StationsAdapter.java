package com.example.smartsolarmicrogrid.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.models.SolarStation;

import java.util.ArrayList;
import java.util.List;

public class StationsAdapter extends RecyclerView.Adapter<StationsAdapter.StationViewHolder> {

    public interface OnStationClickListener {
        void onStationClick(SolarStation station);
    }

    private List<SolarStation> stationList = new ArrayList<>();
    private OnStationClickListener clickListener;

    public StationsAdapter(OnStationClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setStations(List<SolarStation> stations) {
        this.stationList = stations != null ? stations : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        SolarStation station = stationList.get(position);
        holder.tvStationName.setText(station.getName());
        holder.tvStationAddress.setText("📍 " + station.getAddress());
        holder.tvCapacity.setText("Capacity: " + station.getCapacityKw() + " kW");
        holder.tvAvailableSlots.setText("Slots: " + station.getAvailableSlots() + " Available");

        holder.btnBookStation.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onStationClick(station);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onStationClick(station);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stationList.size();
    }

    static class StationViewHolder extends RecyclerView.ViewHolder {
        TextView tvStationName, tvStationAddress, tvCapacity, tvAvailableSlots;
        Button btnBookStation;

        public StationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStationName = itemView.findViewById(R.id.tvStationName);
            tvStationAddress = itemView.findViewById(R.id.tvStationAddress);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvAvailableSlots = itemView.findViewById(R.id.tvAvailableSlots);
            btnBookStation = itemView.findViewById(R.id.btnBookStation);
        }
    }
}
