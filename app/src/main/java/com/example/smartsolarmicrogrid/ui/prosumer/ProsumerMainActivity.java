package com.example.smartsolarmicrogrid.ui.prosumer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.smartsolarmicrogrid.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProsumerMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prosumer_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Load initial Dashboard fragment
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment fragment = null;

            if (itemId == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (itemId == R.id.nav_map) {
                fragment = new MapStationsFragment();
            } else if (itemId == R.id.nav_bookings) {
                fragment = new BookingsFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    public void switchToMapTab() {
        bottomNavigation.setSelectedItemId(R.id.nav_map);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
