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
        getLogger().info("Plugin enabled!");

        userManager = new UserManager();

        // Register commands
        this.getCommand("getdata").setExecutor(new GetDataCommand(userManager));
        this.getCommand("register").setExecutor(new RegisterCommand(userManager));
        this.getCommand("login").setExecutor(new LoginCommand(userManager));
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled!");
        if (userManager != null) {
            userManager.close();
        }
    }
}