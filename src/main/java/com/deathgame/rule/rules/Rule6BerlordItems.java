package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.AbstractRule;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Rule 6: Items dropped by berlord kill non-berlord players
 * When berlord drops items, they become infected.
 * If another participant picks up infected items, they die.
 * Items become safe when berlord picks them back up, or when passed through containers,
 * or when berlord dies.
 */
public class Rule6BerlordItems extends AbstractRule {

    public static final UUID BERLORD_UUID = UUID.fromString("55780bed-96ed-3d3f-b988-291d43312cf7");
    public static final String BERLORD_NAME = "BotKot1k_";
    public static final String INFECTED_TAG = "deathgame_item_infected";

    public Rule6BerlordItems() { super(6, "Предметы, выброшенные " + BERLORD_NAME + ", убивают других игроков"); }
    
    /**
     * Called when any player drops an item
     */
    public void onItemDropped(ServerPlayerEntity player, ItemEntity itemEntity) {
        if (!isValidTarget(player)) return;
        
        if (isBerlord(player)) {
            // Infect the item
            infectItem(itemEntity.getStack());
        }
    }
    
    /**
     * Check if picking up this item should kill the player
     */
    public boolean shouldKillOnPickup(ServerPlayerEntity player, ItemStack stack) {
        if (!isValidTarget(player)) return false;
        
        // Berlord is immune
        if (isBerlord(player)) {
            return false;
        }
        
        return isItemInfected(stack);
    }
    
    /**
     * Actually kill the player after pickup.
     * Note: isValidTarget check should be done by caller.
     */
    public void killPlayerForViolation(ServerPlayerEntity player) {
        killPlayer(player);
    }
    
    /**
     * Check if the player is berlord by UUID or by name (case-insensitive)
     */
    public static boolean isBerlord(ServerPlayerEntity player) {
        // Check UUID first (most reliable)
//        DeathGameMod.LOGGER.info(
//                "Player {} UUID = {}",
//                player.getName().getString(),
//                player.getUuid()
//        );
        if (player.getUuid().equals(BERLORD_UUID)) {
            return true;
        }
        // Fallback to name check (case-insensitive)
        return player.getGameProfile().getName().equalsIgnoreCase(BERLORD_NAME);
    }
    
    public static void infectItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        NbtCompound customData = new NbtCompound();
        
        // Get existing custom data if present
        NbtComponent existing = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (existing != null) {
            customData = existing.copyNbt();
        }
        
        customData.putBoolean(INFECTED_TAG, true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
    }
    
    public static void cleanseItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        NbtComponent existing = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (existing != null) {
            NbtCompound customData = existing.copyNbt();
            customData.remove(INFECTED_TAG);
            
            if (customData.isEmpty()) {
                stack.remove(DataComponentTypes.CUSTOM_DATA);
            } else {
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
            }
        }
    }
    
    public static boolean isItemInfected(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyNbt().getBoolean(INFECTED_TAG);
        }
        return false;
    }
}
