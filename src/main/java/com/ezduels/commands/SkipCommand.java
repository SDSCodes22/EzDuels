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
 * Command to skip the countdown
 */
public class SkipCommand implements CommandExecutor {
    
    private final EzDuelsPlugin plugin;
    
    public SkipCommand(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check if player is in a duel with countdown
        Duel duel = plugin.getDuelManager().getDuel(player);
        if (duel == null) {
            player.sendMessage(Component.text("You are not in a duel!").color(NamedTextColor.RED));
            return true;
        }
        
        if (duel.getState() != Duel.DuelState.COUNTDOWN) {
            player.sendMessage(Component.text("There is no countdown to skip!").color(NamedTextColor.RED));
            return true;
        }
        
        // Vote to skip countdown
        plugin.getDuelManager().skipCountdown(player);
        
        Player opponent = duel.getOpponent(player);
        Component message = Component.text("wants to skip the countdown!")
            .color(NamedTextColor.YELLOW);
        
        player.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space())
            .append(Component.text("You voted to skip the countdown!").color(NamedTextColor.GREEN)));
        
        if (opponent != null) {
            opponent.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space())
                .append(Component.text(player.getName()).color(NamedTextColor.YELLOW))
                .append(Component.space()).append(message));
        }
        
        return true;
    }
}