package com.ezduels.gui;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Duel;
import com.ezduels.model.Prize;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages GUI creation and interactions
 */
public class GuiManager {
    
    private final EzDuelsPlugin plugin;
    private final Map<UUID, String> openGuis;
    private final Map<UUID, Integer> guiPages;
    private final Map<UUID, List<ItemStack>> shownPrizeItems; // Stores a list of the prize items the player is currently being shown. Used to detect when prizes are taken out
    public GuiManager(EzDuelsPlugin plugin) {
        this.plugin = plugin;
        this.openGuis = new ConcurrentHashMap<>();
        this.guiPages = new ConcurrentHashMap<>();
        this.shownPrizeItems = new ConcurrentHashMap<>();
    }
    
    /**
     * Open the duel setup GUI
     */
    public void openDuelSetupGui(Player player, Duel duel) {
        Inventory gui = getDuelSetupGui(player, duel);
        player.openInventory(gui);
        openGuis.put(player.getUniqueId(), "duel_setup");
    }

    /**
     * Refresh the duel setup GUI (avoid InventoryCloseEvent)
     */
    public void refreshDuelSetupGui(Player player, Duel duel) {
        if(!hasGuiOpen(player) || !Objects.equals(getOpenGui(player), "duel_setup")) {
            return; // No bother doing anything if the menu isn't even open
        }
        Inventory openDuelsGui = player.getOpenInventory().getTopInventory();
        Inventory currentDuelsGui = getDuelSetupGui(player, duel);
        openDuelsGui.setContents(currentDuelsGui.getContents());
    }
    private Inventory getDuelSetupGui(Player player, Duel duel) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Duel Setup"));

