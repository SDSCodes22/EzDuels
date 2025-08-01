package com.ezduels.listeners;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Duel;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for player-related events
 */
public class PlayerListener implements Listener {
    
    private final EzDuelsPlugin plugin;
    public static final Map<UUID, Location> teleportationQueue = new HashMap<>();

    public PlayerListener(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player disconnection
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Duel duel = plugin.getDuelManager().getDuel(player);
        
        if (duel == null) {
            return;
        }
        
        // Handle disconnection based on duel state and settings
        if (duel.getState() == Duel.DuelState.FIGHTING) {
            if (duel.isKeepInventory() && !duel.isBettingEnabled()) {
                // Instant loss
                Player opponent = duel.getOpponent(player);
                if (opponent != null) {
                    plugin.getDuelManager().endDuel(duel, opponent, null);
                }
            } else {
                // Grace period for reconnection
                // TODO: Implement reconnection grace period
                Player opponent = duel.getOpponent(player);
                if (opponent != null) {
                    plugin.getDuelManager().endDuel(duel, opponent, null);
                }
            }
        } else {
            // Cancel duel if not fighting
            plugin.getDuelManager().cancelDuel(duel);
        }
    }

    /**
     * Handle player joining back
     */
    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent event) {
        plugin.getLogger().info("Received PlayerLoginEvent!");
        if(teleportationQueue.containsKey(event.getPlayer().getUniqueId())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getLogger().info("Teleporting player!");

                    event.getPlayer().teleportAsync(teleportationQueue.get(event.getPlayer().getUniqueId()));
                    teleportationQueue.remove(event.getPlayer().getUniqueId());
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    public static void addToTeleportationQueue(Player player, Location location) { teleportationQueue.put(player.getUniqueId(), location); }
}