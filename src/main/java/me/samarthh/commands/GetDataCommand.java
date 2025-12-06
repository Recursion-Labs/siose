package me.samarthh.commands;

import me.samarthh.api.SioseApiClient;
import me.samarthh.managers.UserManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GetDataCommand implements CommandExecutor {

    private final UserManager userManager;
    private final SioseApiClient apiClient;

    public GetDataCommand(UserManager userManager, String baseUrl) {
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

        if (!userManager.isAuthenticated(player.getUniqueId())) {
            player.sendMessage("You need to authenticate first! Use /register to start or /login <token> if you have one.");
            return true;
        }

        String token = userManager.getToken(player.getUniqueId());
        player.sendMessage("Fetching data...");

        // Use the API client to fetch data
        apiClient.fetchData(token)
                .thenAccept(response -> {
                    if (response.isSuccess()) {
                        player.sendMessage("Data received: " + response.getData());
                    } else {
                        player.sendMessage("Failed to fetch data: " + response.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    player.sendMessage("Error fetching data: " + throwable.getMessage());
                    return null;
                });

        return true;
    }
}