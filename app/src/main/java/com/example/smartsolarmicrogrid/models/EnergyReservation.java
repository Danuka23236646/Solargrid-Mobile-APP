package com.example.smartsolarmicrogrid.models;

import java.io.Serializable;

/**
 * Model class representing an Energy Reservation / Booking.
 */
public class EnergyReservation implements Serializable {
    private int id;
    private String prosumerNic;
    private int nodeId;
    private String nodeName;
    private String scheduledTime; // ISO format string or "yyyy-MM-dd HH:mm"
    private double kwh;
    private String type; // "SELL", "BUY", "TRANSFER"
    private String status; // "CONFIRMED", "COMPLETED", "CANCELLED"
    private String qrData;

    public EnergyReservation() {}

    public EnergyReservation(int id, String prosumerNic, int nodeId, String nodeName, String scheduledTime, double kwh, String type, String status, String qrData) {
        this.id = id;
        this.prosumerNic = prosumerNic;
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.scheduledTime = scheduledTime;
        this.kwh = kwh;
        this.type = type;
        this.status = status;
        this.qrData = qrData;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProsumerNic() {
        return prosumerNic;
    }

    public void setProsumerNic(String prosumerNic) {
        this.prosumerNic = prosumerNic;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public double getKwh() {
        return kwh;
    }

    public void setKwh(double kwh) {
        this.kwh = kwh;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQrData() {
        return qrData;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
    }
}
