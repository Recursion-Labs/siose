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
    private final String baseUrl = "http://host.docker.internal:3000/v1";
    private final OkHttpClient client;
    private final Gson gson;

    public SioseApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
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
     * Login with token and player data
     * @param uuid Player's UUID
     * @param username Player's username
     * @param token Authentication token
     * @return CompletableFuture with auth response
     */
    public CompletableFuture<AuthResponse> login(String uuid, String username, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject minecraftPlayerData = new JsonObject();
                minecraftPlayerData.addProperty("id", uuid);
                minecraftPlayerData.addProperty("name", username);

                JsonObject json = new JsonObject();
                json.addProperty("token", token);
                json.add("minecraftPlayerData", minecraftPlayerData);

                RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
                Request request = new Request.Builder()
                        .url(this.baseUrl + "/auth/minecraft/login")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        logger.info("Login response: {}", responseBody);
                        return gson.fromJson(responseBody, AuthResponse.class);
                    } else {
                        logger.warn("Login failed with code: {}", response.code());
                        throw new ApiException("Login failed: " + response.code());
                    }
                }
            } catch (IOException e) {
                logger.error("Network error during login: {}", e.getMessage());
                throw new ApiException("Network error during login: " + e.getMessage());
            }
        });
    }

    /**
     * Fetch user data with authentication
     * @param token User's authentication token
     * @return CompletableFuture with user data
     */
    public CompletableFuture<UserData> fetchData(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(this.baseUrl + "/user/@me")
                        .addHeader("x-minecraft-token", token)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        UserWrapper wrapper = gson.fromJson(responseBody, UserWrapper.class);
                        return wrapper.getUser();
                    } else {
                        logger.warn("Data fetch failed with code: {}", response.code());
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
                        .url(this.baseUrl + "/user/@me")
                        .addHeader("x-minecraft-token", token)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        return gson.fromJson(responseBody, ProfileResponse.class);
                    } else {
                        logger.warn("Profile fetch failed with code: {}", response.code());
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
    public static class UserWrapper {
        private UserData user;

        public UserData getUser() { return user; }
        public void setUser(UserData user) { this.user = user; }
    }

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
        private boolean success;
        private String message;
        private UserData user;

        // Getters and setters
        public boolean isValid() { return success; }
        public void setValid(boolean valid) { this.success = valid; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserData getUser() { return user; }
        public void setUser(UserData user) { this.user = user; }
    }

    public static class DataResponse {
        private boolean success;
        private String message;
        private UserData data;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserData getData() { return data; }
        public void setData(UserData data) { this.data = data; }
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