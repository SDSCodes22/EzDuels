package com.ezduels.model;

import com.ezduels.EzDuelsPlugin;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.weather.WeatherType;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a duel arena
 */
public class Arena {
    
    private final String name;
    private final String basename;
    private final World world;
    private final BlockArrayClipboard clipboard; // Saves the current state of the arena for regeneration.
    private final CuboidRegion arenaRegion;
    private final Location minPoint;
    private final Location maxPoint;
    private final Location spawnPoint1;
    private final Location spawnPoint2;
    private boolean inUse;
    
    public Arena(String name, String basename, World world, Location minPoint, Location maxPoint, 
                 Location spawnPoint1, Location spawnPoint2) {
        this.name = name;
        this.basename = basename;
        this.world = world;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.spawnPoint1 = spawnPoint1;
        this.spawnPoint2 = spawnPoint2;
        this.inUse = false;

        // WorldEdit config for regeneration
        com.sk89q.worldedit.world.World WeWorld = BukkitAdapter.adapt(world);
        BlockVector3 pos1 = BlockVector3.at(minPoint.getBlockX(), minPoint.getBlockY(), minPoint.getBlockZ());
        BlockVector3 pos2 = BlockVector3.at(maxPoint.getBlockX(), maxPoint.getBlockY(), maxPoint.getBlockZ());
        this.arenaRegion = new CuboidRegion(WeWorld, pos1, pos2);
        this.clipboard = new BlockArrayClipboard(arenaRegion);
    }
    
    public String getName() {
        return name;
    }
    
    public String getBasename() {
        return basename;
    }
    
    public World getWorld() {
        return world;
    }
    
    public Location getMinPoint() {
        return minPoint;
    }
    
    public Location getMaxPoint() {
        return maxPoint;
    }
    
    public Location getSpawnPoint1() {
        return spawnPoint1;
    }
    
    public Location getSpawnPoint2() {
        return spawnPoint2;
    }
    
    public boolean isInUse() {
        return inUse;
    }
    
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
    
    /**
     * Check if a location is within the arena bounds
     */
    public boolean contains(Location location) {
        if (!location.getWorld().equals(world)) {
            return false;
        }
        
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        
        return x >= Math.min(minPoint.getX(), maxPoint.getX()) &&
               x <= Math.max(minPoint.getX(), maxPoint.getX()) &&
               y >= Math.min(minPoint.getY(), maxPoint.getY()) &&
               y <= Math.max(minPoint.getY(), maxPoint.getY()) &&
               z >= Math.min(minPoint.getZ(), maxPoint.getZ()) &&
               z <= Math.max(minPoint.getZ(), maxPoint.getZ());
    }

    /**
     * Saves the current arena state to a world edit clipboard.
     */
    public void saveState() {
        com.sk89q.worldedit.world.World WeWorld = BukkitAdapter.adapt(world);
        // Taken from WorldEdit API Docs
        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(WeWorld, -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    editSession, arenaRegion, clipboard, arenaRegion.getMinimumPoint()
            );
            // configure here
            Operations.complete(forwardExtentCopy);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
        EzDuelsPlugin.getInstance().getLogger().info("Saved Current Arena state!");
    }
    /**
     * Attempts to regenerate the arena to its original state.
     * CALL THIS WHEN DUEL IS COMPLETE!
     */
    public void regenerate() {
        com.sk89q.worldedit.world.World WeWorld = BukkitAdapter.adapt(world);
        // Paste the WorldGuard clipboard here
        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(WeWorld, -1)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(arenaRegion.getMinimumPoint())
                    // configure here
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
        EzDuelsPlugin.getInstance().getLogger().info("Pasted new Arena!");
    }
}