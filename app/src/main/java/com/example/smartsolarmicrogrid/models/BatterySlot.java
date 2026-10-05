package com.example.smartsolarmicrogrid.models;

import java.io.Serializable;

/**
 * Model class representing a Battery Storage Slot within a Solar Station node.
 */
public class BatterySlot implements Serializable {
    private int id;
    private int stationId;
    private int slotNumber;
    private String status; // "Available", "Charging", "In-Use"
    private double capacityKw;

    public BatterySlot() {}

    public BatterySlot(int id, int stationId, int slotNumber, String status, double capacityKw) {
        this.id = id;
        this.stationId = stationId;
        this.slotNumber = slotNumber;
        this.status = status;
        this.capacityKw = capacityKw;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStationId() {
        return stationId;
    }

    public void setStationId(int stationId) {
        this.stationId = stationId;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCapacityKw() {
        return capacityKw;
    }

    public void setCapacityKw(double capacityKw) {
        this.capacityKw = capacityKw;
    }
}
