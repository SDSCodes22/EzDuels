package com.ezduels.commands;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Duel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Command to challenge another player to a duel
 */
public class DuelCommand implements CommandExecutor, TabCompleter {
    
    private final EzDuelsPlugin plugin;
    
    public DuelCommand(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /duel <player>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }
        
        if (target.equals(player)) {
            player.sendMessage(Component.text("You cannot duel yourself!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check if either player is already in a duel
        if (plugin.getDuelManager().isInDuel(player)) {
            player.sendMessage(Component.text("You are already in a duel!").color(NamedTextColor.RED));
            return true;
        }
        
        if (plugin.getDuelManager().isInDuel(target)) {
            player.sendMessage(Component.text("That player is already in a duel!").color(NamedTextColor.RED));
            return true;
        }
        
        // Create duel
        Duel duel = plugin.getDuelManager().createDuel(player, target);
        if (duel == null) {
            player.sendMessage(Component.text("Failed to create duel!").color(NamedTextColor.RED));
            return true;
        }
        
        // Open setup GUI for challenger
        plugin.getGuiManager().openDuelSetupGui(player, duel);
        
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(commandSender instanceof Player)) return new ArrayList<>();

        if (args.length == 1) {
            Player sender = (Player) commandSender;
            List<String> tabCompletion = Bukkit.getOnlinePlayers().stream()
                    .filter(player -> !player.equals(sender))
                    .map(Player::getName)
                    .sorted()
                    .toList();

            return tabCompletion;
        }

        return new ArrayList<>();
    }
}