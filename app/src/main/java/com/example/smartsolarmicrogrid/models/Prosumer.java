package com.example.smartsolarmicrogrid.models;

import java.io.Serializable;

/**
 * Extension model representing a Prosumer account.
 */
public class Prosumer extends User implements Serializable {
    private String meterId;
    private double currentBalanceKw;
    private String address;

    public Prosumer() {
        super();
    }

    public Prosumer(int id, String nic, String name, String email, String role, String token, String status, String meterId, double currentBalanceKw, String address) {
        super(id, nic, name, email, role, token, status);
        this.meterId = meterId;
        this.currentBalanceKw = currentBalanceKw;
        this.address = address;
    }

    public String getMeterId() {
        return meterId;
    }

    public void setMeterId(String meterId) {
        this.meterId = meterId;
    }

    public double getCurrentBalanceKw() {
        return currentBalanceKw;
    }

    public void setCurrentBalanceKw(double currentBalanceKw) {
        this.currentBalanceKw = currentBalanceKw;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
