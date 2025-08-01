package com.ezduels.model;

import java.util.UUID;

/**
 * Represents player statistics
 */
public class PlayerStats {
    
    private final UUID playerId;
    private int wins;
    private int losses;
    private int totalDuels;
    private long lastDuelTime;
    
    public PlayerStats(UUID playerId) {
        this.playerId = playerId;
        this.wins = 0;
        this.losses = 0;
        this.totalDuels = 0;
        this.lastDuelTime = 0;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getWins() {
        return wins;
    }
    
    public void setWins(int wins) {
        this.wins = wins;
    }
    
    public int getLosses() {
        return losses;
    }
    
    public void setLosses(int losses) {
        this.losses = losses;
    }
    
    public int getTotalDuels() {
        return totalDuels;
    }
    
    public void setTotalDuels(int totalDuels) {
        this.totalDuels = totalDuels;
    }
    
    public long getLastDuelTime() {
        return lastDuelTime;
    }
    
    public void setLastDuelTime(long lastDuelTime) {
        this.lastDuelTime = lastDuelTime;
    }
    
    /**
     * Add a win to the player's statistics
     */
    public void addWin() {
        wins++;
        totalDuels++;
        lastDuelTime = System.currentTimeMillis();
    }
    
    /**
     * Add a loss to the player's statistics
     */
    public void addLoss() {
        losses++;
        totalDuels++;
        lastDuelTime = System.currentTimeMillis();
    }
    
    /**
     * Get the win rate as a percentage
     */
    public double getWinRate() {
        if (totalDuels == 0) {
            return 0.0;
        }
        return (double) wins / totalDuels * 100.0;
    }
}