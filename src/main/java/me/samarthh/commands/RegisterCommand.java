package me.samarthh.commands;

import me.samarthh.managers.UserManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RegisterCommand implements CommandExecutor {

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
        player.sendMessage("Please visit: https://brickchain.samarthh.me/register to register your account.");
        return true;
    }
}