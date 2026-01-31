package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

import java.util.*;

/**
 * Rule 12: Can't look at the sky
 * Conditions for violation (ALL must be true):
 * - Sky light level at player position = 15
 * - Player's pitch is between -90 and -45 (looking up)
 * - No targeted block (raycast doesn't hit a block within range)
 */
public class Rule12NoLookAtSky extends AbstractRule {
    
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    public Rule12NoLookAtSky() {
        super(12, "Нельзя смотреть на небо");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        long currentTime = server.getOverworld().getTime();
        if (currentTime - lastTriggerTime < COOLDOWN_TICKS) {
            return;
        }
        
        for (ServerPlayerEntity player : gameManager.getOnlineParticipants()) {
            if (!isValidTarget(player)) continue;
            
            // Check sky light level at player's eye position
            BlockPos eyePos = BlockPos.ofFloored(player.getEyePos());
            int skyLight = player.getWorld().getLightLevel(LightType.SKY, eyePos);
            
            if (skyLight != 15) continue;
            
            // Check pitch (negative pitch = looking up)
            // -90 = straight up, 0 = horizon, 90 = straight down
            float pitch = player.getPitch();
            if (pitch > -45 || pitch < -90) continue; // Not looking up enough
            
            // Check if there's a targeted block
            HitResult hitResult = player.raycast(256.0, 0, false);
            if (hitResult.getType() == HitResult.Type.BLOCK) continue; // Looking at a block
            
            // All conditions met - player is looking at sky
            DeathGameMod.LOGGER.info("[Rule12] Player {} is looking at the sky (skyLight={}, pitch={})", 
                player.getName().getString(), skyLight, pitch);
            lastTriggerTime = currentTime;
            killPlayer(player);
        }
    }
    
    @Override
    public void reset() {
        lastTriggerTime = 0;
    }
}
