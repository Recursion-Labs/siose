package me.samarthh;

import me.samarthh.commands.GetDataCommand;
import me.samarthh.commands.RegisterCommand;
import me.samarthh.commands.LoginCommand;
import me.samarthh.managers.UserManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private UserManager userManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Plugin enabled!");

        String baseUrl = getConfig().getString("api.base-url", "https://api.example.com");
        userManager = new UserManager();

        // Register commands
        this.getCommand("getdata").setExecutor(new GetDataCommand(userManager, baseUrl));
        this.getCommand("register").setExecutor(new RegisterCommand(userManager, baseUrl));
        this.getCommand("login").setExecutor(new LoginCommand(userManager, baseUrl));
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled!");
        if (userManager != null) {
            userManager.close();
        }
    }
}