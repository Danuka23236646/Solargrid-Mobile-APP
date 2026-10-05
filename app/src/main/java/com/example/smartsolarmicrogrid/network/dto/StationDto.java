package com.example.smartsolarmicrogrid.network.dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class StationDto implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("stationCode")
    private String stationCode;

    @SerializedName("address")
    private String address;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("availableSlots")
    private int availableSlots;

    @SerializedName("totalSlots")
    private int totalSlots;

    @SerializedName("capacityKwh")
    private double capacityKwh;

    @SerializedName("receivedEnergyKwh")
    private double receivedEnergyKwh;

    @SerializedName("dispatchedEnergyKwh")
    private double dispatchedEnergyKwh;

    @SerializedName("currentStoredEnergyKwh")
    private Double currentStoredEnergyKwh;

    @SerializedName("pendingIntakeKwh")
    private Double pendingIntakeKwh;

    @SerializedName("availableIntakeKwh")
    private Double availableIntakeKwh;

    @SerializedName("batteryStoragePercentage")
    private Double batteryStoragePercentage;

    @SerializedName("isOutOfStorage")
    private boolean isOutOfStorage;

    @SerializedName("canReceiveEnergy")
    private boolean canReceiveEnergy;

    @SerializedName("status")
    private String status;

    public StationDto() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }

    public double getCapacityKwh() {
        return capacityKwh;
    }

    public void setCapacityKwh(double capacityKwh) {
        this.capacityKwh = capacityKwh;
    }

    public double getReceivedEnergyKwh() {
        return receivedEnergyKwh;
    }

    public void setReceivedEnergyKwh(double receivedEnergyKwh) {
        this.receivedEnergyKwh = receivedEnergyKwh;
    }

    public double getDispatchedEnergyKwh() {
        return dispatchedEnergyKwh;
    }

    public void setDispatchedEnergyKwh(double dispatchedEnergyKwh) {
        this.dispatchedEnergyKwh = dispatchedEnergyKwh;
    }

    public Double getCurrentStoredEnergyKwh() {
        return currentStoredEnergyKwh;
    }

    public void setCurrentStoredEnergyKwh(Double currentStoredEnergyKwh) {
        this.currentStoredEnergyKwh = currentStoredEnergyKwh;
    }

    public Double getPendingIntakeKwh() {
        return pendingIntakeKwh;
    }

    public void setPendingIntakeKwh(Double pendingIntakeKwh) {
        this.pendingIntakeKwh = pendingIntakeKwh;
    }

    public Double getAvailableIntakeKwh() {
        return availableIntakeKwh;
    }

    public void setAvailableIntakeKwh(Double availableIntakeKwh) {
        this.availableIntakeKwh = availableIntakeKwh;
    }

    public Double getBatteryStoragePercentage() {
        return batteryStoragePercentage;
    }

    public void setBatteryStoragePercentage(Double batteryStoragePercentage) {
        this.batteryStoragePercentage = batteryStoragePercentage;
    }

    public boolean isOutOfStorage() {
        if (isOutOfStorage) return true;
        if (availableSlots <= 0) return true;
        if (availableIntakeKwh != null && availableIntakeKwh <= 0.0) return true;
        return false;
    }

    public void setOutOfStorage(boolean outOfStorage) {
        this.isOutOfStorage = outOfStorage;
    }

    public boolean isCanReceiveEnergy() {
        return canReceiveEnergy && !isOutOfStorage();
    }

    public void setCanReceiveEnergy(boolean canReceiveEnergy) {
        this.canReceiveEnergy = canReceiveEnergy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}