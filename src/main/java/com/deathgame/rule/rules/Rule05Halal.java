package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 5: Halal rules
 * - Can't eat pork (porkchop, cooked porkchop)
 * - Can't kill peaceful animals without one-shotting them
 * - Can't have enchanted items in inventory
 * - Can't run from hostile mobs for more than 20 seconds after being hit (only if mob is still alive)
 */
public class Rule05Halal extends AbstractRule {
    
    private static final Set<Item> PORK_ITEMS = Set.of(
        Items.PORKCHOP,
        Items.COOKED_PORKCHOP
    );
    
    private static final int COMBAT_ESCAPE_TIMEOUT_TICKS = 20 * 20; // 20 seconds
    
    // Track combat state per player: the hostile mob that hit them and when
    private final Map<UUID, CombatData> combatTracking = new HashMap<>();
    
    // Track damaged peaceful mobs per player to check one-shot kills
    private final Map<UUID, Set<Integer>> damagedPeacefulMobs = new HashMap<>();
    
    private static class CombatData {
        final int mobEntityId;
        final long hitTime;
        boolean hasRetaliated;
        
        CombatData(int mobEntityId, long hitTime) {
            this.mobEntityId = mobEntityId;
            this.hitTime = hitTime;
            this.hasRetaliated = false;
        }
    }
    
    public Rule05Halal() {
        super(5, "Нельзя нарушать Халяль/Ислам (есть свинину, убивать животное негуманно не с 1 точного удара, пользоваться магией, убегать от сражения) Ы");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        long currentTime = server.getOverworld().getTime();
        
        for (ServerPlayerEntity player : gameManager.getOnlineParticipants()) {
            if (!isValidTarget(player)) continue;
            
            // Check for enchanted items
            if (hasEnchantedItems(player)) {
                DeathGameMod.LOGGER.info("[Rule5] Player {} has enchanted items", player.getName().getString());
                killPlayer(player);
                continue;
            }
            
            // Check combat escape timer
            UUID uuid = player.getUuid();
            CombatData combat = combatTracking.get(uuid);
            if (combat != null && !combat.hasRetaliated) {
                // Check if timeout exceeded
                if ((currentTime - combat.hitTime) > COMBAT_ESCAPE_TIMEOUT_TICKS) {
                    // Check if the mob is still alive
                    Entity mob = player.getServerWorld().getEntityById(combat.mobEntityId);
                    if (mob != null && mob.isAlive() && mob instanceof LivingEntity livingMob && !livingMob.isDead()) {
                        // Mob is still alive and player didn't retaliate - kill player
                        DeathGameMod.LOGGER.info("[Rule5] Player {} fled from combat for too long (mob still alive)", player.getName().getString());
                        killPlayer(player);
                    } else {
                        // Mob is dead - clear combat tracking, player is safe
                        DeathGameMod.LOGGER.info("[Rule5] Combat mob died, clearing tracking for {}", player.getName().getString());
                    }
                    combatTracking.remove(uuid);
                }
            }
        }
    }
    
    private boolean hasEnchantedItems(ServerPlayerEntity player) {
        // Check main inventory
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && isEnchanted(stack)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isEnchanted(ItemStack stack) {
        // Check for enchantments component
        var enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            return true;
        }
        // Also check stored enchantments (for enchanted books)
        var storedEnchantments = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
        return storedEnchantments != null && !storedEnchantments.isEmpty();
    }
    
    /**
     * Called when player eats food (from mixin)
     */
    public void onPlayerEat(ServerPlayerEntity player, Item foodItem) {
        if (!isValidTarget(player)) return;
        
        if (PORK_ITEMS.contains(foodItem)) {
            DeathGameMod.LOGGER.info("[Rule5] Player {} ate pork", player.getName().getString());
            killPlayer(player);
        }
    }
    
    /**
     * Called when player damages an entity
     */
    public void onPlayerAttack(ServerPlayerEntity player, Entity target, float damage) {
        if (!isValidTarget(player)) return;
        
        // Check if attacking hostile mob (counts as retaliation)
        if (target instanceof HostileEntity) {
            UUID uuid = player.getUuid();
            CombatData combat = combatTracking.get(uuid);
            if (combat != null && target.getId() == combat.mobEntityId) {
                // Player retaliated against the same mob that hit them
                combat.hasRetaliated = true;
                combatTracking.remove(uuid);
                DeathGameMod.LOGGER.info("[Rule5] Player {} retaliated against hostile mob", player.getName().getString());
            }
        }
        
        // Check if attacking peaceful animal
        if (isPeacefulAnimal(target)) {
            UUID uuid = player.getUuid();
            Set<Integer> damaged = damagedPeacefulMobs.computeIfAbsent(uuid, k -> new HashSet<>());
            
            // If this animal was already damaged by this player, it's not a one-shot
            if (damaged.contains(target.getId())) {
                // Animal survived first hit, this is a violation
                DeathGameMod.LOGGER.info("[Rule5] Player {} didn't one-shot peaceful animal", player.getName().getString());
                killPlayer(player);
            } else {
                // First hit - track it
                damaged.add(target.getId());
            }
        }
    }
    
    /**
     * Called when a peaceful mob dies - remove from tracking
     */
    public void onEntityDeath(Entity entity) {
        if (isPeacefulAnimal(entity)) {
            for (Set<Integer> damaged : damagedPeacefulMobs.values()) {
                damaged.remove(entity.getId());
            }
        }
    }
    
    /**
     * Called when player is hit by hostile mob
     */
    public void onPlayerHitByHostile(ServerPlayerEntity player, Entity attacker, long gameTime) {
        if (!isValidTarget(player)) return;
        
        if (attacker instanceof HostileEntity) {
            UUID uuid = player.getUuid();
            // Start new combat tracking (or reset if hit by different/same mob)
            combatTracking.put(uuid, new CombatData(attacker.getId(), gameTime));
            DeathGameMod.LOGGER.info("[Rule5] Player {} hit by hostile mob (id={}), must retaliate within 20s", 
                player.getName().getString(), attacker.getId());
        }
    }
    
    private boolean isPeacefulAnimal(Entity entity) {
        return entity instanceof AnimalEntity && !(entity instanceof HostileEntity);
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        combatTracking.remove(uuid);
        damagedPeacefulMobs.remove(uuid);
    }
    
    @Override
    public void reset() {
        combatTracking.clear();
        damagedPeacefulMobs.clear();
    }
}
