package com.example.smartsolarmicrogrid.models;

import java.io.Serializable;

/**
 * Model class representing a User in the Smart Solar Microgrid system.
 * NIC serves as the primary unique identifier for Prosumers.
 */
public class User implements Serializable {
    private int id;
    private String nic;
    private String name;
    private String email;
    private String role; // "PROSUMER" or "OPERATOR"
    private String token;
    private String status; // "Active", "Pending Deactivation", "Deactivated"
    private String password;
    private String phone;

    public User() {}

    public User(int id, String nic, String name, String email, String role, String token, String status) {
        this.id = id;
        this.nic = nic;
        this.name = name;
        this.email = email;
        this.role = role;
        this.token = token;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
