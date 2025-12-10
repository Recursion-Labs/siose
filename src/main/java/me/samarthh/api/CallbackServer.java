package me.samarthh.api;

import com.google.gson.Gson;
import me.samarthh.listeners.PropertyListener;
import org.bukkit.plugin.Plugin;
import spark.Spark;

import java.util.Map;

/**
 * HTTP callback server for receiving property status updates from external services
 */
public class CallbackServer {

    private final Plugin plugin;
    private final PropertyListener propertyListener;
    private final Gson gson;
    private final int port;
    private final String bindAddress;

    public CallbackServer(Plugin plugin, PropertyListener propertyListener, int port, String bindAddress) {
        this.plugin = plugin;
        this.propertyListener = propertyListener;
        this.gson = new Gson();
        this.port = port;
        this.bindAddress = bindAddress;
    }

    /**
     * Start the HTTP callback server
     */
    public void start() {
        // Bind to specified address to be accessible from outside Docker
        Spark.ipAddress(bindAddress);
        Spark.port(port);

        plugin.getLogger().info("Starting callback server on " + bindAddress + ":" + port);

        // Enable CORS for frontend requests
        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });

        // Handle preflight OPTIONS requests
        Spark.options("/*", (request, response) -> {
            response.status(200);
            return "";
        });

        // POST endpoint for property status updates
        Spark.post("/api/property/status", (request, response) -> {
            try {
                response.type("application/json");

                // Parse JSON body
                PropertyStatusRequest statusRequest = gson.fromJson(request.body(), PropertyStatusRequest.class);

                if (statusRequest == null || statusRequest.getPropertyId() == null || statusRequest.getStatus() == null) {
                    plugin.getLogger().warning("Invalid callback request: missing propertyId or status");
                    response.status(400);
                    return gson.toJson(Map.of("error", "Invalid request body. Required: propertyId and status"));
                }

                plugin.getLogger().info("Processing callback request for property " + statusRequest.getPropertyId() +
                                      " with status: " + statusRequest.getStatus());

                // Update property status (this is now thread-safe due to the fix in PropertyListener)
                propertyListener.receivePropertyStatusUpdate(statusRequest.getPropertyId(), statusRequest.getStatus());

                response.status(200);
                return gson.toJson(Map.of("success", true, "message", "Property status updated"));

            } catch (Exception e) {
                plugin.getLogger().warning("Error processing callback request: " + e.getMessage());
                plugin.getLogger().warning("Request body: " + request.body());
                response.status(500);
                return gson.toJson(Map.of("error", "Internal server error: " + e.getMessage()));
            }
        });

        // GET endpoint to list all registered properties
        Spark.get("/api/property/list", (request, response) -> {
            try {
                response.type("application/json");

                var propertyIds = propertyListener.getRegisteredPropertyIds();
                return gson.toJson(Map.of("properties", propertyIds));

            } catch (Exception e) {
                plugin.getLogger().warning("Error listing properties: " + e.getMessage());
                response.status(500);
                return gson.toJson(Map.of("error", "Internal server error"));
            }
        });

        // Health check endpoint
        Spark.get("/health", (request, response) -> {
            response.type("application/json");
            return gson.toJson(Map.of("status", "healthy", "timestamp", System.currentTimeMillis()));
        });

        // Debug endpoint to test connectivity
        Spark.get("/debug", (request, response) -> {
            response.type("application/json");
            return gson.toJson(Map.of(
                "server", "Siose Minecraft Plugin",
                "port", port,
                "bindAddress", bindAddress,
                "registeredProperties", propertyListener.getRegisteredPropertyIds().size(),
                "timestamp", System.currentTimeMillis()
            ));
        });

        plugin.getLogger().info("Callback server started on port " + port);
    }

    /**
     * Stop the HTTP callback server
     */
    public void stop() {
        Spark.stop();
        plugin.getLogger().info("Callback server stopped");
    }

    /**
     * Request class for property status updates
     */
    public static class PropertyStatusRequest {
        private String propertyId;
        private String status;

        public String getPropertyId() {
            return propertyId;
        }

        public void setPropertyId(String propertyId) {
            this.propertyId = propertyId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}