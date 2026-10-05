package com.example.smartsolarmicrogrid.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.smartsolarmicrogrid.models.User;

public class SessionManager {
    private static final String PREF_NAME = "SmartSolarSession";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_NIC = "user_nic";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_STATUS = "user_status";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, "");
    }

    public void createSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        if (user.getToken() != null) {
            editor.putString(KEY_AUTH_TOKEN, user.getToken());
        }

        if (user.getNic() != null) {
            editor.putString(KEY_USER_NIC, user.getNic());
        }
        if (user.getName() != null) {
            editor.putString(KEY_USER_NAME, user.getName());
        }
        if (user.getEmail() != null) {
            editor.putString(KEY_USER_EMAIL, user.getEmail());
        }
        if (user.getRole() != null) {
            editor.putString(KEY_USER_ROLE, user.getRole());
        }
        if (user.getStatus() != null) {
            editor.putString(KEY_USER_STATUS, user.getStatus());
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserNic() {
        return prefs.getString(KEY_USER_NIC, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "Prosumer");
    }

    public String getUserStatus() {
        return prefs.getString(KEY_USER_STATUS, "Active");
    }

    public void setUserStatus(String status) {
        editor.putString(KEY_USER_STATUS, status);
        editor.apply();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
