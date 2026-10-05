package com.example.smartsolarmicrogrid.network.dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class DashboardSummaryDto implements Serializable {
    @SerializedName("totalActive")
    private int totalActive;

    @SerializedName("activeBookings")
    private int activeBookings;

    @SerializedName("activeCount")
    private int activeCount;

    @SerializedName("pendingCount")
    private int pendingCount;

    @SerializedName("approvedCount")
    private int approvedCount;

    @SerializedName("completedCount")
    private int completedCount;

    @SerializedName("totalEnergyKwhTraded")
    private double totalEnergyKwhTraded;

    @SerializedName("totalEnergyTraded")
    private double totalEnergyTraded;

    @SerializedName("totalEnergyKwh")
    private double totalEnergyKwh;

    @SerializedName("totalEnergySoldKwh")
    private double totalEnergySoldKwh;

    @SerializedName("totalEnergyBoughtKwh")
    private double totalEnergyBoughtKwh;

    @SerializedName("netEnergyKwh")
    private double netEnergyKwh;

    public DashboardSummaryDto() {}

    public int getTotalActive() {
        if (totalActive > 0) return totalActive;
        if (activeBookings > 0) return activeBookings;
        if (activeCount > 0) return activeCount;
        return 0;
    }

    public void setTotalActive(int totalActive) {
        this.totalActive = totalActive;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public int getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(int approvedCount) {
        this.approvedCount = approvedCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public double getTotalEnergyKwhTraded() {
        if (totalEnergyKwhTraded > 0) return totalEnergyKwhTraded;
        if (totalEnergyTraded > 0) return totalEnergyTraded;
        if (totalEnergyKwh > 0) return totalEnergyKwh;
        return 0.0;
    }

    public void setTotalEnergyKwhTraded(double totalEnergyKwhTraded) {
        this.totalEnergyKwhTraded = totalEnergyKwhTraded;
    }

    public double getTotalEnergySoldKwh() {
        return totalEnergySoldKwh;
    }

    public void setTotalEnergySoldKwh(double totalEnergySoldKwh) {
        this.totalEnergySoldKwh = totalEnergySoldKwh;
    }

    public double getTotalEnergyBoughtKwh() {
        return totalEnergyBoughtKwh;
    }

    public void setTotalEnergyBoughtKwh(double totalEnergyBoughtKwh) {
        this.totalEnergyBoughtKwh = totalEnergyBoughtKwh;
    }

    public double getNetEnergyKwh() {
        return netEnergyKwh;
    }

    public void setNetEnergyKwh(double netEnergyKwh) {
        this.netEnergyKwh = netEnergyKwh;
    }
}
