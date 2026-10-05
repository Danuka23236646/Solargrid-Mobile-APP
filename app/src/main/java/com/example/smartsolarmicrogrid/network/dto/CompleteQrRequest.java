package com.example.smartsolarmicrogrid.network.dto;

import com.google.gson.annotations.SerializedName;

public class CompleteQrRequest {
    @SerializedName("qrPayload")
    private String qrPayload;

    @SerializedName("reservationId")
    private String reservationId;

    @SerializedName("actualEnergyAmountKwh")
    private double actualEnergyAmountKwh;

    @SerializedName("completionNotes")
    private String completionNotes;

    public CompleteQrRequest() {}

    public CompleteQrRequest(String qrPayload, String reservationId, double actualEnergyAmountKwh, String completionNotes) {
        this.qrPayload = qrPayload;
        this.reservationId = reservationId;
        this.actualEnergyAmountKwh = actualEnergyAmountKwh;
        this.completionNotes = completionNotes;
    }

    public CompleteQrRequest(String qrPayload, double actualEnergyAmountKwh, String completionNotes) {
        this(qrPayload, null, actualEnergyAmountKwh, completionNotes);
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public double getActualEnergyAmountKwh() {
        return actualEnergyAmountKwh;
    }

    public void setActualEnergyAmountKwh(double actualEnergyAmountKwh) {
        this.actualEnergyAmountKwh = actualEnergyAmountKwh;
    }

    public String getCompletionNotes() {
        return completionNotes;
    }

    public void setCompletionNotes(String completionNotes) {
        this.completionNotes = completionNotes;
    }
}