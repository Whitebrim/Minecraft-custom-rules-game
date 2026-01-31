package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

import java.util.*;

/**
 * Rule 13: Can't stand under tree shadow
 * Conditions for violation (ALL must be true):
 * - Sky light level at player position < 15
 * - There is a leaves block within 5 blocks directly above the player's head
 */
public class Rule13NoTreeShadow extends AbstractRule {
    
    private static final int MAX_CHECK_HEIGHT = 5;
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    public Rule13NoTreeShadow() {
        super(13, "Нельзя стоять под тенью деревьев");
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
            
            // Check sky light level at player's head position
            BlockPos headPos = BlockPos.ofFloored(player.getX(), player.getY() + player.getEyeHeight(player.getPose()), player.getZ());
            int skyLight = player.getWorld().getLightLevel(LightType.SKY, headPos);
            
            if (skyLight >= 15) continue; // Not in shadow
            
            // Check for leaves above
            if (hasLeavesAbove(player, headPos)) {
                DeathGameMod.LOGGER.info("[Rule13] Player {} is under tree shadow (skyLight={})", 
                    player.getName().getString(), skyLight);
                lastTriggerTime = currentTime;
                killPlayer(player);
            }
        }
    }
    
    private boolean hasLeavesAbove(ServerPlayerEntity player, BlockPos startPos) {
        for (int y = 1; y <= MAX_CHECK_HEIGHT; y++) {
            BlockPos checkPos = startPos.up(y);
            BlockState state = player.getWorld().getBlockState(checkPos);
            
            // Check if block is any type of leaves
            if (state.isIn(BlockTags.LEAVES)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void reset() {
        lastTriggerTime = 0;
    }
}
