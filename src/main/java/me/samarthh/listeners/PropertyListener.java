package me.samarthh.listeners;

import me.samarthh.api.SioseApiClient;
import me.samarthh.managers.UserManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.plugin.Plugin;

public class PropertyListener implements Listener {

    private final UserManager userManager;
    private final SioseApiClient apiClient;
    private final Map<UUID, List<Location>> propertyLocations;
    private final Set<Location> unbreakableBlocks;
    private final Plugin plugin;
    private final Map<String, Location> propertyIdToSignLocation;
    private final NamespacedKey propertyIdKey;
    private final Path callbackDataPath;

    public PropertyListener(UserManager userManager, Map<UUID, List<Location>> propertyLocations, Set<Location> unbreakableBlocks, Plugin plugin, SioseApiClient apiClient) {
        this(userManager, propertyLocations, unbreakableBlocks, plugin, apiClient, new HashMap<>());
    }

    public PropertyListener(UserManager userManager, Map<UUID, List<Location>> propertyLocations, Set<Location> unbreakableBlocks, Plugin plugin, SioseApiClient apiClient, Map<String, Location> propertyIdMapping) {
        this.userManager = userManager;
        this.apiClient = apiClient;
        this.propertyLocations = propertyLocations;
        this.unbreakableBlocks = unbreakableBlocks;
        this.plugin = plugin;
        this.propertyIdToSignLocation = new ConcurrentHashMap<>(propertyIdMapping);
        this.propertyIdKey = new NamespacedKey(plugin, "property_id");
        this.callbackDataPath = Paths.get(plugin.getDataFolder().getPath(), "property_callbacks.json");

        // Create data folder if it doesn't exist
        try {
            Files.createDirectories(callbackDataPath.getParent());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create data directory: " + e.getMessage());
        }

        // Start callback checking task
        startCallbackCheckTask();
    }

    private void startCallbackCheckTask() {
        // Check for callback updates every 10 seconds (200 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkCallbacks, 200L, 200L);
    }

    private void checkCallbacks() {
        try {
            if (!Files.exists(callbackDataPath)) {
                return;
            }

            String content = Files.readString(callbackDataPath);
            if (content.trim().isEmpty()) {
                return;
            }

            // Parse JSON and update properties
            // For simplicity, assume format: {"propertyId": "status"}
            // In real implementation, you'd use a proper JSON parser
            String[] updates = content.split("\n");
            for (String update : updates) {
                if (update.trim().isEmpty()) continue;
                // Simple parsing: propertyId:status
                String[] parts = update.split(":");
                if (parts.length == 2) {
                    String propertyId = parts[0].trim();
                    String status = parts[1].trim();
                    updatePropertyStatus(propertyId, status);
                }
            }

            // Clear the callback file after processing
            Files.writeString(callbackDataPath, "");

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to check callbacks: " + e.getMessage());
        }
    }

    /**
     * Public callback method that can be called by external services
     * This is the main callback URL endpoint functionality
     */
    public void receivePropertyStatusUpdate(String propertyId, String status) {
        updatePropertyStatus(propertyId, status);
        plugin.getLogger().info("Received callback update for property " + propertyId + ": " + status);
    }

    /**
     * Get all registered property IDs (for external services to know what properties exist)
     */
    public Set<String> getRegisteredPropertyIds() {
        return new HashSet<>(propertyIdToSignLocation.keySet());
    }

    /**
     * Get property location by ID (for debugging/external services)
     */
    public Location getPropertyLocation(String propertyId) {
        return propertyIdToSignLocation.get(propertyId);
    }

