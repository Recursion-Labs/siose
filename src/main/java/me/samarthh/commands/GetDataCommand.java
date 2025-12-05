package me.samarthh.commands;

import me.samarthh.managers.UserManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

public class GetDataCommand implements CommandExecutor {

    private final OkHttpClient client = new OkHttpClient();
    private final UserManager userManager;

    public GetDataCommand(UserManager userManager) {
        this.userManager = userManager;
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

        // Run API call asynchronously to avoid blocking the main thread
        new Thread(() -> {
            try {
                String url = "https://your-api-endpoint.com/data"; // Replace with actual API
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String data = response.body().string();
                    player.sendMessage("API Response: " + data);
                } else {
                    player.sendMessage("Failed to fetch data: " + response.code());
                }
            } catch (IOException e) {
                player.sendMessage("Error fetching data: " + e.getMessage());
            }
        }).start();

        return true;
    }
}