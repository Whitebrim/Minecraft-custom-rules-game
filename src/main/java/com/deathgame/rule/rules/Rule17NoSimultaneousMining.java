package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 17: Two players can't mine blocks at the same time
 * If two or more players are actively breaking blocks simultaneously, they both die.
 * Tracks mining START events, not just block break completions.
 */
public class Rule17NoSimultaneousMining extends AbstractRule {
    
    // Track players who are currently mining (actively holding down mine button)
    private final Map<UUID, Long> activeMiningPlayers = new HashMap<>();
    
    // Time window - how long after starting to mine player is considered "actively mining"
    private static final long MINING_ACTIVE_WINDOW_TICKS = 10;
    
    // Cooldown to prevent multiple deaths from same mining session
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    public Rule17NoSimultaneousMining() {
        super(17, "Нельзя двум игрокам одновременно ломать блоки");
    }
    
    /**
     * Called when a player STARTS breaking a block (from MiningMixin)
     */
    public void onMiningStart(ServerPlayerEntity player, long gameTime) {
        if (!isValidTarget(player)) return;
        
        UUID uuid = player.getUuid();
        activeMiningPlayers.put(uuid, gameTime);
        
        DeathGameMod.LOGGER.debug("[Rule17] Player {} started mining at tick {}", 
            player.getName().getString(), gameTime);
    }
    
    /**
     * Called when a player STOPS breaking a block (releases button or breaks block)
     */
    public void onMiningStop(ServerPlayerEntity player) {
        if (!isValidTarget(player)) return;
        
        // Keep them in the map briefly to catch rapid re-mining
        // They'll be cleaned up by tick() after MINING_ACTIVE_WINDOW_TICKS
    }
    
    /**
     * Called when a player completes breaking a block (from BlockBreakMixin)
     */
    public void onBlockBreak(ServerPlayerEntity player, long gameTime) {
        if (!isValidTarget(player)) return;
        
        // Update their mining timestamp - they just completed a mine action
        activeMiningPlayers.put(player.getUuid(), gameTime);
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        long currentTime = server.getOverworld().getTime();
        
        // Skip if in cooldown
        if (currentTime - lastTriggerTime < COOLDOWN_TICKS) {
            return;
        }
        
        // Clean up old mining records (players who stopped mining)
        activeMiningPlayers.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > MINING_ACTIVE_WINDOW_TICKS);
        
        // Check if multiple players are mining simultaneously
        List<ServerPlayerEntity> activeMiners = new ArrayList<>();
        
        for (Map.Entry<UUID, Long> entry : activeMiningPlayers.entrySet()) {
            ServerPlayerEntity player = findPlayerByUuid(server, entry.getKey());
            if (player != null && isValidTarget(player)) {
                activeMiners.add(player);
            }
        }
        
        // If 2 or more players are mining simultaneously
        if (activeMiners.size() >= 2) {
            lastTriggerTime = currentTime;
            
            DeathGameMod.LOGGER.info("[Rule17] {} players mining simultaneously: {}", 
                activeMiners.size(),
                activeMiners.stream().map(p -> p.getName().getString()).toList());
            
            for (ServerPlayerEntity miner : activeMiners) {
                killPlayer(miner);
            }
            
            // Clear mining tracking after triggering
            activeMiningPlayers.clear();
        }
    }
    
    private ServerPlayerEntity findPlayerByUuid(MinecraftServer server, UUID uuid) {
        return server.getPlayerManager().getPlayer(uuid);
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity player) {
        activeMiningPlayers.remove(player.getUuid());
    }
    
    @Override
    public void reset() {
        activeMiningPlayers.clear();
        lastTriggerTime = 0;
    }
}
