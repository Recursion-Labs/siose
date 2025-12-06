package me.samarthh.commands;

import me.samarthh.api.SioseApiClient;
import me.samarthh.managers.UserManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RegisterCommand implements CommandExecutor {

    private final UserManager userManager;
    private final SioseApiClient apiClient;

    public RegisterCommand(UserManager userManager, String baseUrl) {
        this.userManager = userManager;
        this.apiClient = new SioseApiClient(baseUrl);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (userManager.isAuthenticated(uuid)) {
            player.sendMessage("You are already authenticated!");
            return true;
        }

        player.sendMessage("Initiating registration...");

        // Use the API client for registration
        apiClient.registerUser(uuid.toString(), player.getName())
                .thenAccept(response -> {
                    if (response.isSuccess()) {
                        String message = "Registration initiated!";
                        if (response.getRegistrationUrl() != null) {
                            message += " Visit: " + response.getRegistrationUrl();
                        }
                        if (response.getRegistrationCode() != null) {
                            message += " Code: " + response.getRegistrationCode();
                        }
                        message += " Then use /login <token> to complete authentication.";
                        player.sendMessage(message);
                    } else {
                        player.sendMessage("Registration failed: " + response.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    player.sendMessage("Error during registration: " + throwable.getMessage());
                    return null;
                });

        return true;
    }
}