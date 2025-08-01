package com.ezduels.manager;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Duel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages betting system for duels
 */
public class BettingManager {
    
    private final EzDuelsPlugin plugin;
    private final Map<UUID, Map<Player, List<ItemStack>>> activeBets;
    private final Map<UUID, Set<Player>> confirmedBets;
    private final Map<UUID, BukkitTask> betTasks;
    private final Map<UUID, BukkitTask> reminderTasks;
    private final Map<UUID, BukkitTask> confirmationCountdownTasks;
    private final Map<UUID, Integer> countdownStages;

    public BettingManager(EzDuelsPlugin plugin) {
        this.plugin = plugin;
        this.activeBets = new ConcurrentHashMap<>();
        this.confirmedBets = new ConcurrentHashMap<>();
        this.betTasks = new ConcurrentHashMap<>();
        this.reminderTasks = new ConcurrentHashMap<>();
        this.confirmationCountdownTasks = new ConcurrentHashMap<>();
        this.countdownStages = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize betting for a duel
     */
    public void initializeBetting(Duel duel) {
        if (!duel.isBettingEnabled()) {
            return;
        }
        
        UUID duelId = duel.getId();
        activeBets.put(duelId, new ConcurrentHashMap<>());
        confirmedBets.put(duelId, new HashSet<>());
        
        // Initialize empty bet lists
        activeBets.get(duelId).put(duel.getChallenger(), new ArrayList<>());
        activeBets.get(duelId).put(duel.getTarget(), new ArrayList<>());
        
        // Start bet timeout task
        startBetTimeout(duel);
        
        // Start reminder task
        startReminderTask(duel);
    }
    
    /**
     * Start the bet timeout task
     */
    private void startBetTimeout(Duel duel) {
        int duration = plugin.getConfig().getInt("duels.bet-menu-duration", 300);
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Timeout betting
                timeoutBetting(duel);
            }
        }.runTaskLater(plugin, duration * 20L);
        
