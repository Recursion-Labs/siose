package me.samarthh;

import me.samarthh.api.SioseApiClient;
import me.samarthh.commands.GetDataCommand;
import me.samarthh.commands.RegisterCommand;
import me.samarthh.commands.LoginCommand;
import me.samarthh.commands.LogoutCommand;
import me.samarthh.commands.RequestProperty;
import me.samarthh.listeners.PropertyListener;
import me.samarthh.managers.UserManager;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class Main extends JavaPlugin {

    private UserManager userManager;
    private Map<UUID, List<Location>> propertyLocations = new HashMap<>();
    private Set<Location> unbreakableBlocks = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Plugin enabled!");

        //String baseUrl = getConfig().getString("api.base-url", "http://host.docker.internal:3000/v1");
        userManager = new UserManager();
        SioseApiClient apiClient = new SioseApiClient();

        // Register commands
        this.getCommand("getdata").setExecutor(new GetDataCommand(userManager, apiClient));
        this.getCommand("register").setExecutor(new RegisterCommand(userManager, apiClient));
        this.getCommand("login").setExecutor(new LoginCommand(userManager, apiClient));
        this.getCommand("logout").setExecutor(new LogoutCommand(userManager));
        this.getCommand("requestproperty").setExecutor(new RequestProperty(userManager, apiClient));

        // Register events
        PropertyListener propertyListener = new PropertyListener(userManager, propertyLocations, unbreakableBlocks, this);
        getServer().getPluginManager().registerEvents(propertyListener, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled!");
        if (userManager != null) {
            userManager.close();
        }
    }
}