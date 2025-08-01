package com.ezduels.commands;

import com.ezduels.EzDuelsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to view unclaimed prizes
 */
public class PrizesCommand implements CommandExecutor {
    
    private final EzDuelsPlugin plugin;
    
    public PrizesCommand(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check if player has prizes
        if (!plugin.getPrizeManager().hasPrizes(player)) {
            player.sendMessage(Component.text("You have no prizes to claim!").color(NamedTextColor.YELLOW));
            return true;
        }
        
        // Determine page
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid page number!").color(NamedTextColor.RED));
                return true;
            }
        }
        
        // Open prizes GUI
        plugin.getGuiManager().openPrizesGui(player, page);
        
        return true;
    }
}