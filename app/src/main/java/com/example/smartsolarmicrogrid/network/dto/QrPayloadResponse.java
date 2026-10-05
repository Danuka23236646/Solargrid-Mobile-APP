package com.example.smartsolarmicrogrid.network.dto;

import com.google.gson.annotations.SerializedName;

public class QrPayloadResponse {
    @SerializedName("reservationId")
    private String reservationId;

    @SerializedName("reservationReference")
    private String reservationReference;

    @SerializedName("qrPayload")
    private String qrPayload;

    @SerializedName("expiresAtUtc")
    private String expiresAtUtc;

    public QrPayloadResponse() {}

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getReservationReference() {
        return reservationReference;
    }

    public void setReservationReference(String reservationReference) {
        this.reservationReference = reservationReference;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    public String getExpiresAtUtc() {
        return expiresAtUtc;
    }

    public void setExpiresAtUtc(String expiresAtUtc) {
        this.expiresAtUtc = expiresAtUtc;
    }
}
