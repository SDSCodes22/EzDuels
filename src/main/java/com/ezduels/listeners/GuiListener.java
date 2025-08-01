package com.ezduels.listeners;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Arena;
import com.ezduels.model.Duel;
import com.ezduels.model.Prize;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Listener for GUI interactions
 */
public class GuiListener implements Listener {
    
    private final EzDuelsPlugin plugin;
    
    public GuiListener(EzDuelsPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle inventory click events
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        String guiType = plugin.getGuiManager().getOpenGui(player);
        if (guiType == null) {
            return;
        }
        
        // We never allow a drop so always deny (ez exploits)
        if(event.getClick() == ClickType.DROP || (!(event.getCursor().isEmpty()) && event.getClickedInventory() == null)) {
            event.setCancelled(true);
        }
        switch (guiType) {
            case "duel_setup":
                handleDuelSetupClick(player, event);
                event.setCancelled(true);
                break;
            case "betting":
                handleBettingClick(player, event);
                break;
            case "prizes":
                handlePrizesClick(player, event);
                break;
        }
    }
    
    /**
     * Handle duel setup GUI clicks
     */
    private void handleDuelSetupClick(Player player, InventoryClickEvent event) {
        Duel duel = plugin.getDuelManager().getDuel(player);
        if (duel == null || !duel.getChallenger().equals(player)) {
            return;
        }
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 11: // Keep Inventory toggle
                duel.setKeepInventory(!duel.isKeepInventory());
                plugin.getGuiManager().refreshDuelSetupGui(player, duel);
                break;
            case 13: // Betting toggle
                duel.setBettingEnabled(!duel.isBettingEnabled());
                plugin.getGuiManager().refreshDuelSetupGui(player, duel);
                break;
            case 15: // Arena selection
                cycleArenaSelection(player, duel);
                break;
            case 22: // Confirm
                confirmDuelSetup(player, duel);
                break;
        }
    }
    
    /**
     * Cycle through available arena options
     */
    private void cycleArenaSelection(Player player, Duel duel) {
        List<String> arenaGroups = new ArrayList<>(plugin.getArenaManager().getArenaGroups());
        arenaGroups.add(0, "AUTO"); // Add AUTO as first option
        
        String currentArena = duel.getArena() != null ? duel.getArena().getBasename() : "AUTO";
        int currentIndex = arenaGroups.indexOf(currentArena);
        int nextIndex = (currentIndex + 1) % arenaGroups.size();
        
        String nextArena = arenaGroups.get(nextIndex);
        if ("AUTO".equals(nextArena)) {
            duel.setArena(null);
        } else {
            // Get an available arena from the group
            Arena arena = plugin.getArenaManager().getAvailableArena(nextArena);
            duel.setArena(arena);
        }
        
        plugin.getGuiManager().refreshDuelSetupGui(player, duel);
    }
    
    /**
     * Handle betting GUI clicks
     */
    private void handleBettingClick(Player player, InventoryClickEvent event) {
        Duel duel = plugin.getDuelManager().getDuel(player);
        if (duel == null || !duel.isBettingEnabled()) {
            return;
        }
        
        int slot = event.getSlot();
        
        // Check if clicking confirmation buttons
        if (slot == 45) {
            // Player confirmation / unconfirmation
            if(plugin.getBettingManager().isConfirmed(duel, player)) {
                plugin.getBettingManager().unconfirmBet(duel, player);
            } else {
                plugin.getBettingManager().confirmBet(duel, player);
            }
            plugin.getGuiManager().refreshBetGui(duel.getOpponent(player), duel);
            plugin.getGuiManager().refreshBetGui(player, duel);
            event.setCancelled(true);
        } else if (slot == 53) {
            // Prevent clicking other player's confirmation button
            event.setCancelled(true);
        } else if (slot % 9 >= 4 && event.getClickedInventory() == event.getWhoClicked().getOpenInventory().getTopInventory()) {
            // Prevent clicking on other player's side or on the glass divider
            event.setCancelled(true);
        }
        else if (bettingInventoryBypassAttempt(event)) {
            event.setCancelled(true);
        }
        else {
            // Handle bet item placement/removal, but ignore the click if the player clicked their inventory.
            handleBetItemClick(player, duel, event);
        }
    }

    /**
     * Returns true under the following scenarios:
     * - Player attempting to drop an item
     * - Player trying to "gather all" to their inventory
     * @param event Relevant InventoryClickEvent
     * @return True if this is a bypass e.g. trying to drop the item.
     */
    private Boolean bettingInventoryBypassAttempt(InventoryClickEvent event) {
        // Case 1: Player attempting to drop item
        if(event.getClick() == ClickType.DROP) return true;
        return false; // Do not simplify for readability and for expanding.
    }
    /**
     * Handle bet item clicks
     */
    private void handleBetItemClick(Player player, Duel duel, InventoryClickEvent event) {
        int slot = event.getSlot();
        
        // Only allow players to modify their own side
        if (!isPlayerSlot(slot, player, duel) && event.getClickedInventory() == event.getWhoClicked().getOpenInventory().getTopInventory()) {
            event.setCancelled(true); // Keep cancelled
            return;
        }

        // Update bet after the click
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            List<ItemStack> playerBet = new ArrayList<>();
            
            // Collect items from player's side
            for (int i = 0; i < 45; i++) {
                if (isPlayerSlot(i, player, duel)) {
                    ItemStack item = event.getInventory().getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        playerBet.add(item);
                    }
                }
            }
            
            plugin.getBettingManager().updateBet(duel, player, playerBet);
            plugin.getGuiManager().refreshBetGui(duel.getOpponent(player), duel);
            plugin.getGuiManager().refreshBetGui(player, duel);
        }, 1L);
    }
    
    /**
     * Check if a slot belongs to a player in the betting GUI (slot on the left)
     */
    private boolean isPlayerSlot(int slot, Player player, Duel duel) {
        return slot % 9 < 4;
    }
    
    /**
     * Handle prizes GUI clicks
     */
    private void handlePrizesClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        
        if (slot == 48) { // Previous page
            int currentPage = plugin.getGuiManager().getGuiPage(player);
            if (currentPage > 1) {
                plugin.getGuiManager().openPrizesGui(player, currentPage - 1);
            }
        } else if (slot == 50) { // Next page
            int currentPage = plugin.getGuiManager().getGuiPage(player);
            plugin.getGuiManager().openPrizesGui(player, currentPage + 1);
        } else if (slot == 49) { // Close
            player.closeInventory();
        }
    }
    
    /**
     * Confirm duel setup
     */
    private void confirmDuelSetup(Player player, Duel duel) {
        // Set state to pending
        duel.setState(Duel.DuelState.PENDING);
        player.closeInventory();
        
        // Send challenge message to target
        Component challengeMessage = MiniMessage.miniMessage().deserialize(
            plugin.getConfig().getString("messages.duel-challenge", "")
                .replace("{challenger}", player.getName())
        );
        
        String lootDrop = duel.isKeepInventory() ? "Disabled" : "Enabled";
        String betting = duel.isBettingEnabled() ? "Enabled" : "Disabled";
        String arena = duel.getArena() != null ? duel.getArena().getBasename() : "Auto";
        
        Component detailsMessage = MiniMessage.miniMessage().deserialize(
            plugin.getConfig().getString("messages.duel-details", "")
                .replace("{loot}", lootDrop)
                .replace("{betting}", betting)
                .replace("{arena}", arena)
        );
        
        Component acceptMessage = MiniMessage.miniMessage().deserialize(
            plugin.getConfig().getString("messages.duel-accept", "")
        );
        
        duel.getTarget().sendMessage(challengeMessage);
        duel.getTarget().sendMessage(detailsMessage);
        duel.getTarget().sendMessage(acceptMessage);
        
        Component sentMessage = Component.text("Duel challenge sent to " + duel.getTarget().getName() + "!")
            .color(NamedTextColor.GREEN);
        player.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(sentMessage));
    }
    
    /**
     * Handle inventory close events
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !plugin.getGuiManager().hasGuiOpen(player)) {
            return;
        }
        plugin.getLogger().info("Received InventoryCloseEvent!");
        Player p = (Player) event.getPlayer();
        Duel duel = plugin.getDuelManager().getDuel(p);
        // Cancel any active cooldown if active
        if(plugin.getGuiManager().hasGuiOpen(p) && plugin.getGuiManager().getOpenGui(p).equals("betting")) {
            plugin.getBettingManager().unconfirmBet(duel, player);
        }
        if(plugin.getGuiManager().hasGuiOpen(p) && plugin.getGuiManager().getOpenGui(p).equals("prizes")) {
            plugin.getPrizeManager().updatePlayerPrizes(event.getInventory(), plugin.getGuiManager().getShownPrizes(player), player);
        }

        if(plugin.getGuiManager().hasGuiOpen(p) && plugin.getGuiManager().getOpenGui(p).equals("duel_setup")) {
            // Cancel any duel this player was about to make (only if setting up)
            if(duel != null && duel.getState() == Duel.DuelState.CREATING) {
                plugin.getDuelManager().cancelDuel(duel);
            }
        }
        plugin.getGuiManager().closeGui(player);
    }
}