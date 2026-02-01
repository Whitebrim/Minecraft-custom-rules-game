package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.AbstractRule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 19: Can't attack a different hostile mob while the previous one you hit is still alive
 * If a player attacks hostile mob A, they can't attack hostile mob B until mob A is dead.
 */
public class Rule19NoSwitchHostileMob extends AbstractRule {
    
    // Track the last hostile mob each player attacked (by entity ID)
    private final Map<UUID, Integer> lastAttackedMob = new HashMap<>();
    
    public Rule19NoSwitchHostileMob() {
        super(19, "Нельзя бить другого враждебного моба пока предыдущий жив");
    }
    
    /**
     * Called when a player attacks an entity (from AttackMixin)
     */
    public void onPlayerAttack(ServerPlayerEntity attacker, Entity target) {
        if (!isValidTarget(attacker)) return;
        
        // Only care about hostile mobs
        if (!(target instanceof HostileEntity hostileMob)) {
            return;
        }
        
        UUID uuid = attacker.getUuid();
        Integer lastMobId = lastAttackedMob.get(uuid);
        
        if (lastMobId == null) {
            // First hostile mob attack - just record it
            lastAttackedMob.put(uuid, target.getId());
            DeathGameMod.LOGGER.info("[Rule19] Player {} started attacking hostile mob (id={})", 
                attacker.getName().getString(), target.getId());
            return;
        }
        
        // Check if attacking a different mob
        if (lastMobId != target.getId()) {
            // Check if the previous mob is still alive
            Entity lastMob = attacker.getServerWorld().getEntityById(lastMobId);
            
            if (lastMob != null && lastMob.isAlive() && !lastMob.isRemoved()) {
                // Check if it's a LivingEntity that might be dead
                if (lastMob instanceof LivingEntity livingMob && !livingMob.isDead()) {
                    // Previous mob still alive - violation!
                    DeathGameMod.LOGGER.info("[Rule19] Player {} attacked different hostile mob while previous (id={}) is still alive", 
                        attacker.getName().getString(), lastMobId);
                    killPlayer(attacker);
                    lastAttackedMob.remove(uuid);
                    return;
                }
            }
            
            // Previous mob is dead - update tracking to new mob
            lastAttackedMob.put(uuid, target.getId());
            DeathGameMod.LOGGER.info("[Rule19] Player {} switched to new hostile mob (id={}), previous mob dead", 
                attacker.getName().getString(), target.getId());
        }
        // If attacking the same mob, just continue tracking
    }
    
    /**
     * Called when a hostile mob dies - clear tracking for players who were fighting it
     */
    public void onHostileMobDeath(Entity entity) {
        if (!(entity instanceof HostileEntity)) return;
        
        int deadMobId = entity.getId();
        
        // Clear tracking for all players who were fighting this mob
        lastAttackedMob.entrySet().removeIf(entry -> entry.getValue() == deadMobId);
        
        DeathGameMod.LOGGER.info("[Rule19] Hostile mob (id={}) died, clearing tracking", deadMobId);
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity player) {
        lastAttackedMob.remove(player.getUuid());
    }
    
    @Override
    public void reset() {
        lastAttackedMob.clear();
    }
}
