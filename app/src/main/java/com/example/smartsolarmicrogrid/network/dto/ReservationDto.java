package com.example.smartsolarmicrogrid.network.dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ReservationDto implements Serializable {
    @SerializedName("_id")
    private String mongoId;

    @SerializedName("id")
    private String id;

    @SerializedName("reservationReference")
    private String reservationReference;

    @SerializedName("stationId")
    private String stationId;

    @SerializedName("stationName")
    private String stationName;

    @SerializedName("prosumerNic")
    private String prosumerNic;

    @SerializedName("userNic")
    private String userNic;

    @SerializedName("nic")
    private String nic;

    @SerializedName("bookingSlotId")
    private String bookingSlotId;

    @SerializedName("transferType")
    private String transferType; // "EnergyDropOff" or "Charging"

    @SerializedName("volume")
    private double volume;

    @SerializedName("volumeKwh")
    private double volumeKwh;

    @SerializedName("energyVolumeKwh")
    private double energyVolumeKwh;

    @SerializedName("energyAmountKwh")
    private double energyAmountKwh;

    @SerializedName("energyAmount")
    private double energyAmount;

    @SerializedName("kwh")
    private double kwh;

    @SerializedName("status")
    private String status; // "Pending", "Approved", "Completed", "Cancelled"

    @SerializedName("scheduledTime")
    private String scheduledTime;

    @SerializedName("scheduledTimeUtc")
    private String scheduledTimeUtc;

    @SerializedName("slotStartTimeUtc")
    private String slotStartTimeUtc;

    @SerializedName("startTimeUtc")
    private String startTimeUtc;

    @SerializedName("slotTime")
    private String slotTime;

    @SerializedName("bookingTime")
    private String bookingTime;

    @SerializedName("reservationTime")
    private String reservationTime;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("qrPayload")
    private String qrPayload;

    public ReservationDto() {}

    public String getId() {
        if (id != null && !id.isEmpty()) return id;
        if (mongoId != null && !mongoId.isEmpty()) return mongoId;
        if (reservationReference != null && !reservationReference.isEmpty()) return reservationReference;
        return "1";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReservationReference() {
        return reservationReference;
    }

    public void setReservationReference(String reservationReference) {
        this.reservationReference = reservationReference;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getProsumerNic() {
        if (prosumerNic != null && !prosumerNic.isEmpty()) return prosumerNic;
        if (userNic != null && !userNic.isEmpty()) return userNic;
        if (nic != null && !nic.isEmpty()) return nic;
        return null;
    }

    public void setProsumerNic(String prosumerNic) {
        this.prosumerNic = prosumerNic;
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
        if (volumeKwh > 0) return volumeKwh;
        if (volume > 0) return volume;
        if (energyVolumeKwh > 0) return energyVolumeKwh;
        if (energyAmountKwh > 0) return energyAmountKwh;
        if (energyAmount > 0) return energyAmount;
        if (kwh > 0) return kwh;
        return 0.0;
    }

    public void setEnergyAmountKwh(double energyAmountKwh) {
        this.energyAmountKwh = energyAmountKwh;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScheduledTime() {
        String raw = null;
        if (scheduledTime != null && !scheduledTime.isEmpty()) raw = scheduledTime;
        else if (slotStartTimeUtc != null && !slotStartTimeUtc.isEmpty()) raw = slotStartTimeUtc;
        else if (startTimeUtc != null && !startTimeUtc.isEmpty()) raw = startTimeUtc;
        else if (slotTime != null && !slotTime.isEmpty()) raw = slotTime;
        else if (scheduledTimeUtc != null && !scheduledTimeUtc.isEmpty()) raw = scheduledTimeUtc;
        else if (reservationTime != null && !reservationTime.isEmpty()) raw = reservationTime;
        else if (bookingTime != null && !bookingTime.isEmpty()) raw = bookingTime;
        else if (startTime != null && !startTime.isEmpty()) raw = startTime;

        if (raw == null) return null;

        // Skip UTC conversion if it's already a clean local datetime string from the API
        if (!raw.contains("T") && !raw.contains("Z")) {
            return raw;
        }

        return formatUtcToLocalTime(raw);
    }

    public static String formatUtcToLocalTime(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date date = null;
            if (raw.contains("T")) {
                String clean = raw.replaceAll("Z$", "");
                if (clean.contains(".")) {
                    clean = clean.substring(0, clean.indexOf("."));
                }
                date = isoFormat.parse(clean);
            }

            if (date != null) {
                SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                outFormat.setTimeZone(TimeZone.getDefault()); // Local time (+5:30)
                return outFormat.format(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (raw.contains("T")) {
            try {
                raw = raw.replace("T", " ");
                if (raw.contains(".")) raw = raw.substring(0, raw.indexOf("."));
                if (raw.length() >= 16) raw = raw.substring(0, 16);
            } catch (Exception ignored) {}
        }
        return raw;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }
}