        // Keep Inventory toggle
        ItemStack keepInventoryItem = new ItemStack(duel.isKeepInventory() ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta keepInventoryMeta = keepInventoryItem.getItemMeta();
        keepInventoryMeta.displayName(Component.text("Keep Inventory: " + (duel.isKeepInventory() ? "ON" : "OFF"))
                .color(duel.isKeepInventory() ? NamedTextColor.GREEN : NamedTextColor.RED));
        keepInventoryItem.setItemMeta(keepInventoryMeta);
        gui.setItem(11, keepInventoryItem);

        // Betting toggle
        ItemStack bettingItem = new ItemStack(duel.isBettingEnabled() ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta bettingMeta = bettingItem.getItemMeta();
        bettingMeta.displayName(Component.text("Betting: " + (duel.isBettingEnabled() ? "ENABLED" : "DISABLED"))
                .color(duel.isBettingEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        bettingItem.setItemMeta(bettingMeta);
        gui.setItem(13, bettingItem);

        // Arena selection
        ItemStack arenaItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta arenaMeta = arenaItem.getItemMeta();
        String arenaName = duel.getArena() != null ? duel.getArena().getBasename() : "AUTO";
        arenaMeta.displayName(Component.text("Arena: " + arenaName).color(NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to cycle through available arenas").color(NamedTextColor.GRAY));
        arenaMeta.lore(lore);
        arenaItem.setItemMeta(arenaMeta);
        gui.setItem(15, arenaItem);

        // Confirm button
        ItemStack confirmItem = new ItemStack(Material.EMERALD);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(Component.text("Confirm Duel").color(NamedTextColor.GREEN));
        confirmItem.setItemMeta(confirmMeta);
        gui.setItem(22, confirmItem);
        return gui;
    }
    /**
     * Open the betting GUI
     */
    public void openBettingGui(Player player, Duel duel) {
        Inventory betGui = getBettingGui(player, duel);
        player.openInventory(betGui);
        openGuis.put(player.getUniqueId(), "betting");
    }

    /**
     * Updates the current bet GUI with latest information, without closing the inventory. Opens bet GUI if not already
     * open.
     * @param player The Player with the GUI open
     * @param duel The duel, which references the player
     */
    public void refreshBetGui(Player player, Duel duel) {
        if(!hasGuiOpen(player) || !Objects.equals(getOpenGui(player), "betting")) {
            return; // No bother doing anything if the menu isn't even open
        }
        Inventory openBettingGui = player.getOpenInventory().getTopInventory();
        Inventory currentBettingGui = getBettingGui(player, duel);
        openBettingGui.setContents(currentBettingGui.getContents());
    }

    /**
     * Get the Betting GUI (Helper function)
     */
    private Inventory getBettingGui(Player player, Duel duel) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("    Your Bet         Their Bet"));

        // Gray stained glass divider in center column
        ItemStack divider = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta dividerMeta = divider.getItemMeta();
        dividerMeta.displayName(Component.text(" "));
        divider.setItemMeta(dividerMeta);

        for (int i = 4; i < 54; i += 9) {
            gui.setItem(i, divider);
        }

        // Load existing bets
        List<ItemStack> playerBet = plugin.getBettingManager().getBet(duel, player);
        List<ItemStack> opponentBet = plugin.getBettingManager().getBet(duel, duel.getOpponent(player));

        // Place this player's bet items (left side)
        for (int i = 0; i < playerBet.size() && i < 32; i++) {
            int slot = getLeftSlot(i);
            if (slot != -1) {
                gui.setItem(slot, playerBet.get(i).clone());
            }
        }

        // Place opponent's bet items (right side)
        for (int i = 0; i < opponentBet.size() && i < 20; i++) {
            int slot = getRightSlot(i);
            if (slot != -1) {
                // Set custom item metadata to prevent exploits
                ItemStack opponentItem = opponentBet.get(i).clone();
                ItemMeta itemMeta = opponentItem.getItemMeta();
                itemMeta.setLore(Arrays.asList( // TODO: Figure out how to do this with Adventure API
                        "",
                        ChatColor.YELLOW + "Opponent has put this up to bet."
                ));
                opponentItem.setItemMeta(itemMeta);
                gui.setItem(slot, opponentItem);
            }
        }
        // Fill remaining with Light Gray Stained Glass Panes (avoid exploits)
        for(int i = opponentBet.size(); i < 20; i++) {
            int slot = getRightSlot(i);
            if (slot != -1) {
                ItemStack itemStack = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 1);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.displayName(Component.text(" "));
                itemStack.setItemMeta(itemMeta);
                gui.setItem(slot, itemStack);
            }
        }

        // Confirmation buttons
        boolean playerConfirmed = plugin.getBettingManager().isConfirmed(duel, player);
        boolean opponentConfirmed = plugin.getBettingManager().isConfirmed(duel, duel.getOpponent(player));

        ItemStack playerConfirm = new ItemStack(playerConfirmed ? Material.GREEN_STAINED_GLASS : Material.RED_STAINED_GLASS);
        ItemMeta playerConfirmItemMeta = playerConfirm.getItemMeta();
        playerConfirmItemMeta.displayName(Component.text(player.getName() + " - " +
                        (playerConfirmed ? "Confirmed" : "Not Confirmed"))
                .color(playerConfirmed ? NamedTextColor.GREEN : NamedTextColor.RED));
        playerConfirm.setItemMeta(playerConfirmItemMeta);
        gui.setItem(45, playerConfirm);

        ItemStack opponentConfirm = new ItemStack(opponentConfirmed ? Material.GREEN_STAINED_GLASS : Material.RED_STAINED_GLASS);
        ItemMeta opponentConfirmItemMeta = opponentConfirm.getItemMeta();
        opponentConfirmItemMeta.displayName(Component.text(duel.getOpponent(player).getName() + " - " +
                        (opponentConfirmed ? "Confirmed" : "Not Confirmed"))
                .color(opponentConfirmed ? NamedTextColor.GREEN : NamedTextColor.RED));
        opponentConfirm.setItemMeta(opponentConfirmItemMeta);
        gui.setItem(53, opponentConfirm);

        // Display the current countdown stage, if not 0
        int countdownStage = plugin.getBettingManager().getCountdownStage(duel);
        if(countdownStage > 0) {
            // Left side
            for(int i = 46; i < (46 + countdownStage ); i ++) {
                ItemStack confirmItem = new ItemStack(Material.GREEN_STAINED_GLASS);
                gui.setItem(i, confirmItem);
            }

            // Right side
            for(int i = 52; i > (52 - countdownStage); i --) {
                ItemStack confirmItem = new ItemStack(Material.GREEN_STAINED_GLASS);
                gui.setItem(i, confirmItem);
            }
        }
        return gui;
    }

    /**
     * Open the prizes GUI
     */
    public void openPrizesGui(Player player, int page) {
        List<Prize> prizes = plugin.getPrizeManager().getPrizes(player);
        if (prizes.isEmpty()) {
            player.sendMessage(Component.text("You have no prizes to claim!").color(NamedTextColor.RED));
            return;
        }
        
        int itemsPerPage = 45;
        int maxPages = (int) Math.ceil((double) getTotalPrizeItems(prizes) / itemsPerPage);
        page = Math.max(1, Math.min(page, maxPages));
        
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("Prizes - Page " + page + "/" + maxPages));
        // Reset current prize items if exists
        shownPrizeItems.remove(player.getUniqueId());
        // Add prize items
        List<ItemStack> allItems = new ArrayList<>();
        for (Prize prize : prizes) {
            allItems.addAll(prize.getItems());
        }
        shownPrizeItems.put(player.getUniqueId(), allItems);

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            gui.setItem(i - startIndex, allItems.get(i));
        }
        
        // Navigation buttons
        if (page > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            prevMeta.displayName(Component.text("Previous Page").color(NamedTextColor.YELLOW));
            prevPage.setItemMeta(prevMeta);
            gui.setItem(48, prevPage);
        }
        
        if (page < maxPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.displayName(Component.text("Next Page").color(NamedTextColor.YELLOW));
            nextPage.setItemMeta(nextMeta);
            gui.setItem(50, nextPage);
        }
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED));
        closeItem.setItemMeta(closeMeta);
        gui.setItem(49, closeItem);
        
        player.openInventory(gui);
        openGuis.put(player.getUniqueId(), "prizes");
        guiPages.put(player.getUniqueId(), page);
    }


    /**
     * Get the left side slot for betting GUI
     */
    private int getLeftSlot(int index) {
        int row = index / 4;
        int col = index % 4;
        int slot = row * 9 + col;
        return slot < 45 ? slot : -1;
    }
    
    /**
     * Get the right side slot for betting GUI
     */
    private int getRightSlot(int index) {
        int row = index / 4;
        int col = index % 4;
        int slot = row * 9 + col + 5;
        return slot < 45 ? slot : -1;
    }
    
    /**
     * Get total number of items in all prizes
     */
    private int getTotalPrizeItems(List<Prize> prizes) {
        return prizes.stream().mapToInt(prize -> prize.getItems().size()).sum();
    }
    
    /**
     * Get the GUI type a player has open
     */
    public String getOpenGui(Player player) {
        return openGuis.get(player.getUniqueId());
    }
    
    /**
     * Get the GUI page a player is on
     */
    public int getGuiPage(Player player) {
        return guiPages.getOrDefault(player.getUniqueId(), 1);
    }

    /**
     * Get the list of ItemStacks of prizes shown to the current player
     */
    public List<ItemStack> getShownPrizes(Player player) { return shownPrizeItems.get(player.getUniqueId()); }
    public List<ItemStack> getShownPrizes(UUID playerUuid) { return shownPrizeItems.get(playerUuid); }

    /**
     * Close a player's GUI
     */
    public void closeGui(Player player) {
        openGuis.remove(player.getUniqueId());
        guiPages.remove(player.getUniqueId());
        shownPrizeItems.remove(player.getUniqueId());
    }
    
    /**
     * Check if a player has a GUI open
     */
    public boolean hasGuiOpen(Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }
}