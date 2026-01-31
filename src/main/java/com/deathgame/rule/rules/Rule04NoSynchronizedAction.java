package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 4: All players can't simultaneously do the same action
 * If ALL participants are sneaking, OR all are sprinting, OR all are jumping at the same time, everyone dies.
 */
public class Rule04NoSynchronizedAction extends AbstractRule {
    
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    // Track players who were on ground last tick (for jump detection)
    private final Set<UUID> wasOnGround = new HashSet<>();
    
    public Rule04NoSynchronizedAction() {
        super(4, "Нельзя чтобы все игроки одновременно шифтили, бежали или прыгали");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        long currentTime = server.getOverworld().getTime();
        if (currentTime - lastTriggerTime < COOLDOWN_TICKS) {
            return;
        }
        
        List<ServerPlayerEntity> players = gameManager.getOnlineParticipants()
            .stream()
            .filter(this::isValidTarget)
            .toList();
        
        // Need at least 2 players for this rule
        if (players.size() < 2) return;
        
        // Check sneaking
        boolean allSneaking = players.stream().allMatch(ServerPlayerEntity::isSneaking);
        
        // Check sprinting
        boolean allSprinting = players.stream().allMatch(ServerPlayerEntity::isSprinting);
        
        // Check jumping (transition from on ground to not on ground with upward velocity)
        List<ServerPlayerEntity> jumpingPlayers = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
            boolean wasGrounded = wasOnGround.contains(player.getUuid());
            boolean isGrounded = player.isOnGround();
            boolean hasUpwardVelocity = player.getVelocity().y > 0.1;
            
            if (wasGrounded && !isGrounded && hasUpwardVelocity) {
                jumpingPlayers.add(player);
            }
            
            // Update ground tracking
            if (isGrounded) {
                wasOnGround.add(player.getUuid());
            } else {
                wasOnGround.remove(player.getUuid());
            }
        }
        boolean allJumping = jumpingPlayers.size() == players.size() && players.size() >= 2;
        
        String reason = null;
        if (allSneaking) {
            reason = "sneaking";
        } else if (allSprinting) {
            reason = "sprinting";
        } else if (allJumping) {
            reason = "jumping";
        }
        
        if (reason != null) {
            lastTriggerTime = currentTime;
            DeathGameMod.LOGGER.info("[Rule4] All players are {} simultaneously", reason);
            for (ServerPlayerEntity player : players) {
                killPlayer(player);
            }
        }
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity player) {
        wasOnGround.remove(player.getUuid());
    }
    
    @Override
    public void reset() {
        lastTriggerTime = 0;
        wasOnGround.clear();
    }
}
