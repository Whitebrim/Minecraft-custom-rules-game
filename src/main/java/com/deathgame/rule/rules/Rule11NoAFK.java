package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Rule 11: Can't be AFK for 30 seconds
 * AFK is defined as not moving position AND not changing view direction.
 */
public class Rule11NoAFK extends AbstractRule {
    
    private static final int AFK_THRESHOLD_TICKS = 30 * 20; // 30 seconds
    private static final double POSITION_THRESHOLD = 0.1; // Movement threshold
    private static final float ROTATION_THRESHOLD = 1.0f; // Rotation threshold in degrees
    
    private final Map<UUID, Vec3d> lastPosition = new HashMap<>();
    private final Map<UUID, Float> lastYaw = new HashMap<>();
    private final Map<UUID, Float> lastPitch = new HashMap<>();
    private final Map<UUID, Integer> afkTicks = new HashMap<>();
    
    public Rule11NoAFK() {
        super(11, "Нельзя стоять АФК 30 секунд");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        for (ServerPlayerEntity player : gameManager.getOnlineParticipants()) {
            if (!isValidTarget(player)) continue;
            
            UUID uuid = player.getUuid();
            Vec3d currentPos = player.getPos();
            float currentYaw = player.getYaw();
            float currentPitch = player.getPitch();
            
            Vec3d prevPos = lastPosition.get(uuid);
            Float prevYaw = lastYaw.get(uuid);
            Float prevPitch = lastPitch.get(uuid);
            
            boolean hasMoved = false;
            
            if (prevPos != null && prevYaw != null && prevPitch != null) {
                // Check position change
                double posDelta = currentPos.distanceTo(prevPos);
                if (posDelta > POSITION_THRESHOLD) {
                    hasMoved = true;
                }
                
                // Check rotation change
                float yawDelta = Math.abs(currentYaw - prevYaw);
                float pitchDelta = Math.abs(currentPitch - prevPitch);
                if (yawDelta > ROTATION_THRESHOLD || pitchDelta > ROTATION_THRESHOLD) {
                    hasMoved = true;
                }
            }
            
            // Update tracking
            lastPosition.put(uuid, currentPos);
            lastYaw.put(uuid, currentYaw);
            lastPitch.put(uuid, currentPitch);
            
            if (hasMoved) {
                afkTicks.put(uuid, 0);
            } else {
                int ticks = afkTicks.getOrDefault(uuid, 0) + 1;
                afkTicks.put(uuid, ticks);
                
                if (ticks >= AFK_THRESHOLD_TICKS) {
                    DeathGameMod.LOGGER.info("[Rule11] Player {} was AFK for 30 seconds", player.getName().getString());
                    killPlayer(player);
                    afkTicks.put(uuid, 0);
                }
            }
        }
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        lastPosition.remove(uuid);
        lastYaw.remove(uuid);
        lastPitch.remove(uuid);
        afkTicks.remove(uuid);
    }
    
    @Override
    public void reset() {
        lastPosition.clear();
        lastYaw.clear();
        lastPitch.clear();
        afkTicks.clear();
    }
}
