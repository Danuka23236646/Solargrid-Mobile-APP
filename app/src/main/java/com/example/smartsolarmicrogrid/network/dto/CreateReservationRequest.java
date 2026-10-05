package com.example.smartsolarmicrogrid.network.dto;

import com.google.gson.annotations.SerializedName;

public class CreateReservationRequest {
    @SerializedName("stationId")
    private String stationId;

    @SerializedName("bookingSlotId")
    private String bookingSlotId;

    @SerializedName("transferType")
    private String transferType; // "EnergyDropOff" or "Charging"

    @SerializedName("energyAmountKwh")
    private double energyAmountKwh;

    @SerializedName("scheduledTime")
    private String scheduledTime;

    @SerializedName("notes")
    private String notes;

    public CreateReservationRequest() {}

    public CreateReservationRequest(String stationId, String bookingSlotId, String transferType, double energyAmountKwh, String scheduledTime, String notes) {
        this.stationId = stationId;
        this.bookingSlotId = bookingSlotId;
        this.transferType = transferType;
        this.energyAmountKwh = energyAmountKwh;
        this.scheduledTime = scheduledTime;
        this.notes = notes;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getBookingSlotId() {
        return bookingSlotId;
    }

    public void setBookingSlotId(String bookingSlotId) {
        this.bookingSlotId = bookingSlotId;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public double getEnergyAmountKwh() {
        return energyAmountKwh;
    }

    public void setEnergyAmountKwh(double energyAmountKwh) {
        this.energyAmountKwh = energyAmountKwh;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
