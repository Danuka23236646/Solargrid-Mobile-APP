package com.example.smartsolarmicrogrid.network.dto;

public class QrVerifyRequest {
    private String qrData;
    private int operatorId;

    public QrVerifyRequest() {}

    public QrVerifyRequest(String qrData, int operatorId) {
        this.qrData = qrData;
        this.operatorId = operatorId;
    }

    public String getQrData() {
        return qrData;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
    }

    public int getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(int operatorId) {
        this.operatorId = operatorId;
    }
}
