package me.samarthh;

import me.samarthh.api.SioseApiClient;
import me.samarthh.commands.GetDataCommand;
import me.samarthh.commands.RegisterCommand;
import me.samarthh.commands.LoginCommand;
import me.samarthh.commands.LogoutCommand;
import me.samarthh.commands.RequestProperty;
import me.samarthh.listeners.PropertyListener;
import me.samarthh.managers.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class Main extends JavaPlugin {

    private UserManager userManager;
    private PropertyListener propertyListener;
    private Map<UUID, List<Location>> propertyLocations = new HashMap<>();
    private Set<Location> unbreakableBlocks = new HashSet<>();
    private final Path unbreakableBlocksFile;
    private final Path propertyIdsFile;

    public Main() {
        this.unbreakableBlocksFile = Paths.get(getDataFolder().getPath(), "unbreakable_blocks.txt");
        this.propertyIdsFile = Paths.get(getDataFolder().getPath(), "property_ids.txt");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Plugin enabled!");

        // Load unbreakable blocks from file
        loadUnbreakableBlocks();

        // Load property IDs from file
        Map<String, Location> propertyIdMapping = loadPropertyIds();

        //String baseUrl = getConfig().getString("api.base-url", "http://host.docker.internal:3000");
        userManager = new UserManager();
        SioseApiClient apiClient = new SioseApiClient();

        // Register commands
        this.getCommand("getdata").setExecutor(new GetDataCommand(userManager, apiClient));
        this.getCommand("register").setExecutor(new RegisterCommand(userManager, apiClient));
        this.getCommand("login").setExecutor(new LoginCommand(userManager, apiClient));
        this.getCommand("logout").setExecutor(new LogoutCommand(userManager));
        this.getCommand("requestproperty").setExecutor(new RequestProperty(userManager, apiClient));

        // Register events
        propertyListener = new PropertyListener(userManager, propertyLocations, unbreakableBlocks, this, apiClient, propertyIdMapping);
        getServer().getPluginManager().registerEvents(propertyListener, this);
    }

    /**
     * Get the PropertyListener instance for callback access
     */
    public PropertyListener getPropertyListener() {
        return propertyListener;
    }

    /**
     * Load unbreakable blocks from file
     */
    private void loadUnbreakableBlocks() {
        try {
            if (!Files.exists(unbreakableBlocksFile)) {
                getLogger().info("No unbreakable blocks file found, starting fresh");
                return;
            }

            List<String> lines = Files.readAllLines(unbreakableBlocksFile);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    try {
                        String worldName = parts[0];
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int z = Integer.parseInt(parts[3]);

                        Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                        unbreakableBlocks.add(loc);
                    } catch (NumberFormatException e) {
                        getLogger().warning("Invalid location format in unbreakable blocks file: " + line);
                    }
                }
            }
            getLogger().info("Loaded " + unbreakableBlocks.size() + " unbreakable blocks from file");
        } catch (IOException e) {
            getLogger().warning("Failed to load unbreakable blocks: " + e.getMessage());
        }
    }

    /**
     * Load property IDs mapping from file
     */
    private Map<String, Location> loadPropertyIds() {
        Map<String, Location> propertyIdMapping = new HashMap<>();
        try {
            if (!Files.exists(propertyIdsFile)) {
                getLogger().info("No property IDs file found, starting fresh");
                return propertyIdMapping;
            }

            List<String> lines = Files.readAllLines(propertyIdsFile);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    try {
                        String propertyId = parts[0];
                        String worldName = parts[1];
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        int z = Integer.parseInt(parts[4]);

                        Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                        propertyIdMapping.put(propertyId, loc);
                    } catch (NumberFormatException e) {
                        getLogger().warning("Invalid property ID format in file: " + line);
                    }
                }
            }
            getLogger().info("Loaded " + propertyIdMapping.size() + " property ID mappings from file");
        } catch (IOException e) {
            getLogger().warning("Failed to load property IDs: " + e.getMessage());
        }
        return propertyIdMapping;
    }

    /**
     * Save property IDs mapping to file
     */
    private void savePropertyIds() {
        if (propertyListener == null) return;

        try {
            // Create data folder if it doesn't exist
            Files.createDirectories(propertyIdsFile.getParent());

            try (PrintWriter writer = new PrintWriter(new FileWriter(propertyIdsFile.toFile()))) {
                Set<String> propertyIds = propertyListener.getRegisteredPropertyIds();
                for (String propertyId : propertyIds) {
                    Location loc = propertyListener.getPropertyLocation(propertyId);
                    if (loc != null) {
                        writer.println(propertyId + "," + loc.getWorld().getName() + "," +
                                     loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                    }
                }
            }
            getLogger().info("Saved property ID mappings to file");
        } catch (IOException e) {
            getLogger().warning("Failed to save property IDs: " + e.getMessage());
        }
    }

    /**
     * Save unbreakable blocks to file
     */
    private void saveUnbreakableBlocks() {
        try {
            // Create data folder if it doesn't exist
            Files.createDirectories(unbreakableBlocksFile.getParent());

            try (PrintWriter writer = new PrintWriter(new FileWriter(unbreakableBlocksFile.toFile()))) {
                for (Location loc : unbreakableBlocks) {
                    writer.println(loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                }
            }
            getLogger().info("Saved " + unbreakableBlocks.size() + " unbreakable blocks to file");
        } catch (IOException e) {
            getLogger().warning("Failed to save unbreakable blocks: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled!");

        // Save unbreakable blocks to file
        saveUnbreakableBlocks();

        // Save property IDs to file
        savePropertyIds();

        if (userManager != null) {
            userManager.close();
        }
    }
}