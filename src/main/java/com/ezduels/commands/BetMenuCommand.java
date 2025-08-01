package com.ezduels.commands;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Duel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the betting menu
 */
public class BetMenuCommand implements CommandExecutor {
    
    private final EzDuelsPlugin plugin;
    
    public BetMenuCommand(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check if player has an active duel with betting
        Duel duel = plugin.getDuelManager().getDuel(player);
        if (duel == null) {
            player.sendMessage(Component.text("You are not in a duel!").color(NamedTextColor.RED));
            return true;
        }
        
        if (!duel.isBettingEnabled()) {
            player.sendMessage(Component.text("Betting is not enabled for this duel!").color(NamedTextColor.RED));
            return true;
        }
        
        if (!plugin.getBettingManager().isBettingActive(duel) || (duel.getState() != Duel.DuelState.PENDING && duel.getState() != Duel.DuelState.SETTING_UP)) {
            player.sendMessage(Component.text("Betting is not currently active!").color(NamedTextColor.RED));
            return true;
        }
        
        // Open betting GUI
        plugin.getGuiManager().openBettingGui(player, duel);
        
        return true;
    }
}