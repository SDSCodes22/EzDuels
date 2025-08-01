package com.ezduels.listeners;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Arena;
import com.ezduels.model.Duel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Listener for arena-related events
 */
public class ArenaListener implements Listener {
    
    private final EzDuelsPlugin plugin;
    
    public ArenaListener(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle block placement in arenas
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Duel duel = plugin.getDuelManager().getDuel(player);
        
        if (duel == null || duel.getState() != Duel.DuelState.FIGHTING) {
            if(duel != null && duel.getState() == Duel.DuelState.COUNTDOWN) {
                event.setCancelled(true);
            }
            return;
        }
        
        Arena arena = duel.getArena();
        if (arena == null) {
            return;
        }
        
        // Check if block is within arena bounds
        if (!arena.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle block breaking in arenas
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Duel duel = plugin.getDuelManager().getDuel(player);

        if (duel == null || duel.getState() != Duel.DuelState.FIGHTING) {
            if(duel != null && duel.getState() == Duel.DuelState.COUNTDOWN) {
                event.setCancelled(true);
            }
            return;
        }

        Arena arena = duel.getArena();
        if (arena == null) {
            return;
        }

        // Check if block is within arena bounds
        if (!arena.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}