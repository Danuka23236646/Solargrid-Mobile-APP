package com.example.smartsolarmicrogrid.ui.prosumer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smartsolarmicrogrid.R;
import com.example.smartsolarmicrogrid.database.DatabaseHelper;
import com.example.smartsolarmicrogrid.models.SolarStation;
import com.example.smartsolarmicrogrid.network.ApiClient;
import com.example.smartsolarmicrogrid.network.dto.StationDto;
import com.example.smartsolarmicrogrid.ui.prosumer.booking.CreateBookingActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapStationsFragment extends Fragment {

    private static final LatLng COLOMBO_CENTER = new LatLng(6.9271, 79.8612);
    private static final float INITIAL_ZOOM = 8.5f;
    private static final float LOCATION_ZOOM = 11.0f;
    private static final double NEARBY_RADIUS_KM = 50.0;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseHelper dbHelper;
    private Call<List<StationDto>> stationRequest;
    private boolean viewActive;

    private CardView cardStationDetails;
    private TextView tvMapStationName;
    private TextView tvMapStationAddress;
    private TextView tvMapStationCapacity;
    private TextView tvMapStationSlots;
    private Button btnMapBookNow;

    private SolarStation selectedStation;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = DatabaseHelper.getInstance(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_stations, container, false);
        viewActive = true;

        cardStationDetails = view.findViewById(R.id.cardStationDetails);
        tvMapStationName = view.findViewById(R.id.tvMapStationName);
        tvMapStationAddress = view.findViewById(R.id.tvMapStationAddress);
        tvMapStationCapacity = view.findViewById(R.id.tvMapStationCapacity);
        tvMapStationSlots = view.findViewById(R.id.tvMapStationSlots);
        btnMapBookNow = view.findViewById(R.id.btnMapBookNow);

        cardStationDetails.setVisibility(View.GONE);

        btnMapBookNow.setOnClickListener(v -> {
            if (selectedStation != null && isViewActive()) {
                Intent intent = new Intent(requireContext(), CreateBookingActivity.class);
                intent.putExtra("STATION_ID", selectedStation.getId());
                intent.putExtra("STATION_NAME", selectedStation.getName());
                startActivity(intent);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.stationMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this::onMapReady);
        } else {
            Toast.makeText(requireContext(), "Unable to initialize the stations map.", Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private void onMapReady(@NonNull GoogleMap map) {
        if (!isViewActive()) return;

        googleMap = map;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(COLOMBO_CENTER, INITIAL_ZOOM));
        googleMap.setOnMarkerClickListener(this::onStationMarkerClicked);
        loadStationsUsingDeviceLocation();
    }

    private boolean onStationMarkerClicked(@NonNull Marker marker) {
        Object tag = marker.getTag();
        if (tag instanceof SolarStation && isViewActive()) {
            displayStationDetails((SolarStation) tag);
            marker.showInfoWindow();
            return true;
        }
        return false;
    }

    private void loadStationsUsingDeviceLocation() {
        if (!isViewActive() || googleMap == null) return;

        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        enableMyLocationLayer();
        requestLastLocation();
    }

    private void onLocationPermissionResult(Map<String, Boolean> permissions) {
        if (!isViewActive() || googleMap == null) return;

        boolean fineGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION));
        boolean coarseGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION));
        if (fineGranted || coarseGranted) {
            enableMyLocationLayer();
            requestLastLocation();
        } else {
            showMessage("Location permission denied. Loading all active stations.");
            loadActiveStations();
        }
    }

    private boolean hasLocationPermission() {
        if (getContext() == null) return false;
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void enableMyLocationLayer() {
        if (googleMap == null || !hasLocationPermission()) return;
        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException ignored) {
            showMessage("The map could not enable the device location layer.");
        }
    }

    private void requestLastLocation() {
        if (!hasLocationPermission()) {
            loadActiveStations();
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (!isViewActive() || googleMap == null) return;
                        if (location != null) {
                            loadNearbyStations(location);
                        } else {
                            showMessage("Device location is unavailable. Loading all active stations.");
                            loadActiveStations();
                        }
                    })
                    .addOnFailureListener(error -> {
                        if (!isViewActive()) return;
                        showMessage("Could not get the device location. Loading all active stations.");
                        loadActiveStations();
                    });
        } catch (SecurityException ignored) {
            showMessage("Location access is unavailable. Loading all active stations.");
            loadActiveStations();
        }
    }

    private void loadNearbyStations(@NonNull Location location) {
        if (!isViewActive() || googleMap == null) return;

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), LOCATION_ZOOM));

        enqueueStationRequest(
                ApiClient.getApiService(requireContext()).getNearbyStations(
                        location.getLatitude(), location.getLongitude(), NEARBY_RADIUS_KM),
                "No nearby stations were found.",
                true,
                false
        );
    }

    private void loadActiveStations() {
        if (!isViewActive()) return;
        enqueueStationRequest(
                ApiClient.getApiService(requireContext()).getActiveStations(),
                "No active stations were found.",
                false,
                true
        );
    }

    private void enqueueStationRequest(@NonNull Call<List<StationDto>> request,
                                       @NonNull String emptyMessage,
                                       boolean retryWithActiveStations,
                                       boolean cacheResponse) {
        if (stationRequest != null) {
            stationRequest.cancel();
        }
        stationRequest = request;
        request.enqueue(new Callback<List<StationDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<StationDto>> call,
                                   @NonNull Response<List<StationDto>> response) {
                if (call.isCanceled() || !isViewActive() || call != stationRequest) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<SolarStation> stations = toSolarStations(response.body());
                    if (stations.isEmpty()) {
                        displayStations(Collections.emptyList());
                        showMessage(emptyMessage);
                    } else {
                        if (cacheResponse) {
                            dbHelper.saveStations(stations);
                        }
                        displayStations(stations);
                    }
                } else {
                    handleStationRequestFailure(retryWithActiveStations);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StationDto>> call, @NonNull Throwable error) {
                if (call.isCanceled() || !isViewActive() || call != stationRequest) return;
                handleStationRequestFailure(retryWithActiveStations);
            }
        });
    }

    private void handleStationRequestFailure(boolean retryWithActiveStations) {
        if (retryWithActiveStations) {
            showMessage("Nearby stations could not be loaded. Loading all active stations.");
            loadActiveStations();
        } else {
            showCachedStations();
        }
    }

    @NonNull
    private List<SolarStation> toSolarStations(@NonNull List<StationDto> stationDtos) {
        List<SolarStation> stations = new ArrayList<>();
        int fallbackId = 1;
        for (StationDto dto : stationDtos) {
            if (dto == null) continue;

            int stationId = fallbackId++;
            try {
                if (dto.getId() != null && dto.getId().matches("\\d+")) {
                    stationId = Integer.parseInt(dto.getId());
                } else if (dto.getId() != null) {
                    stationId = Math.abs(dto.getId().hashCode());
                }
            } catch (NumberFormatException ignored) {
                // Keep the deterministic list fallback used by the existing booking flow.
            }

            SolarStation station = new SolarStation(
                    stationId,
                    dto.getName(),
                    dto.getAddress(),
                    dto.getLatitude(),
                    dto.getLongitude(),
                    dto.getCapacityKwh(),
                    dto.getAvailableSlots()
            );
            if (dto.getId() != null) {
                station.setStringId(dto.getId());
            }
            if (dto.getCurrentStoredEnergyKwh() != null) {
                station.setCurrentStoredEnergyKwh(dto.getCurrentStoredEnergyKwh());
            }
            if (dto.getAvailableIntakeKwh() != null) {
                station.setAvailableIntakeKwh(dto.getAvailableIntakeKwh());
            }
            if (dto.getBatteryStoragePercentage() != null) {
                station.setBatteryStoragePercentage(dto.getBatteryStoragePercentage());
            }
            station.setOutOfStorage(dto.isOutOfStorage());
            stations.add(station);
        }
        return stations;
    }

    private void showCachedStations() {
        if (!isViewActive()) return;

        List<SolarStation> cachedStations = dbHelper.getAllStations();
        if (cachedStations.isEmpty()) {
            displayStations(Collections.emptyList());
            showMessage("Live stations could not be loaded, and no cached station data is available.");
        } else {
            displayStations(cachedStations);
            showMessage("Live stations could not be loaded. Showing cached station data.");
        }
    }

    private void displayStations(@NonNull List<SolarStation> stations) {
        if (!isViewActive() || googleMap == null) return;

        googleMap.clear();
        selectedStation = null;
        cardStationDetails.setVisibility(View.GONE);

        for (SolarStation station : stations) {
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(station.getLatitude(), station.getLongitude()))
                    .title(station.getName())
                    .snippet(station.getAddress()));
            if (marker != null) {
                marker.setTag(station);
            }
        }
    }

    private void displayStationDetails(@NonNull SolarStation station) {
        if (!isViewActive()) return;
        selectedStation = station;
        tvMapStationName.setText(station.getName());
        tvMapStationAddress.setText("📍 " + station.getAddress());
        tvMapStationCapacity.setText("Capacity: " + station.getCapacityKw() + " kW");
        tvMapStationSlots.setText("Slots: " + station.getAvailableSlots() + " Available");
        cardStationDetails.setVisibility(View.VISIBLE);
    }

    private void showMessage(@NonNull String message) {
        if (isViewActive()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isViewActive() {
        return viewActive && isAdded() && getView() != null;
    }

    @Override
    public void onDestroyView() {
        viewActive = false;
        if (stationRequest != null) {
            stationRequest.cancel();
            stationRequest = null;
        }
        if (googleMap != null) {
            googleMap.setOnMarkerClickListener(null);
            googleMap = null;
        }

        selectedStation = null;
        cardStationDetails = null;
        tvMapStationName = null;
        tvMapStationAddress = null;
        tvMapStationCapacity = null;
        tvMapStationSlots = null;
        btnMapBookNow = null;
        super.onDestroyView();
    }
}
