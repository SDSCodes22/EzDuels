package com.ezduels.manager;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.listeners.PlayerListener;
import com.ezduels.model.Arena;
import com.ezduels.model.Duel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active duels and duel-related operations
 */
public class DuelManager {
    
    private final EzDuelsPlugin plugin;
    private final Map<UUID, Duel> activeDuels;
    private final Map<UUID, Duel> pendingDuels;
    private final Map<UUID, BukkitTask> countdownTasks;
    private final Map<UUID, Set<UUID>> skipVotes;
    private final Map<UUID, Location> previousLocation;

    public DuelManager(EzDuelsPlugin plugin) {
        this.plugin = plugin;
        this.activeDuels = new ConcurrentHashMap<>();
        this.pendingDuels = new ConcurrentHashMap<>();
        this.countdownTasks = new ConcurrentHashMap<>();
        this.skipVotes = new ConcurrentHashMap<>();
        this.previousLocation = new ConcurrentHashMap<>();
    }
    
    /**
     * Create a new duel challenge
     */
    public Duel createDuel(Player challenger, Player target) {
        // Check if either player is already in a duel
        if (isInDuel(challenger) || isInDuel(target)) {
            return null;
        }
        
        Duel duel = new Duel(challenger, target);
        pendingDuels.put(challenger.getUniqueId(), duel);
        pendingDuels.put(target.getUniqueId(), duel);
        
        return duel;
    }
    
    /**
     * Accept a duel challenge
     */
    public boolean acceptDuel(Player player) {
        Duel duel = pendingDuels.get(player.getUniqueId());
        if (duel == null || !duel.getTarget().equals(player)) {
            return false;
        }
        
        // Move from pending to active
        pendingDuels.remove(duel.getChallenger().getUniqueId());
        pendingDuels.remove(duel.getTarget().getUniqueId());
        
        duel.setState(Duel.DuelState.SETTING_UP);
        activeDuels.put(duel.getChallenger().getUniqueId(), duel);
        activeDuels.put(duel.getTarget().getUniqueId(), duel);
        
        return true;
    }

    /**
     * Deny a duel challenge
     */
    public boolean denyDuel(Player player) {
        Duel duel = pendingDuels.get(player.getUniqueId());
        if (duel == null || !duel.getTarget().equals(player)) {
            return false;
        }
        pendingDuels.remove(duel.getChallenger().getUniqueId());
        pendingDuels.remove(duel.getTarget().getUniqueId());
        cleanup(duel);
        return true;
    }

    /**
     * Start the duel countdown
     */
    public void startCountdown(Duel duel) {
        if (duel.getState() != Duel.DuelState.SETTING_UP) {
            return;
        }
        
        duel.setState(Duel.DuelState.COUNTDOWN);
        int countdownDuration = plugin.getConfig().getInt("duels.countdown-duration", 30);
        
        BukkitTask task = new BukkitRunnable() {
            int timeLeft = countdownDuration;
            
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    startFight(duel);
                    cancel();
                    return;
                }
                
                // Send countdown message to both players
                Component message = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.countdown", "<yellow>{seconds}s till fight. /skip to skip</yellow>")
                        .replace("{seconds}", String.valueOf(timeLeft))
                );
                
