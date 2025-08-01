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
 * Command to cancel a duel before it starts
 */
public class CancelFightCommand implements CommandExecutor {
    
    private final EzDuelsPlugin plugin;
    
    public CancelFightCommand(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check if player is in a duel that can be cancelled
        Duel duel = plugin.getDuelManager().getDuel(player);
        if (duel == null) {
            player.sendMessage(Component.text("You are not in a duel!").color(NamedTextColor.RED));
            return true;
        }
        
        if (duel.getState() == Duel.DuelState.FIGHTING) {
            player.sendMessage(Component.text("You cannot cancel a duel that has already started! Use /leavefight instead.").color(NamedTextColor.RED));
            return true;
        }
        
        // Cancel the duel
        plugin.getDuelManager().cancelDuel(duel);
        
        // Send cancellation messages
        Component cancelMessage = Component.text("Duel cancelled!").color(NamedTextColor.YELLOW);
        player.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(cancelMessage));
        
        Player opponent = duel.getOpponent(player);
        if (opponent != null) {
            Component opponentMessage = Component.text(player.getName() + " has cancelled the duel!").color(NamedTextColor.YELLOW);
            opponent.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(opponentMessage));
        }
        
        return true;
    }
}