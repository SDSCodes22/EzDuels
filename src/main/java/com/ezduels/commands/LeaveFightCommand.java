package com.ezduels.commands;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Duel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command to leave/forfeit a fight
 */
public class LeaveFightCommand implements CommandExecutor {
    
    private final EzDuelsPlugin plugin;
    
    public LeaveFightCommand(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check if player is in a fighting duel
        Duel duel = plugin.getDuelManager().getDuel(player);
        if (duel == null) {
            player.sendMessage(Component.text("You are not in a duel!").color(NamedTextColor.RED));
            return true;
        }
        
        if (duel.getState() != Duel.DuelState.FIGHTING) {
            player.sendMessage(Component.text("You are not currently fighting!").color(NamedTextColor.RED));
            return true;
        }
        
        // End duel with opponent as winner
        Player opponent = duel.getOpponent(player);
        if (opponent != null) {
            plugin.getDuelManager().endDuel(duel, opponent, null);
            // Set them in spectator
            player.setGameMode(GameMode.SPECTATOR);
            // Send forfeit messages
            Component forfeitMessage = Component.text("You have forfeited the duel!").color(NamedTextColor.RED);
            player.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(forfeitMessage));
            
            Component winMessage = Component.text(player.getName() + " has forfeited! You win!").color(NamedTextColor.GREEN);
            opponent.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(winMessage));
        } else {
            plugin.getDuelManager().cancelDuel(duel);
        }
        
        return true;
    }
}