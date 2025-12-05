package me.samarthh.commands;

import me.samarthh.managers.UserManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.UUID;

public class RegisterCommand implements CommandExecutor {

    private final OkHttpClient client = new OkHttpClient();
    private final UserManager userManager;

    public RegisterCommand(UserManager userManager) {
        this.userManager = userManager;
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

        // Hit the registration API
        new Thread(() -> {
            try {
                String url = "https://your-api-endpoint.com/register"; // Replace with actual API
                JsonObject json = new JsonObject();
                json.addProperty("uuid", uuid.toString());
                json.addProperty("username", player.getName());

                RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
                Request request = new Request.Builder().url(url).post(body).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                    String link = responseJson.get("link").getAsString(); // Assuming API returns a link
                    player.sendMessage("Registration initiated! Visit: " + link + " and use /login <token> to complete.");
                } else {
                    player.sendMessage("Failed to register: " + response.code());
                }
            } catch (IOException e) {
                player.sendMessage("Error during registration: " + e.getMessage());
            }
        }).start();

        return true;
    }
}