package com.ezduels.listeners;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Duel;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for duel-related events
 */
public class DuelListener implements Listener {
    
    private final EzDuelsPlugin plugin;
    private final ArrayList<UUID> respawnQueue;
    private final Map<UUID, Location> deathLocations;
    public DuelListener(EzDuelsPlugin plugin) {
        this.plugin = plugin;
        this.respawnQueue = new ArrayList<>();
        this.deathLocations = new ConcurrentHashMap<>();
    }
    
    /**
     * Handle player death during duels
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Duel duel = plugin.getDuelManager().getDuel(player);
        
        if (duel == null || duel.getState() != Duel.DuelState.FIGHTING) {
            return;
        }
        
        // Handle death in duel
        Player opponent = duel.getOpponent(player);
        if (opponent != null) {
            plugin.getDuelManager().endDuel(duel, opponent, event.getDrops());
        }
        
        // Handle keep inventory
        if (duel.isKeepInventory()) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
        } else {
            // Prevent normal drops - items will be handled by DuelManager
            event.getDrops().clear();
            event.setKeepInventory(false);
        }
        // Change gamerules so they do not see the death screen
        player.getWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        respawnQueue.add(player.getUniqueId());
        deathLocations.put(player.getUniqueId(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if(!respawnQueue.contains(event.getPlayer().getUniqueId())) {
            return; // Skip if it is not
        }
        Player player = event.getPlayer();
        // TODO: Breaks on servers with IMMEDIATE_RESPAWN set to true by default
        player.getWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, false);

        // Put them into spectator mode and teleport them to their death location
        event.getPlayer().setGameMode(GameMode.SPECTATOR);
        Location deathLocation = deathLocations.get(player.getUniqueId());
        if(deathLocation != null) {
            plugin.getLogger().info("[DuelListener.java:78] Death Location: " + deathLocation);
            event.getPlayer().teleportAsync(deathLocation);
        } else {
            plugin.getLogger().warning("Death location for player is null");
        }
        // Remove from queue
        respawnQueue.remove(player.getUniqueId());
        deathLocations.remove(player.getUniqueId());
        // Rest will be handled by duels manager!
    }

    /**
     * Prevent player from abusing Specatator mode to teleport to other players
     */
    @EventHandler
    public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
        // Check if player is in duel
        if(!plugin.getDuelManager().isInDuel(event.getPlayer())) {
            return;
        }
        if(event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            event.setCancelled(true);
        }
    }
}