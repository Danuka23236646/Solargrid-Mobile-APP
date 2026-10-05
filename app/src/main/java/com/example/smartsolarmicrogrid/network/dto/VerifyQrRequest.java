package com.example.smartsolarmicrogrid.network.dto;

import com.google.gson.annotations.SerializedName;

public class VerifyQrRequest {
    @SerializedName("qrPayload")
    private String qrPayload;

    public VerifyQrRequest() {}

    public VerifyQrRequest(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }
}
