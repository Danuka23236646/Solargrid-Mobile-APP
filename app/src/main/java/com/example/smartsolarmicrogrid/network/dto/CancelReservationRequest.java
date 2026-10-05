package com.example.smartsolarmicrogrid.network.dto;

import com.google.gson.annotations.SerializedName;

public class CancelReservationRequest {
    @SerializedName("cancellationReason")
    private String cancellationReason;

    public CancelReservationRequest() {}

    public CancelReservationRequest(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