    /**
     * Update property status by ID (callback method)
     */
    public void updatePropertyStatus(String propertyId, String status) {
        Location signLoc = propertyIdToSignLocation.get(propertyId);
        if (signLoc == null) {
            plugin.getLogger().warning("Property ID not found: " + propertyId);
            return;
        }

        Block block = signLoc.getBlock();
        if (!(block.getState() instanceof Sign sign)) {
            plugin.getLogger().warning("Sign not found at location for property: " + propertyId);
            return;
        }

        // Update sign on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            SignSide front = sign.getSide(Side.FRONT);
            front.line(0, Component.text("BrickChain"));
            front.line(1, Component.text("Property"));

            switch (status.toLowerCase()) {
                case "approved":
                    front.line(2, Component.text("Approved"));
                    break;
                case "rejected":
                    front.line(2, Component.text("Rejected"));
                    break;
                case "pending":
                    front.line(2, Component.text("Pending"));
                    break;
                default:
                    front.line(2, Component.text(status));
                    break;
            }

            sign.update();
        });
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.displayName() != null && Component.text("BrickChain-Property-Onboarding").equals(meta.displayName())) {
                UUID uuid = player.getUniqueId();
                if (!userManager.isAuthenticated(uuid)) {
                    player.sendMessage("You must be authenticated to define property.");
                    event.setCancelled(true);
                    return;
                }

                Location loc = event.getBlock().getLocation();
                List<Location> locations = propertyLocations.computeIfAbsent(uuid, k -> new ArrayList<>());
                locations.add(loc);
                unbreakableBlocks.add(loc); // Make it unbreakable
                
                if (locations.size() == 1) {
                    player.sendMessage("First corner set. Place the second fence block for length.");
                } else if (locations.size() == 2) {
                    player.sendMessage("Second corner set. Place the third fence block for breadth.");
                } else if (locations.size() == 3) {
                    // Calculate area
                    Location first = locations.get(0);
                    Location second = locations.get(1);
                    Location third = locations.get(2);
                    
                    double length = first.distance(second);
                    double breadth = first.distance(third);
                    int areaSqFt = (int) Math.round(length * breadth);
                    
                    // Create entity JSON
                    String entity = String.format("{\"coordinates\": [[%d,%d,%d], [%d,%d,%d], [%d,%d,%d]], \"area\": %d}",
                            first.getBlockX(), first.getBlockY(), first.getBlockZ(),
                            second.getBlockX(), second.getBlockY(), second.getBlockZ(),
                            third.getBlockX(), third.getBlockY(), third.getBlockZ(),
                            areaSqFt);
                    
                    String token = userManager.getToken(uuid);
                    if (token != null) {
                        // Call API asynchronously
                        apiClient.requestProperty(token, entity)
                                .thenAccept(response -> {
                                    // Run UI updates on main thread
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        if (response.isSuccess() || response.getMessage().toLowerCase().contains("successfully")) {
                                            player.sendMessage("Property registration requested.");

                                            // Generate unique property ID
                                            String propertyId = UUID.randomUUID().toString();

                                            // Place a sign above the first fence block
                                            Location signLoc = first.clone().add(0, 1, 0);
                                            Block signBlock = signLoc.getBlock();
                                            signBlock.setType(Material.OAK_SIGN);
                                            if (signBlock.getState() instanceof Sign sign) {
                                                // Store property ID on the sign
                                                PersistentDataContainer container = sign.getPersistentDataContainer();
                                                container.set(propertyIdKey, PersistentDataType.STRING, propertyId);

                                                // Register the property ID to sign location mapping
                                                propertyIdToSignLocation.put(propertyId, signLoc);

                                                SignSide front = sign.getSide(Side.FRONT);
                                                front.line(0, Component.text("BrickChain"));
                                                front.line(1, Component.text("Property"));
                                                front.line(2, Component.text("Requested"));
                                                front.line(3, Component.text("ID: " + propertyId.substring(0, 8)));
                                                sign.update();
                                                unbreakableBlocks.add(signLoc); // Make the sign unbreakable too

                                                // Log the property ID for the external service
                                                plugin.getLogger().info("Property registered with ID: " + propertyId + " at " + signLoc);
                                            }
                                        } else {
                                            player.sendMessage("Failed to submit property request: " + response.getMessage());
                                        }
                                    });
                                })
                                .exceptionally(throwable -> {
                                    // Run error message on main thread
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        player.sendMessage("Error submitting property request: " + throwable.getMessage());
                                    });
                                    return null;
                                });
                    }
                    
                    // Clear for next request
                    propertyLocations.remove(uuid);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (unbreakableBlocks.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}