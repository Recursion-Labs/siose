package me.samarthh.commands;

import me.samarthh.api.SioseApiClient;
import me.samarthh.managers.UserManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class RequestProperty implements CommandExecutor {

    private final UserManager userManager;
    private final SioseApiClient apiClient;

    public RequestProperty(UserManager userManager, SioseApiClient apiClient) {
        this.userManager = userManager;
        this.apiClient = apiClient;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (!userManager.isAuthenticated(uuid)) {
            player.sendMessage("You must be authenticated to request a property. Use /login first.");
            return true;
        }

        String token = userManager.getToken(uuid);
        if (token == null) {
            player.sendMessage("No token found. Please login again.");
            return true;
        }

        player.sendMessage("Checking eligibility for property request...");

        apiClient.checkEligibility(token)
                .thenAccept(response -> {
                    if (response.isEligible()) {
                        // Create enchanted fence blocks
                        ItemStack fence = new ItemStack(Material.OAK_FENCE, 3);
                        ItemMeta meta = fence.getItemMeta();
                        if (meta != null) {
                            meta.displayName(Component.text("BrickChain-Property-Onboarding"));
                            meta.addEnchant(Enchantment.DURABILITY, 1, true); // Unbreaking I to make it enchanted
                            fence.setItemMeta(meta);
                        }

                        // Give to player
                        player.getInventory().addItem(fence);
                        player.sendMessage("You have received 3 enchanted fence blocks. Place them to define your property area: first for corner, second for length, third for breadth.");
                    } else {
                        player.sendMessage("You are not eligible to request a property: " + response.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    player.sendMessage("Error checking eligibility: " + throwable.getMessage());
                    return null;
                });

        return true;
    }
}