        betTasks.put(duel.getId(), task);
    }
    
    /**
     * Start the reminder task
     */
    private void startReminderTask(Duel duel) {
        int interval = plugin.getConfig().getInt("duels.bet-reminder-interval", 5);
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                sendBetReminder(duel);
            }
        }.runTaskTimer(plugin, interval * 20L, interval * 20L);
        
        reminderTasks.put(duel.getId(), task);
    }
    
    /**
     * Send bet reminder to players
     */
    private void sendBetReminder(Duel duel) {
        Component message = MiniMessage.miniMessage().deserialize(
            plugin.getConfig().getString("messages.bet-reminder", 
                "<gray>[EzDuels] You have an active bet menu. Click here or type /betmenu to reopen.")
        );
        // Conditionally send betting menu reminder if not already open.
        if(Objects.equals(plugin.getGuiManager().getOpenGui(duel.getChallenger()), "betting"))
            duel.getChallenger().sendMessage(message);
        if(Objects.equals(plugin.getGuiManager().getOpenGui(duel.getTarget()), "betting"))
            duel.getTarget().sendMessage(message);
    }
    
    /**
     * Update a player's bet
     */
    public void updateBet(Duel duel, Player player, List<ItemStack> items) {
        Map<Player, List<ItemStack>> bets = activeBets.get(duel.getId());
        if (bets == null) {
            return;
        }
        
        bets.put(player, new ArrayList<>(items));
        
        // Reset confirmations when bets change
        confirmedBets.get(duel.getId()).clear();
        cancelConfirmCountdownTask(duel);

    }
    
    /**
     * Confirm a player's bet
     */
    public void confirmBet(Duel duel, Player player) {
        Set<Player> confirmed = confirmedBets.get(duel.getId());
        if (confirmed == null) {
            return;
        }
        
        confirmed.add(player);
        
        // Check if both players have confirmed
        if (confirmed.contains(duel.getChallenger()) && confirmed.contains(duel.getTarget())) {
            startConfirmCountdownTask(duel);
        }
    }
    /**
     * Unconfirm a player's bet
     */
    public void unconfirmBet(Duel duel, Player player) {
        Set<Player> confirmed = confirmedBets.get(duel.getId());
        if (confirmed == null) {
            return;
        }
        cancelConfirmCountdownTask(duel);
        confirmed.remove(player);
    }
    
    /**
     * Check if a player has confirmed their bet
     */
    public boolean isConfirmed(Duel duel, Player player) {
        Set<Player> confirmed = confirmedBets.get(duel.getId());
        return confirmed != null && confirmed.contains(player);
    }
    /**
     * Start a cooldown for the betting
     */
    public void startConfirmCountdownTask(Duel duel) {
        BukkitTask task = new BukkitRunnable() {
            int countdownStage = 0;
            @Override
            public void run() {
                countdownStage += 1;
                if(countdownStage > 3) {
                    // Clean up
                    finalizeBetting(duel);
                    countdownStages.remove(duel.getId());
                    confirmationCountdownTasks.remove(duel.getId());
                    cancel();
                }
                countdownStages.put(duel.getId(), countdownStage);
                // Refresh GUIs
                plugin.getGuiManager().refreshBetGui(duel.getChallenger(), duel);
                plugin.getGuiManager().refreshBetGui(duel.getTarget(), duel);
            }
        }.runTaskTimer(plugin, 20L, 20L);

        confirmationCountdownTasks.put(duel.getId(), task);
    }
    /**
     * Cancel a confirmation countdown task
     */
    public void cancelConfirmCountdownTask(Duel duel) {
        BukkitTask countdownTask = confirmationCountdownTasks.get(duel.getId());
        if(countdownTask != null) {
            countdownTask.cancel();
        }
        // cleanup
        confirmationCountdownTasks.remove(duel.getId());
        countdownStages.remove(duel.getId());
        // Update GUI of both players
        plugin.getGuiManager().refreshBetGui(duel.getChallenger(), duel);
        plugin.getGuiManager().refreshBetGui(duel.getTarget(), duel);
    }
    /**
     * Get a player's current bet
     */
    public List<ItemStack> getBet(Duel duel, Player player) {
        Map<Player, List<ItemStack>> bets = activeBets.get(duel.getId());
        if (bets == null) {
            return new ArrayList<>();
        }
        return bets.getOrDefault(player, new ArrayList<>());
    }
    
    /**
     * Finalize betting and start the duel
     */
    private void finalizeBetting(Duel duel) {
        // Cancel timeout and reminder tasks
        BukkitTask betTask = betTasks.remove(duel.getId());
        if (betTask != null) {
            betTask.cancel();
        }
        
        BukkitTask reminderTask = reminderTasks.remove(duel.getId());
        if (reminderTask != null) {
            reminderTask.cancel();
        }
        // Close both of their inventories
        duel.getTarget().closeInventory();
        duel.getChallenger().closeInventory();


        // Start the duel countdown
        plugin.getDuelManager().startCountdown(duel);
    }
    
    /**
     * Remove bet items from player inventories
     * - UNUSED -
     */
    private void removeBetItems(Duel duel) {
        Map<Player, List<ItemStack>> bets = activeBets.get(duel.getId());
        if (bets == null) {
            return;
        }
        
        for (Map.Entry<Player, List<ItemStack>> entry : bets.entrySet()) {
            Player player = entry.getKey();
            List<ItemStack> items = entry.getValue();
            
            for (ItemStack item : items) {
                player.getInventory().removeItem(item);
            }
        }
    }
    
    /**
     * Timeout betting (players took too long)
     */
    private void timeoutBetting(Duel duel) {
        // Return items to players
        returnBetItems(duel);
        
        // Cancel the duel
        plugin.getDuelManager().cancelDuel(duel);
        
        // Clean up
        cleanupBetting(duel);
    }
    
    /**
     * Return bet items to players
     */
    public void returnBetItems(Duel duel) {
        Map<Player, List<ItemStack>> bets = activeBets.get(duel.getId());
        if (bets == null) {
            return;
        }
        
        for (Map.Entry<Player, List<ItemStack>> entry : bets.entrySet()) {
            Player player = entry.getKey();
            List<ItemStack> items = entry.getValue();
            
            for (ItemStack item : items) {
                if (player.getInventory().firstEmpty() == -1) {
                    // Inventory full, add to prizes
                    plugin.getPrizeManager().addPrize(player, Arrays.asList(item));
                } else {
                    player.getInventory().addItem(item);
                }
            }
        }
    }
    
    /**
     * Process bet win (give items to winner)
     */
    public void processBetWin(Duel duel, Player winner) {
        Map<Player, List<ItemStack>> bets = activeBets.get(duel.getId());
        if (bets == null) {
            return;
        }
        // Collect all ItemStacks
        List<ItemStack> allItems = new ArrayList<>();
        for(Map.Entry<Player, List<ItemStack>> entry : bets.entrySet()) {
            allItems.addAll(entry.getValue());
        }
        // Add to winner's prizes
        plugin.getPrizeManager().addPrize(winner, allItems);
        // Cheeky message to the loser
        duel.getOpponent(winner).sendMessage(
                EzDuelsPlugin.getPluginPrefix().append(
                        Component.text("You lost everything you bet!", NamedTextColor.RED)));
        // Clean up
        cleanupBetting(duel);
    }
    
    /**
     * Clean up betting data
     */
    public void cleanupBetting(Duel duel) {
        UUID duelId = duel.getId();
        // Check if any of the players have the betting GUI open. If so, close it for them
        Player challenger = duel.getChallenger();
        Player target = duel.getTarget();
        if(plugin.getGuiManager().hasGuiOpen(target) && plugin.getGuiManager().getOpenGui(target).equals("betting")) {
            target.closeInventory();
        }
        if(plugin.getGuiManager().hasGuiOpen(challenger) && plugin.getGuiManager().getOpenGui(challenger).equals("betting")) {
            challenger.closeInventory();
        }
        activeBets.remove(duelId);
        confirmedBets.remove(duelId);
        
        BukkitTask betTask = betTasks.remove(duelId);
        if (betTask != null) {
            betTask.cancel();
        }
        
        BukkitTask reminderTask = reminderTasks.remove(duelId);
        if (reminderTask != null) {
            reminderTask.cancel();
        }
    }
    
    /**
     * Check if betting is active for a duel
     */
    public boolean isBettingActive(Duel duel) {
        return activeBets.containsKey(duel.getId());
    }

    public int getCountdownStage(Duel duel) { return countdownStages.getOrDefault(duel.getId(), 0); }
}