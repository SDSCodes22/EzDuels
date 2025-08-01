package com.ezduels.model;

import com.ezduels.EzDuelsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import javax.xml.stream.events.Namespace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Represents a prize collection for a player
 */
public class Prize {

    public static final NamespacedKey  persistentKey = new NamespacedKey(EzDuelsPlugin.getInstance(), "prize-id");
    private final UUID playerId;
    private final List<ItemStack> items;
    private final long createdTime;
    private final long expirationTime;
    private final UUID prizeId;

    public Prize(UUID playerId, List<ItemStack> items, long expirationTime) {
        this.playerId = playerId;
        this.prizeId = UUID.randomUUID();
        this.items = new ArrayList<>(items);
        this.createdTime = System.currentTimeMillis();
        this.expirationTime = expirationTime;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getPrizeId() { return prizeId; }

    /**
     * Returns a list of items with Persistent Data attached
     * */
    public List<ItemStack> getItems() {
        List<ItemStack> outItems = new ArrayList<>();
        for( ItemStack item : items) {
            if(item == null) continue;
            ItemStack clonedItem = item.clone(); // Clone so we keep a clean copy in the class instance to avoid unexpected behaviour.
            clonedItem.editMeta( meta -> {
                meta.getPersistentDataContainer().set(persistentKey, PersistentDataType.STRING, prizeId.toString());
            });
            outItems.add(clonedItem);
        }
        return outItems;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public long getExpirationTime() {
        return expirationTime;
    }
    
    /**
     * Check if this prize has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
    
    /**
     * Get the time remaining until expiration in seconds
     */
    public long getTimeRemaining() {
        return Math.max(0, (expirationTime - System.currentTimeMillis()) / 1000);
    }
    
    /**
     * Remove an item from the prize collection. Now supports partial removal
     */
    public boolean removeItem(ItemStack item) {
        // Temporarily create inventory to support partial removal
        Inventory tempInventory = Bukkit.createInventory(null, ((items.size() / 9) + 1) * 9);
        ItemStack[] contents = new ItemStack[items.size()];
        contents = items.toArray(contents);
        tempInventory.setContents(contents);
        System.out.println("b4 tempinv contents: " + Arrays.toString(tempInventory.getContents()));
        System.out.println("item to rm: " + item);

        tempInventory.removeItem(item);
        System.out.println("tempinv contents: " + Arrays.toString(tempInventory.getContents()));
        List<ItemStack> outputItems = new ArrayList<>();
        for ( ItemStack finalItem : tempInventory.getContents() ) {
            if(finalItem == null || finalItem.isEmpty()) continue;
            outputItems.add(finalItem);
        }

        System.out.println("outputitems contents: " + outputItems);

        items.clear();
        items.addAll(outputItems);

        System.out.println("items contents: " + items);
        return true;
    }
    
    /**
     * Check if the prize is empty
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
}