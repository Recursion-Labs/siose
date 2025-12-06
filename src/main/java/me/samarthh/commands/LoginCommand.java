package me.samarthh.commands;

import me.samarthh.api.SioseApiClient;
import me.samarthh.managers.UserManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LoginCommand implements CommandExecutor {

    private final UserManager userManager;
    private final SioseApiClient apiClient;

    public LoginCommand(UserManager userManager, String baseUrl) {
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

        if (args.length != 1) {
            player.sendMessage("Usage: /login <token>");
            return true;
        }

        String token = args[0];
        player.sendMessage("Validating token...");

        // Use the API client to validate the token
        apiClient.validateToken(uuid.toString(), token)
                .thenAccept(response -> {
                    if (response.isValid()) {
                        userManager.setToken(uuid, token);
                        player.sendMessage("Successfully logged in! You can now use /getdata.");
                    } else {
                        player.sendMessage("Invalid token: " + response.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    player.sendMessage("Error during login: " + throwable.getMessage());
                    return null;
                });

        return true;
    }
}