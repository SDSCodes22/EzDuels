package com.ezduels;

import com.ezduels.arena.ArenaManager;
import com.ezduels.commands.*;
import com.ezduels.gui.GuiManager;
import com.ezduels.listeners.*;
import com.ezduels.manager.BettingManager;
import com.ezduels.manager.DuelManager;
import com.ezduels.manager.PrizeManager;
import com.ezduels.manager.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Main plugin class for EzDuels
 * Handles initialization and provides access to managers
 */
public class EzDuelsPlugin extends JavaPlugin {
    
    private static EzDuelsPlugin instance;
    private static Component pluginPrefix;
    private static Logger pluginLogger;
    
    // Managers
    private DuelManager duelManager;
    private ArenaManager arenaManager;
    private BettingManager bettingManager;
    private PrizeManager prizeManager;
    private StatsManager statsManager;
    private GuiManager guiManager;
    
    @Override
    public void onEnable() {
        instance = this;
        pluginLogger = getLogger();
        
        // Save default config
        saveDefaultConfig();
        
        // Load plugin prefix from config
        loadPluginPrefix();
        
        // Initialize managers
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        pluginLogger.info("EzDuels has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Clean up active duels
        if (duelManager != null) {
            duelManager.cleanup();
        }
        
        // Save data
        if (statsManager != null) {
            statsManager.saveAll();
        }
        
        pluginLogger.info("EzDuels has been disabled!");
    }
    
    /**
     * Load the plugin prefix from config using MiniMessage
     */
    private void loadPluginPrefix() {
        String prefixString = getConfig().getString("plugin.prefix", "<gray>[<color:#45bbff><b>DUELS</b></color>]</gray>");
        pluginPrefix = MiniMessage.miniMessage().deserialize(prefixString);
    }
    
    /**
     * Initialize all managers
     */
    private void initializeManagers() {
        arenaManager = new ArenaManager(this);
        duelManager = new DuelManager(this);
        bettingManager = new BettingManager(this);
        prizeManager = new PrizeManager(this);
        statsManager = new StatsManager(this);
        guiManager = new GuiManager(this);
    }
    
    /**
     * Register all commands
     */
    private void registerCommands() {
        getCommand("duel").setExecutor(new DuelCommand(this));
        getCommand("duel").setTabCompleter(new DuelCommand(this));
        getCommand("duelaccept").setExecutor(new DuelAcceptCommand(this));
        getCommand("dueldeny").setExecutor(new DuelDenyCommand(this));
        getCommand("betmenu").setExecutor(new BetMenuCommand(this));
        getCommand("skip").setExecutor(new SkipCommand(this));
        getCommand("leavefight").setExecutor(new LeaveFightCommand(this));
        getCommand("cancelfight").setExecutor(new CancelFightCommand(this));
        getCommand("prizes").setExecutor(new PrizesCommand(this));
        // TODO: Spectator system needs an overhaul
        // getCommand("spectatefight").setExecutor(new SpectateFightCommand(this));
        getCommand("duelsadmin").setExecutor(new DuelsAdminCommand(this));
    }
    
    /**
     * Register all event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new DuelListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaListener(this), this);
        getServer().getPluginManager().registerEvents(new BettingListener(this), this);
    }
    
    // Getters for dependency injection
    public static EzDuelsPlugin getInstance() {
        return instance;
    }
    
    public static Component getPluginPrefix() {
        return pluginPrefix;
    }
    
    public static Logger getPluginLogger() {
        return pluginLogger;
    }
    
    public DuelManager getDuelManager() {
        return duelManager;
    }
    
    public ArenaManager getArenaManager() {
        return arenaManager;
    }
    
    public BettingManager getBettingManager() {
        return bettingManager;
    }
    
    public PrizeManager getPrizeManager() {
        return prizeManager;
    }
    
    public StatsManager getStatsManager() {
        return statsManager;
    }
    
    public GuiManager getGuiManager() {
        return guiManager;
    }
}