                duel.getChallenger().sendActionBar(message);
                duel.getTarget().sendActionBar(message);
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        countdownTasks.put(duel.getId(), task);
        skipVotes.put(duel.getId(), new HashSet<>());
    }
    
    /**
     * Vote to skip the countdown
     */
    public void skipCountdown(Player player) {
        Duel duel = activeDuels.get(player.getUniqueId());
        if (duel == null || duel.getState() != Duel.DuelState.COUNTDOWN) {
            return;
        }
        
        Set<UUID> votes = skipVotes.get(duel.getId());
        if (votes == null) {
            return;
        }
        
        votes.add(player.getUniqueId());
        
        // Check if both players have voted
        if (votes.contains(duel.getChallenger().getUniqueId()) && 
            votes.contains(duel.getTarget().getUniqueId())) {
            
            // Cancel countdown and start fight
            BukkitTask task = countdownTasks.remove(duel.getId());
            if (task != null) {
                task.cancel();
            }
            skipVotes.remove(duel.getId());
            
            startFight(duel);
        }
    }
    
    /**
     * Start the actual fight
     */
    private void startFight(Duel duel) {
        duel.setState(Duel.DuelState.FIGHTING);
        // Save their starting location
        previousLocation.put(duel.getChallenger().getUniqueId(), duel.getChallenger().getLocation());
        previousLocation.put(duel.getTarget().getUniqueId(), duel.getTarget().getLocation());

        // Get or assign arena
        if (duel.getArena() != null) {
            duel.getArena().setInUse(true);
            duel.getChallenger().teleport(duel.getArena().getSpawnPoint1());
            duel.getTarget().teleport(duel.getArena().getSpawnPoint2());
        } else {
            // Auto-assign an arena
            for (String arenaGroup : plugin.getArenaManager().getArenaGroups()) {
                Arena availableArena = plugin.getArenaManager().getAvailableArena(arenaGroup);
                if (availableArena != null) {
                    duel.setArena(availableArena);
                    availableArena.setInUse(true);
                    duel.getChallenger().teleport(availableArena.getSpawnPoint1());
                    duel.getTarget().teleport(availableArena.getSpawnPoint2());
                    break;
                }
            }
        }
        // Save the current state of the arena
        duel.getArena().saveState();

        // Set their health and saturation to full
        duel.getChallenger().setHealth(20);
        duel.getChallenger().setFoodLevel(20);
        duel.getChallenger().setSaturation(20);
        duel.getTarget().setHealth(20);
        duel.getTarget().setFoodLevel(20);
        duel.getTarget().setSaturation(20);

        // Set them to adventure mode
        duel.getChallenger().setGameMode(GameMode.ADVENTURE);
        duel.getTarget().setGameMode(GameMode.ADVENTURE);
        // Start PvP cooldown
        startPvpCooldown(duel);
    }
    
    /**
     * Start PvP cooldown before combat begins
     */
    private void startPvpCooldown(Duel duel) {
        new BukkitRunnable() {
            int timeLeft = 5;
            
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    // Enable PvP and send start message
                    Component message = Component.text("Fight started! May the best player win!")
                        .color(NamedTextColor.GREEN);

                    duel.getChallenger().setGameMode(GameMode.SURVIVAL);
                    duel.getTarget().setGameMode(GameMode.SURVIVAL);

                    duel.getChallenger().sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(message));
                    duel.getTarget().sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(message));
                    cancel();
                    return;
                }
                
                // Send cooldown message
                Component cooldownMessage = Component.text("PvP enabled in " + timeLeft + "s")
                    .color(NamedTextColor.YELLOW);
                
                duel.getChallenger().sendActionBar(cooldownMessage);
                duel.getTarget().sendActionBar(cooldownMessage);
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * End a duel with a winner
     */
    public void endDuel(Duel duel, Player winner, @Nullable List<ItemStack> drops) {
        duel.setState(Duel.DuelState.FINISHED);
        duel.setEndTime(System.currentTimeMillis());
        
        Player loser = duel.getOpponent(winner);
        // Set the drops to be the loser's inventory contents if drops is null and keep inventory is off
        if(!duel.isKeepInventory() && drops == null) {
            drops = Arrays.asList(loser.getInventory().getContents());
            loser.getInventory().clear();
        }
        // Send end messages
        Component winMessage = Component.text("You won the duel!")
            .color(net.kyori.adventure.text.format.NamedTextColor.GREEN).appendNewline();

        Component loseMessage = Component.text("You lost the duel!")
            .color(net.kyori.adventure.text.format.NamedTextColor.RED);
        
        winner.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(winMessage));
        if (loser != null) {
            loser.sendMessage(EzDuelsPlugin.getPluginPrefix().append(Component.space()).append(loseMessage));
        }
        
        // Update statistics
        plugin.getStatsManager().addWin(winner);
        if (loser != null) {
            plugin.getStatsManager().addLoss(loser);
        }
        
        // Handle prizes and inventory
        handleDuelEnd(duel, winner, loser, drops);
        teleportPlayersBack(duel, winner);
        // Clean up
        cleanup(duel);
    }
    
    /**
     * Cancel a duel
     */
    public void cancelDuel(Duel duel) {
        duel.setState(Duel.DuelState.CANCELLED);
        
        // Cancel any running tasks
        BukkitTask task = countdownTasks.remove(duel.getId());
        if (task != null) {
            task.cancel();
        }
        skipVotes.remove(duel.getId());
        
        // Clean up betting
        if (duel.isBettingEnabled()) {
            plugin.getBettingManager().returnBetItems(duel);
            plugin.getBettingManager().cleanupBetting(duel);
        }
        
        // Clean up
        cleanup(duel);
    }

    /**
     * Handle teleporting the victor and loser to their original location after the duel has ended
     */
    private void teleportPlayersBack(Duel duel, Player winner) {
        // Wait for 10 seconds before teleporting them back
        new BukkitRunnable() {
            int timeLeft = 10;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    // Set the loser to survival and teleport them back
                    Player loser = duel.getOpponent(winner);
                    loser.setGameMode(GameMode.SURVIVAL);

                    // Teleport them both back
                    winner.teleportAsync(previousLocation.get(winner.getUniqueId()));
                    // Loser could be offline
                    if(loser.isOnline()) {
                        loser.teleportAsync(previousLocation.get(loser.getUniqueId()));
                    } else {
                        // Add them to teleportation queue to teleport them later
                        PlayerListener.addToTeleportationQueue(loser, previousLocation.get(loser.getUniqueId()));
                        plugin.getLogger().info("Added player to teleportation queue");
                    }
                    // Pop them from the list
                    previousLocation.remove(winner.getUniqueId());
                    previousLocation.remove(loser.getUniqueId());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Release arena
                            if (duel.getArena() != null) {
                                duel.getArena().regenerate();
                                duel.getArena().setInUse(false);
                            }
                            plugin.getLogger().info("Reset arena!");
                        }
                    }.runTaskLater(plugin, 5L); // Run later so players are definitely teleported away before arena is reset.
                    cancel();
                    return;
                }

                // Send cooldown message
                Component cooldownMessage = Component.text("Arena Closing in " + timeLeft + "s")
                        .color(NamedTextColor.YELLOW);

                duel.getChallenger().sendActionBar(cooldownMessage);
                duel.getTarget().sendActionBar(cooldownMessage);

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    /**
     * Handle duel end logic (prizes, inventory, etc.)
     */
    private void handleDuelEnd(Duel duel, Player winner, Player loser, List<ItemStack> droppedItems) {
        // Handle inventory drops if keep inventory is off and loser exists
        if (!duel.isKeepInventory() && loser != null) {
            // Clear loser's inventory
            loser.getInventory().clear(); // (Redundancy)
            // Add items to prizes
            plugin.getPrizeManager().addPrize(winner, droppedItems);
        }
        
        // Handle betting if enabled
        if (duel.isBettingEnabled()) {
            plugin.getBettingManager().processBetWin(duel, winner);
        }
        // NOTE: Release arena after players are teleported away
    }
    
    /**
     * Clean up duel data
     */
    private void cleanup(Duel duel) {
        activeDuels.remove(duel.getChallenger().getUniqueId());
        activeDuels.remove(duel.getTarget().getUniqueId());
        pendingDuels.remove(duel.getChallenger().getUniqueId());
        pendingDuels.remove(duel.getTarget().getUniqueId());
        
        BukkitTask task = countdownTasks.remove(duel.getId());
        if (task != null) {
            task.cancel();
        }
        skipVotes.remove(duel.getId());
    }
    
    /**
     * Check if a player is in a duel
     */
    public boolean isInDuel(Player player) {
        return activeDuels.containsKey(player.getUniqueId()) || 
               pendingDuels.containsKey(player.getUniqueId());
    }
    
    /**
     * Get a player's active duel
     */
    public Duel getDuel(Player player) {
        Duel duel = activeDuels.get(player.getUniqueId());
        if (duel == null) {
            duel = pendingDuels.get(player.getUniqueId());
        }
        return duel;
    }
    
    /**
     * Clean up all duels (for plugin shutdown)
     */
    public void cleanup() {
        // Cancel all running tasks
        for (BukkitTask task : countdownTasks.values()) {
            task.cancel();
        }
        countdownTasks.clear();
        
        // Clean up all data
        activeDuels.clear();
        pendingDuels.clear();
        skipVotes.clear();
    }
    
    /**
     * Get all active duels
     */
    public Collection<Duel> getActiveDuels() {
        return new HashSet<>(activeDuels.values());
    }
}