package me.samarthh.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class SioseApiClient {
    private static final Logger logger = LoggerFactory.getLogger(SioseApiClient.class);
    private final String baseUrl;
    private final OkHttpClient client;
    private final Gson gson;

    public SioseApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    /**
     * Register a new user
     * @param uuid Player's UUID
     * @param username Player's username
     * @return CompletableFuture with registration response
     */
    public CompletableFuture<RegistrationResponse> registerUser(String uuid, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("uuid", uuid);
                json.addProperty("username", username);

                RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
                Request request = new Request.Builder()
                        .url(this.baseUrl + "/register")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        return gson.fromJson(responseBody, RegistrationResponse.class);
                    } else {
                        logger.warn("Registration failed with code: {}", response.code());
                        throw new ApiException("Registration failed: " + response.code());
                    }
                }
                } catch (IOException e) {
                    logger.error("Network error during registration: {}", e.getMessage());
                    throw new ApiException("Network error during registration: " + e.getMessage());
                }
        });
    }

    /**
     * Validate and get user token
     * @param uuid Player's UUID
     * @param token Authentication token
     * @return CompletableFuture with validation response
     */
    public CompletableFuture<AuthResponse> validateToken(String uuid, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("uuid", uuid);
                json.addProperty("token", token);

                RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
                Request request = new Request.Builder()
                        .url(this.baseUrl + "/validate")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        return gson.fromJson(responseBody, AuthResponse.class);
                    } else {
                        logger.warn("Token validation failed with code: {}", response.code());
                        throw new ApiException("Token validation failed: " + response.code());
                    }
                }
            } catch (IOException e) {
                logger.error("Network error during registration: {}", e.getMessage());
                throw new ApiException("Network error during registration: " + e.getMessage());
            }
        });
    }

    /**
     * Fetch data with authentication
     * @param token User's authentication token
     * @return CompletableFuture with data response
     */
    public CompletableFuture<DataResponse> fetchData(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(this.baseUrl + "/data")
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        return gson.fromJson(responseBody, DataResponse.class);
                    } else {
                        logger.warn("Data fetch failed with code: " + response.code());
                        throw new ApiException("Data fetch failed: " + response.code());
                    }
                }
            } catch (IOException e) {
                logger.error("Network error during data fetch: {}", e.getMessage());
                throw new ApiException("Network error during data fetch: " + e.getMessage());
            }
        });
    }

    /**
     * Fetch user profile information
     * @param token User's authentication token
     * @return CompletableFuture with profile response
     */
    public CompletableFuture<ProfileResponse> getUserProfile(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(this.baseUrl + "/profile")
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        return gson.fromJson(responseBody, ProfileResponse.class);
                    } else {
                        logger.warn("Profile fetch failed with code: " + response.code());
                        throw new ApiException("Profile fetch failed: " + response.code());
                    }
                }
            } catch (IOException e) {
                logger.error("Network error during profile fetch: {}", e.getMessage());
                throw new ApiException("Network error during profile fetch: " + e.getMessage());
            }
        });
    }

    // Response DTOs
    public static class RegistrationResponse {
        private boolean success;
        private String message;
        private String registrationUrl;
        private String registrationCode;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getRegistrationUrl() { return registrationUrl; }
        public void setRegistrationUrl(String registrationUrl) { this.registrationUrl = registrationUrl; }

        public String getRegistrationCode() { return registrationCode; }
        public void setRegistrationCode(String registrationCode) { this.registrationCode = registrationCode; }
    }

    public static class AuthResponse {
        private boolean valid;
        private String message;
        private UserData user;

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserData getUser() { return user; }
        public void setUser(UserData user) { this.user = user; }
    }

    public static class DataResponse {
        private boolean success;
        private String message;
        private Object data; // Can be more specific based on your API

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    public static class ProfileResponse {
        private boolean success;
        private String message;
        private UserData profile;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserData getProfile() { return profile; }
        public void setProfile(UserData profile) { this.profile = profile; }
    }

    public static class UserData {
        private String uuid;
        private String username;
        private String email;
        private long registeredAt;
        private String status;

        // Getters and setters
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public long getRegisteredAt() { return registeredAt; }
        public void setRegisteredAt(long registeredAt) { this.registeredAt = registeredAt; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }
    }
}