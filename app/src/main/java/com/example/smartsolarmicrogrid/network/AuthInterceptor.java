package com.example.smartsolarmicrogrid.network;

import android.content.Context;
import android.text.TextUtils;

import com.example.smartsolarmicrogrid.util.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor that automatically attaches 'Authorization: Bearer <JWT_TOKEN>'
 * and 'ngrok-skip-browser-warning: true' to outgoing HTTP requests.
 */
public class AuthInterceptor implements Interceptor {

    private final SessionManager sessionManager;

    public AuthInterceptor(Context context) {
        this.sessionManager = new SessionManager(context.getApplicationContext());
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String token = sessionManager.getAuthToken();

        Request.Builder builder = originalRequest.newBuilder()
                .header("ngrok-skip-browser-warning", "true");

        if (!TextUtils.isEmpty(token)) {
            builder.header("Authorization", "Bearer " + token);
        }

        return chain.proceed(builder.build());
    }
}
