package me.samarthh.listeners;

import me.samarthh.managers.UserManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PropertyListener implements Listener {

    private final UserManager userManager;
    private final Map<UUID, List<Location>> propertyLocations;
    private final Set<Location> unbreakableBlocks;

    public PropertyListener(UserManager userManager, Map<UUID, List<Location>> propertyLocations, Set<Location> unbreakableBlocks) {
        this.userManager = userManager;
        this.propertyLocations = propertyLocations;
        this.unbreakableBlocks = unbreakableBlocks;
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
                        // Call API
                        new Thread(() -> {
                            try {
                                var apiClient = new me.samarthh.api.SioseApiClient();
                                var response = apiClient.requestProperty(token, entity).get(); // Blocking for simplicity
                                if (response.isSuccess()) {
                                    player.sendMessage("Property request submitted successfully! Area: " + areaSqFt + " square feet.");
                                } else {
                                    player.sendMessage("Failed to submit property request: " + response.getMessage());
                                }
                            } catch (Exception e) {
                                player.sendMessage("Error submitting property request: " + e.getMessage());
                            }
                        }).start();
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