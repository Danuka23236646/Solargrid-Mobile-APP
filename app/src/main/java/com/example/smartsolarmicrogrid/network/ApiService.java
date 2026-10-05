package com.example.smartsolarmicrogrid.network;

import com.example.smartsolarmicrogrid.network.dto.ApiResponse;
import com.example.smartsolarmicrogrid.network.dto.CancelReservationRequest;
import com.example.smartsolarmicrogrid.network.dto.CompleteQrRequest;
import com.example.smartsolarmicrogrid.network.dto.CreateReservationRequest;
import com.example.smartsolarmicrogrid.network.dto.DashboardSummaryDto;
import com.example.smartsolarmicrogrid.network.dto.LoginRequest;
import com.example.smartsolarmicrogrid.network.dto.LoginResponse;
import com.example.smartsolarmicrogrid.network.dto.QrPayloadResponse;
import com.example.smartsolarmicrogrid.network.dto.RegisterProsumerRequest;
import com.example.smartsolarmicrogrid.network.dto.ReservationDto;
import com.example.smartsolarmicrogrid.network.dto.SlotDto;
import com.example.smartsolarmicrogrid.network.dto.StationDto;
import com.example.smartsolarmicrogrid.network.dto.UserDto;
import com.example.smartsolarmicrogrid.network.dto.VerifyQrRequest;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/register/prosumer")
    Call<LoginResponse> registerProsumer(@Body RegisterProsumerRequest request);

    @GET("reservations/me/dashboard")
    Call<DashboardSummaryDto> getMyDashboardSummary();

    @GET("reservations/dashboard/summary")
    Call<DashboardSummaryDto> getDashboardSummary();

    @GET("reservations/me")
    Call<List<ReservationDto>> getMyReservationsList(@Query("pageSize") int pageSize);

    @GET("reservations/me/history")
    Call<List<ReservationDto>> getMyReservationHistory();

    @GET("stations/active")
    Call<List<StationDto>> getActiveStations();

    @GET("stations/nearby")
    Call<List<StationDto>> getNearbyStations(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("radiusKm") double radiusKm
    );

    @GET("stations/{stationId}/slots")
    Call<List<SlotDto>> getStationSlots(
            @Path("stationId") String stationId,
            @Query("includePast") boolean includePast
    );

    @POST("reservations")
    Call<ReservationDto> createReservation(@Body CreateReservationRequest request);

    @PUT("reservations/{id}")
    Call<ReservationDto> updateReservation(@Path("id") String id, @Body CreateReservationRequest request);

    @GET("reservations/{id}")
    Call<ReservationDto> getReservationDetails(@Path("id") String id);

    @POST("reservations/{id}/qr")
    Call<QrPayloadResponse> generateQrPayload(@Path("id") String id);

    @POST("reservations/{id}/cancel")
    Call<ReservationDto> cancelReservation(@Path("id") String id, @Body CancelReservationRequest request);

    @GET("users/me")
    Call<UserDto> getMyUserProfile();

    @PUT("users/me")
    Call<UserDto> updateMyUserProfile(@Body UserDto userDto);

    @POST("qr/verify")
    Call<ResponseBody> verifyQrPayload(@Body VerifyQrRequest request);

    @POST("qr/complete")
    Call<ResponseBody> completeQrTransfer(@Body CompleteQrRequest request);
}