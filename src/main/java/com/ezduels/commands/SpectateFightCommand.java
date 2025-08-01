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

import java.util.Optional;

/**
 * Command to spectate a fight
 */
public class SpectateFightCommand implements CommandExecutor {
    
    private final EzDuelsPlugin plugin;
    
    public SpectateFightCommand(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check if player is already in a duel
        if (plugin.getDuelManager().isInDuel(player)) {
            player.sendMessage(Component.text("You cannot spectate while in a duel!").color(NamedTextColor.RED));
            return true;
        }
        
        // Find an active fight to spectate
        Optional<Duel> activeDuel = plugin.getDuelManager().getActiveDuels().stream()
            .filter(duel -> duel.getState() == Duel.DuelState.FIGHTING)
            .findFirst();
        
        if (activeDuel.isEmpty()) {
            player.sendMessage(Component.text("No active fights to spectate!").color(NamedTextColor.YELLOW));
            return true;
        }
        
        Duel duel = activeDuel.get();
        
        // Set player to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        
        // Teleport to arena
        if (duel.getArena() != null) {
            // Teleport to middle of arena
            double x = (duel.getArena().getMinPoint().getX() + duel.getArena().getMaxPoint().getX()) / 2;
            double y = Math.max(duel.getArena().getMinPoint().getY(), duel.getArena().getMaxPoint().getY()) + 5;
            double z = (duel.getArena().getMinPoint().getZ() + duel.getArena().getMaxPoint().getZ()) / 2;
            
            player.teleport(new org.bukkit.Location(duel.getArena().getWorld(), x, y, z));
        } else {
            // Teleport to one of the fighters
            player.teleport(duel.getChallenger().getLocation());
        }
        
        // Send spectate message
        Component message = Component.text("You are now spectating the fight between ")
            .append(Component.text(duel.getChallenger().getName()).color(NamedTextColor.YELLOW))
            .append(Component.text(" and "))
            .append(Component.text(duel.getTarget().getName()).color(NamedTextColor.YELLOW))
            .append(Component.text("!"))
            .color(NamedTextColor.GREEN);
        
        player.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(message));
        
        return true;
    }
}