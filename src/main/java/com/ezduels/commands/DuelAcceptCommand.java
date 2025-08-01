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
 * Command to accept a duel challenge
 */
public class DuelAcceptCommand implements CommandExecutor {
    
    private final EzDuelsPlugin plugin;
    
    public DuelAcceptCommand(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check if player has a pending duel
        Duel duel = plugin.getDuelManager().getDuel(player);
        if (duel == null || !duel.getTarget().equals(player)) {
            player.sendMessage(Component.text("You have no pending duel challenges!").color(NamedTextColor.RED));
            return true;
        }
        
        // Accept the duel
        if (plugin.getDuelManager().acceptDuel(player)) {
            // Send confirmation to both players
            Component message = Component.text("Duel accepted! Setting up...").color(NamedTextColor.GREEN);
            player.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(message));
            duel.getChallenger().sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(message));
            
            // Handle betting setup or start countdown
            if (duel.isBettingEnabled()) {
                plugin.getBettingManager().initializeBetting(duel);
                plugin.getGuiManager().openBettingGui(player, duel);
                plugin.getGuiManager().openBettingGui(duel.getChallenger(), duel);
            } else {
                plugin.getDuelManager().startCountdown(duel);
            }
        } else {
            player.sendMessage(Component.text("Failed to accept duel!").color(NamedTextColor.RED));
        }
        
        return true;
    }
}