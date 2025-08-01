package com.ezduels.listeners;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Duel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Listener for betting-related events
 */
public class BettingListener implements Listener {
    
    private final EzDuelsPlugin plugin;
    
    public BettingListener(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle inventory changes during betting
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // Check if player is in betting phase
        Duel duel = plugin.getDuelManager().getDuel(player);
        if (duel == null || !duel.isBettingEnabled()) {
            return;
        }
        
        if (!plugin.getBettingManager().isBettingActive(duel)) {
            return;
        }
        
        // Check if clicking in player's main inventory during betting
        if (event.getClickedInventory() == player.getInventory()) {
            // Reset bet confirmations when inventory changes
            // This is handled by the betting manager when bets are updated
        }
    }
}