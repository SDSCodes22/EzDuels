package com.ezduels.model;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents a duel between two players
 */
public class Duel {
    
    private final UUID id;
    private final Player challenger;
    private final Player target;
    private boolean keepInventory;
    private boolean bettingEnabled;
    private Arena arena;
    private DuelState state;
    private long startTime;
    private long endTime;
    
    public Duel(Player challenger, Player target) {
        this.id = UUID.randomUUID();
        this.challenger = challenger;
        this.target = target;
        this.keepInventory = true;
        this.bettingEnabled = false;
        this.state = DuelState.CREATING;
        this.startTime = System.currentTimeMillis();
    }
    
    public UUID getId() {
        return id;
    }
    
    public Player getChallenger() {
        return challenger;
    }
    
    public Player getTarget() {
        return target;
    }
    
    public boolean isKeepInventory() {
        return keepInventory;
    }
    
    public void setKeepInventory(boolean keepInventory) {
        this.keepInventory = keepInventory;
    }
    
    public boolean isBettingEnabled() {
        return bettingEnabled;
    }
    
    public void setBettingEnabled(boolean bettingEnabled) {
        this.bettingEnabled = bettingEnabled;
    }
    
    public Arena getArena() {
        return arena;
    }
    
    public void setArena(Arena arena) {
        this.arena = arena;
    }
    
    public DuelState getState() {
        return state;
    }
    
    public void setState(DuelState state) {
        this.state = state;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    /**
     * Check if the given player is involved in this duel
     */
    public boolean isInvolved(Player player) {
        return challenger.equals(player) || target.equals(player);
    }
    
    /**
     * Get the opponent of the given player
     */
    public Player getOpponent(Player player) {
        if (challenger.equals(player)) {
            return target;
        } else if (target.equals(player)) {
            return challenger;
        }
        return null;
    }
    
    /**
     * Duel state enumeration
     */
    public enum DuelState {
        CREATING,       // Choosing Duel Settings
        PENDING,        // Waiting for acceptance
        SETTING_UP,     // Setting up bets/arena
        COUNTDOWN,      // Pre-fight countdown
        FIGHTING,       // Active fight
        FINISHED,       // Duel completed
        CANCELLED       // Duel cancelled
    }
}