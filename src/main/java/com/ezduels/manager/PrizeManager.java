package com.ezduels.manager;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Prize;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player prizes and prize expiration
 */
public class PrizeManager {
    
    private final EzDuelsPlugin plugin;
    private final Map<UUID, List<Prize>> playerPrizes;
    private BukkitTask reminderTask;
    private BukkitTask cleanupTask;
    
    public PrizeManager(EzDuelsPlugin plugin) {
        this.plugin = plugin;
        this.playerPrizes = new ConcurrentHashMap<>();
        
        startReminderTask();
        startCleanupTask();
    }
    
    /**
     * Add a prize for a player
     */
    public void addPrize(Player player, List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        long expirationTime = System.currentTimeMillis() + 
            (plugin.getConfig().getInt("prizes.expiration-time", 3600) * 1000L);
        
        Prize prize = new Prize(player.getUniqueId(), items, expirationTime);
        
        playerPrizes.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(prize);
        
        // Notify player
        Component message = Component.text("You have received prizes! Use /prizes to claim them.")
            .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW);
        player.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(message));
    }
    
    /**
     * Get all prizes for a player
     */
    public List<Prize> getPrizes(Player player) {
        return playerPrizes.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    /**
     * Auto-Remove Prizes by comparing the current opened prize inventory to the
     */
    public void updatePlayerPrizes(Inventory currentPrizeInventory, List<ItemStack> originalPrizeItems, Player player) {
        // Working with inventory is easier for computing. Convert back to List<ItemStack> later.
        Inventory currentPrizeItems = extractPrizes(currentPrizeInventory);
        List<ItemStack> addedItems = new ArrayList<>();
        // If an item in the currentPrizeItems does not have a custom PDC, it has been added by the player
        for ( ItemStack item : currentPrizeItems.getContents()) {
            if(item == null || item.isEmpty()) continue;
            if(!item.getPersistentDataContainer().has(Prize.persistentKey)) {
                addedItems.add(item);
                currentPrizeItems.removeItem(item);
            }
        }

        Inventory takenPrizes = Bukkit.createInventory(null, 45);
        // Casting in Java is weird...
        ItemStack[] originalItems = new ItemStack[originalPrizeItems.size()];
        originalItems = originalPrizeItems.toArray(originalItems);
        takenPrizes.setContents(originalItems);

        // current prizes holds section / entirety of originalPrizes, so remove iteratively from taken prizes
        for( ItemStack item : currentPrizeItems) {
            if(item == null || item.isEmpty()) continue;
            HashMap<Integer, ItemStack> failedRemovalItems = takenPrizes.removeItem(item);
            if(!failedRemovalItems.isEmpty()) {
                // Illegal state: current prize items shouldn't have more
                // Could be possbile player added item of same type, so add this to addedItems
                // TODO: Possible Problematic Code
                plugin.getLogger().warning("When withdrawing prizes, current prizes exceed original prizes! Illegal State, may cause bugs!");
                addedItems.addAll(failedRemovalItems.values());
            }
        }
        // takenPrizes now holds the difference i.e. everything in originalPrizeItems NOT in currentPrizeItems

        //      1. Remove prizes from prizes by reading PDCs to get class reference.
        // Compute hashmap with UUID-Prizes for easy lookup
        List<Prize> prizes = playerPrizes.get(player.getUniqueId());
        Map<UUID, Prize> uuidPrizeHashMap = new ConcurrentHashMap<>();
        for(Prize prize : prizes) {
            uuidPrizeHashMap.put(prize.getPrizeId(), prize);
        }
        for( ItemStack itemStack : takenPrizes.getContents() ) {
            if(itemStack == null || itemStack.isEmpty()) continue;
            String uuidStr = itemStack.getPersistentDataContainer().get(Prize.persistentKey, PersistentDataType.STRING);
            if(uuidStr == null) {
                throw new IllegalStateException("Persistent Key in Taken Prize not Found.");
            }
            Prize associatedPrize = uuidPrizeHashMap.get(UUID.fromString(uuidStr));
            if(associatedPrize == null) {
                throw new IllegalStateException("Associated Prize with Persistent Data not found.");
            }
            withdrawItem(player, associatedPrize, itemStack);
        }

        //      2. Give Back added items back to the player
        // plugin.getLogger().info("Length of items added:" + addedItems.size());
        for( ItemStack itemStack : addedItems ) {
            Map<Integer, ItemStack> excess = player.getInventory().addItem(itemStack);
            for (ItemStack leftover : excess.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        // Remove the Persistent Tag from taken prize items.
        sanitizePlayerInventory(player);
    }
    /**
     * Remove Prize Persistent Data from items in player's inventory
     */
    private void sanitizePlayerInventory(Player player) {
        ItemStack[] inventoryItems = player.getInventory().getContents();
        for( ItemStack item : inventoryItems ) {
            if(item == null || item.isEmpty()) continue;
            item.editMeta(meta -> {
               meta.getPersistentDataContainer().remove(Prize.persistentKey);
            });
        }
        player.getInventory().setContents(inventoryItems);
    }
    /**
     * Remove a prize from a player's collection
     */
    public void removePrize(Player player, Prize prize) {
        List<Prize> prizes = playerPrizes.get(player.getUniqueId());
        if (prizes != null) {
            prizes.remove(prize);
            if (prizes.isEmpty()) {
                playerPrizes.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Withdraw an item from a prize
     */
    public void withdrawItem(Player player, Prize prize, ItemStack item) {
        // Sanitize the item to remove PDCs
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().remove(Prize.persistentKey);
        });
        // DEBUG
        plugin.getLogger().info("Stack size of taken item:" + item.getAmount());
        // Not partial item
        if (prize.removeItem(item)) {
            // Remove prize if empty
            if (prize.isEmpty()) {
                removePrize(player, prize);
            }
        }
    }

    /**
     * Check if a player has prizes
     */
    public boolean hasPrizes(Player player) {
        List<Prize> prizes = playerPrizes.get(player.getUniqueId());
        return prizes != null && !prizes.isEmpty();
    }
    
    /**
     * Start the reminder task
     */
    private void startReminderTask() {
        int interval = plugin.getConfig().getInt("prizes.reminder-interval", 300);
        
        reminderTask = new BukkitRunnable() {
            @Override
            public void run() {
                sendPrizeReminders();
            }
        }.runTaskTimer(plugin, interval * 20L, interval * 20L);
    }
    
    /**
     * Start the cleanup task
     */
    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredPrizes();
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Every minute
    }
    
    /**
     * Send prize reminders to players
     */
    private void sendPrizeReminders() {
        Component message = MiniMessage.miniMessage().deserialize(
            plugin.getConfig().getString("messages.prize-reminder", 
                "<gray>[EzDuels] You have unclaimed prizes. Use /prizes to view them before they expire.")
        );
        
        for (UUID playerId : playerPrizes.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * Clean up expired prizes
     */
    private void cleanupExpiredPrizes() {
        Iterator<Map.Entry<UUID, List<Prize>>> playerIterator = playerPrizes.entrySet().iterator();
        
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, List<Prize>> entry = playerIterator.next();
            List<Prize> prizes = entry.getValue();
            
            prizes.removeIf(Prize::isExpired);
            
            if (prizes.isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    /**
     * Helper function to remove the bottom row from the opened prize GUI
     */
    private Inventory extractPrizes(Inventory prizeInventory) {
        Inventory outputInventory = Bukkit.createInventory(null, 45); // This is a "virtual inventory", so no holder
        for(int i = 0; i < 45; i++) {
            if(prizeInventory.getItem(i) == null || prizeInventory.getItem(i).isEmpty()) continue;
            outputInventory.addItem(prizeInventory.getItem(i));
        }
        return outputInventory;
    }

    /**
     * Shutdown the prize manager
     */
    public void shutdown() {
        if (reminderTask != null) {
            reminderTask.cancel();
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
    }
}