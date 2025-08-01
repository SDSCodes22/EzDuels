package com.ezduels.arena;

import com.ezduels.EzDuelsPlugin;
import com.ezduels.model.Arena;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages duel arenas
 */
public class ArenaManager {
    
    private final EzDuelsPlugin plugin;
    private final Map<String, List<Arena>> arenaGroups;
    private final File arenasFile;
    private final Yaml yaml;
    
    public ArenaManager(EzDuelsPlugin plugin) {
        this.plugin = plugin;
        this.arenaGroups = new ConcurrentHashMap<>();
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
        
        loadArenas();
    }
    
    /**
     * Load arenas from file
     */
    private void loadArenas() {
        if (!arenasFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(arenasFile)) {
            Map<String, Object> data = yaml.load(reader);
            if (data == null) {
                return;
            }
            
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String basename = entry.getKey();
                List<Map<String, Object>> arenasList = (List<Map<String, Object>>) entry.getValue();
                
                List<Arena> arenas = new ArrayList<>();
                for (Map<String, Object> arenaData : arenasList) {
                    Arena arena = deserializeArena(arenaData);
                    if (arena != null) {
                        arenas.add(arena);
                    }
                }
                
                arenaGroups.put(basename, arenas);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load arenas: " + e.getMessage());
        }
    }
    
    /**
     * Save arenas to file
     */
    public void saveArenas() {
        try {
            if (!arenasFile.exists()) {
                arenasFile.getParentFile().mkdirs();
                arenasFile.createNewFile();
            }
            
            Map<String, Object> data = new HashMap<>();
            for (Map.Entry<String, List<Arena>> entry : arenaGroups.entrySet()) {
                List<Map<String, Object>> arenasList = new ArrayList<>();
                for (Arena arena : entry.getValue()) {
                    arenasList.add(serializeArena(arena));
                }
                data.put(entry.getKey(), arenasList);
            }
            
            try (FileWriter writer = new FileWriter(arenasFile)) {
                yaml.dump(data, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas: " + e.getMessage());
        }
    }
    
    /**
     * Create a new arena from WorldEdit selection
     */
    public Arena createArena(String basename, Player player) {
        try {
            WorldEditPlugin worldEdit = (WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
            if (worldEdit == null) {
                return null;
            }
            
            // com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
            World weWorld = BukkitAdapter.adapt(player.getWorld());
            Region region = worldEdit.getSession(player).getSelection(weWorld);
            
            if (region == null) {
                return null;
            }
            
            // Generate arena name
            List<Arena> existingArenas = arenaGroups.getOrDefault(basename, new ArrayList<>());
            String arenaName = basename + (existingArenas.size() + 1);
            
            // Get world
            org.bukkit.World world = player.getWorld();
            
            // Get region bounds
            com.sk89q.worldedit.math.BlockVector3 minVector = region.getMinimumPoint();
            com.sk89q.worldedit.math.BlockVector3 maxVector = region.getMaximumPoint();
            Location minPoint = new Location(world, minVector.x(), minVector.y(), minVector.z());
            Location maxPoint = new Location(world, maxVector.x(), maxVector.y(), maxVector.z());
            
            // Create arena (spawn points will be set later)
            Arena arena = new Arena(arenaName, basename, world, minPoint, maxPoint, null, null);
            
            // Add to group
            existingArenas.add(arena);
            arenaGroups.put(basename, existingArenas);
            
            return arena;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create arena: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Set spawn location for an arena
     */
    public boolean setSpawnLocation(Arena arena, int spawnNumber, Location location) {
        // Create new arena with updated spawn location
        Location spawn1 = spawnNumber == 1 ? location : arena.getSpawnPoint1();
        Location spawn2 = spawnNumber == 2 ? location : arena.getSpawnPoint2();
        
        Arena updatedArena = new Arena(arena.getName(), arena.getBasename(), arena.getWorld(), 
            arena.getMinPoint(), arena.getMaxPoint(), spawn1, spawn2);
        
        // Replace in the group
        List<Arena> group = arenaGroups.get(arena.getBasename());
        if (group != null) {
            int index = group.indexOf(arena);
            if (index != -1) {
                group.set(index, updatedArena);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get an available arena from a group
     */
    public Arena getAvailableArena(String basename) {
        List<Arena> group = arenaGroups.get(basename);
        if (group == null || group.isEmpty()) {
            return null;
        }
        
        return group.stream()
            .filter(arena -> !arena.isInUse())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get all arena groups
     */
    public Set<String> getArenaGroups() {
        return arenaGroups.keySet();
    }
    
    /**
     * Get arenas in a group
     */
    public List<Arena> getArenas(String basename) {
        return arenaGroups.getOrDefault(basename, new ArrayList<>());
    }
    
    /**
     * Check if an arena group exists
     */
    public boolean hasArenaGroup(String basename) {
        return arenaGroups.containsKey(basename);
    }
    
    /**
     * Serialize arena to map
     */
    private Map<String, Object> serializeArena(Arena arena) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", arena.getName());
        data.put("basename", arena.getBasename());
        data.put("world", arena.getWorld().getName());
        data.put("minPoint", serializeLocation(arena.getMinPoint()));
        data.put("maxPoint", serializeLocation(arena.getMaxPoint()));
        
        if (arena.getSpawnPoint1() != null) {
            data.put("spawnPoint1", serializeLocation(arena.getSpawnPoint1()));
        }
        if (arena.getSpawnPoint2() != null) {
            data.put("spawnPoint2", serializeLocation(arena.getSpawnPoint2()));
        }
        
        return data;
    }
    
    /**
     * Deserialize arena from map
     */
    private Arena deserializeArena(Map<String, Object> data) {
        try {
            String name = (String) data.get("name");
            String basename = (String) data.get("basename");
            String worldName = (String) data.get("world");
            
            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                return null;
            }
            
            Location minPoint = deserializeLocation((Map<String, Object>) data.get("minPoint"), world);
            Location maxPoint = deserializeLocation((Map<String, Object>) data.get("maxPoint"), world);
            
            Location spawnPoint1 = null;
            Location spawnPoint2 = null;
            
            if (data.containsKey("spawnPoint1")) {
                spawnPoint1 = deserializeLocation((Map<String, Object>) data.get("spawnPoint1"), world);
            }
            if (data.containsKey("spawnPoint2")) {
                spawnPoint2 = deserializeLocation((Map<String, Object>) data.get("spawnPoint2"), world);
            }
            
            return new Arena(name, basename, world, minPoint, maxPoint, spawnPoint1, spawnPoint2);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize arena: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Serialize location to map
     */
    private Map<String, Object> serializeLocation(Location location) {
        Map<String, Object> data = new HashMap<>();
        data.put("x", location.getX());
        data.put("y", location.getY());
        data.put("z", location.getZ());
        data.put("yaw", location.getYaw());
        data.put("pitch", location.getPitch());
        return data;
    }
    
    /**
     * Deserialize location from map
     */
    private Location deserializeLocation(Map<String, Object> data, org.bukkit.World world) {
        double x = ((Number) data.get("x")).doubleValue();
        double y = ((Number) data.get("y")).doubleValue();
        double z = ((Number) data.get("z")).doubleValue();
        float yaw = ((Number) data.getOrDefault("yaw", 0.0f)).floatValue();
        float pitch = ((Number) data.getOrDefault("pitch", 0.0f)).floatValue();
        
        return new Location(world, x, y, z, yaw, pitch);
    }
}