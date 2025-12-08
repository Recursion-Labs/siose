package me.samarthh.listeners;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.plugin.Plugin;

public class PropertyListener implements Listener {

    private final UserManager userManager;
    private final Map<UUID, List<Location>> propertyLocations;
    private final Set<Location> unbreakableBlocks;
    private final Plugin plugin;

    public PropertyListener(UserManager userManager, Map<UUID, List<Location>> propertyLocations, Set<Location> unbreakableBlocks, Plugin plugin) {
        this.userManager = userManager;
        this.propertyLocations = propertyLocations;
        this.unbreakableBlocks = unbreakableBlocks;
        this.plugin = plugin;
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
                        new Thread(() -> {
                            try {
                                var apiClient = new me.samarthh.api.SioseApiClient();
                                var response = apiClient.requestProperty(token, entity).get(); // Blocking for simplicity
                                
                                // Run UI updates on main thread
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    if (response.isSuccess() || response.getMessage().toLowerCase().contains("successfully")) {
                                        player.sendMessage("Property registration requested.");

                                        // Place a sign above the first fence block
                                        Location signLoc = first.clone().add(0, 1, 0);
                                        Block signBlock = signLoc.getBlock();
                                        signBlock.setType(Material.OAK_SIGN);
                                        if (signBlock.getState() instanceof Sign sign) {
                                            SignSide front = sign.getSide(Side.FRONT);
                                            front.line(0, Component.text("BrickChain"));
                                            front.line(1, Component.text("Property"));
                                            front.line(2, Component.text("Requested"));
                                            sign.update();
                                            unbreakableBlocks.add(signLoc); // Make the sign unbreakable too
                                        }
                                    } else {
                                        player.sendMessage("Failed to submit property request: " + response.getMessage());
                                    }
                                });
                            } catch (Exception e) {
                                // Run error message on main thread
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    player.sendMessage("Error submitting property request: " + e.getMessage());
                                });
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