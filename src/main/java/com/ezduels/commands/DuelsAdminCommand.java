package com.ezduels.commands;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin command for managing duels and arenas
 */
public class DuelsAdminCommand implements CommandExecutor {
    
    private final EzDuelsPlugin plugin;
    
    public DuelsAdminCommand(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "arena":
                return handleArenaCommand(player, args);
            case "reload":
                return handleReloadCommand(player);
            default:
                sendHelp(player);
                return true;
        }
    }
    
    /**
     * Handle arena subcommands
     */
    private boolean handleArenaCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /duelsadmin arena <create|define|spawnloc|list>").color(NamedTextColor.RED));
            return true;
        }
        
        switch (args[1].toLowerCase()) {
            case "create":
                return handleArenaCreate(player, args);
            case "define":
                return handleArenaDefine(player, args);
            case "spawnloc":
                return handleArenaSpawnLoc(player, args);
            case "list":
                return handleArenaList(player);
            default:
                player.sendMessage(Component.text("Usage: /duelsadmin arena <create|define|spawnloc|list>").color(NamedTextColor.RED));
                return true;
        }
    }
    
    /**
     * Handle arena creation
     */
    private boolean handleArenaCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /duelsadmin arena create <basename>").color(NamedTextColor.RED));
            return true;
        }
        
        String basename = args[2];
        
        // Check if arena group already exists
        if (plugin.getArenaManager().hasArenaGroup(basename)) {
            player.sendMessage(Component.text("Arena group '" + basename + "' already exists!").color(NamedTextColor.RED));
            return true;
        }
        
        player.sendMessage(Component.text("Arena group '" + basename + "' created! Now select a region with WorldEdit and use /duelsadmin arena define " + basename).color(NamedTextColor.GREEN));
        
        return true;
    }
    
    /**
     * Handle arena definition
     */
    private boolean handleArenaDefine(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /duelsadmin arena define <basename>").color(NamedTextColor.RED));
            return true;
        }
        
        String basename = args[2];
        
        // Create arena from WorldEdit selection
        Arena arena = plugin.getArenaManager().createArena(basename, player);
        if (arena == null) {
            player.sendMessage(Component.text("Failed to create arena! Make sure you have a WorldEdit selection.").color(NamedTextColor.RED));
            return true;
        }
        
        player.sendMessage(Component.text("Arena '" + arena.getName() + "' created! Now set spawn locations with /duelsadmin arena spawnloc 1 and /duelsadmin arena spawnloc 2").color(NamedTextColor.GREEN));
        
        // Save arenas
        plugin.getArenaManager().saveArenas();
        
        return true;
    }
    
    /**
     * Handle arena spawn location setting
     */
    private boolean handleArenaSpawnLoc(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /duelsadmin arena spawnloc <1|2>").color(NamedTextColor.RED));
            return true;
        }
        
        int spawnNumber;
        try {
            spawnNumber = Integer.parseInt(args[2]);
            if (spawnNumber != 1 && spawnNumber != 2) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Spawn number must be 1 or 2!").color(NamedTextColor.RED));
            return true;
        }
        
        // Find the most recent arena created in this world
        Arena targetArena = null;
        for (String basename : plugin.getArenaManager().getArenaGroups()) {
            for (Arena arena : plugin.getArenaManager().getArenas(basename)) {
                if (arena.getWorld().equals(player.getWorld())) {
                    if (arena.contains(player.getLocation())) {
                        targetArena = arena;
                        break;
                    }
                }
            }
            if (targetArena != null) break;
        }
        
        if (targetArena == null) {
            player.sendMessage(Component.text("You must be standing in an arena to set spawn locations!").color(NamedTextColor.RED));
            return true;
        }
        
        // Set spawn location
        if (plugin.getArenaManager().setSpawnLocation(targetArena, spawnNumber, player.getLocation())) {
            player.sendMessage(Component.text("Spawn location " + spawnNumber + " set for arena '" + targetArena.getName() + "'!").color(NamedTextColor.GREEN));
            plugin.getArenaManager().saveArenas();
        } else {
            player.sendMessage(Component.text("Failed to set spawn location!").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    /**
     * Handle arena listing
     */
    private boolean handleArenaList(Player player) {
        if (plugin.getArenaManager().getArenaGroups().isEmpty()) {
            player.sendMessage(Component.text("No arenas have been created yet!").color(NamedTextColor.YELLOW));
            return true;
        }
        
        player.sendMessage(Component.text("Arena Groups:").color(NamedTextColor.GREEN));
        for (String basename : plugin.getArenaManager().getArenaGroups()) {
            int count = plugin.getArenaManager().getArenas(basename).size();
            player.sendMessage(Component.text("- " + basename + " (" + count + " arenas)").color(NamedTextColor.YELLOW));
        }
        
        return true;
    }
    
    /**
     * Handle reload command
     */
    private boolean handleReloadCommand(Player player) {
        plugin.reloadConfig();
        player.sendMessage(Component.text("EzDuels configuration reloaded!").color(NamedTextColor.GREEN));
        return true;
    }
    
    /**
     * Send help message
     */
    private void sendHelp(Player player) {
        player.sendMessage(Component.text("EzDuels Admin Commands:").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("/duelsadmin arena create <basename> - Create a new arena group").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/duelsadmin arena define <basename> - Define arena from WorldEdit selection").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/duelsadmin arena spawnloc <1|2> - Set spawn location for arena").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/duelsadmin arena list - List all arenas").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/duelsadmin reload - Reload configuration").color(NamedTextColor.YELLOW));
    }
}