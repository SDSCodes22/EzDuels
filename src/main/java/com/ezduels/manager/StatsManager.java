package com.ezduels.manager;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.PlayerStats;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player statistics
 */
public class StatsManager {
    
    private final EzDuelsPlugin plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private final File statsFile;
    private final Yaml yaml;
    
    public StatsManager(EzDuelsPlugin plugin) {
        this.plugin = plugin;
        this.playerStats = new ConcurrentHashMap<>();
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
        
        loadStats();
    }
    
    /**
     * Load statistics from file
     */
    private void loadStats() {
        if (!statsFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(statsFile)) {
            Map<String, Object> data = yaml.load(reader);
            if (data == null) {
                return;
            }
            
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                UUID playerId = UUID.fromString(entry.getKey());
                Map<String, Object> statsData = (Map<String, Object>) entry.getValue();
                
                PlayerStats stats = new PlayerStats(playerId);
                stats.setWins((Integer) statsData.getOrDefault("wins", 0));
                stats.setLosses((Integer) statsData.getOrDefault("losses", 0));
                stats.setTotalDuels((Integer) statsData.getOrDefault("totalDuels", 0));
                stats.setLastDuelTime((Long) statsData.getOrDefault("lastDuelTime", 0L));
                
                playerStats.put(playerId, stats);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load stats: " + e.getMessage());
        }
    }
    
    /**
     * Save statistics to file
     */
    public void saveStats() {
        try {
            if (!statsFile.exists()) {
                statsFile.getParentFile().mkdirs();
                statsFile.createNewFile();
            }
            
            Map<String, Object> data = new HashMap<>();
            for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
                PlayerStats stats = entry.getValue();
                Map<String, Object> statsData = new HashMap<>();
                
                statsData.put("wins", stats.getWins());
                statsData.put("losses", stats.getLosses());
                statsData.put("totalDuels", stats.getTotalDuels());
                statsData.put("lastDuelTime", stats.getLastDuelTime());
                
                data.put(entry.getKey().toString(), statsData);
            }
            
            try (FileWriter writer = new FileWriter(statsFile)) {
                yaml.dump(data, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save stats: " + e.getMessage());
        }
    }
    
    /**
     * Get player statistics
     */
    public PlayerStats getStats(Player player) {
        return playerStats.computeIfAbsent(player.getUniqueId(), k -> new PlayerStats(player.getUniqueId()));
    }
    
    /**
     * Add a win to a player's statistics
     */
    public void addWin(Player player) {
        PlayerStats stats = getStats(player);
        stats.addWin();
        saveStats();
    }
    
    /**
     * Add a loss to a player's statistics
     */
    public void addLoss(Player player) {
        PlayerStats stats = getStats(player);
        stats.addLoss();
        saveStats();
    }
    
    /**
     * Save all statistics
     */
    public void saveAll() {
        saveStats();
    }
    
    /**
     * Get top players by wins
     */
    public Map<UUID, PlayerStats> getTopPlayersByWins(int limit) {
        return playerStats.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().getWins(), a.getValue().getWins()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }
}