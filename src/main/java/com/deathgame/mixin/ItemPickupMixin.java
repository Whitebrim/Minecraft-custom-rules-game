package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule6BerlordItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemPickupMixin {
    
    /**
     * Check at TAIL - after pickup has completed.
     * Scan player's inventory for infected items.
     * If found and player is not berlord - kill them.
     */
    @Inject(method = "onPlayerCollision", at = @At("TAIL"))
    private void onPlayerCollisionTail(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        try {
            Rule6BerlordItems rule = (Rule6BerlordItems) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(6);
            
            if (rule == null) {
                return;
            }
            
            // Check if rule is enabled and game is running
            if (!DeathGameMod.getInstance().getRuleManager().isRuleEnabled(6)) {
                return;
            }
            
            if (!DeathGameMod.getInstance().getGameManager().isGameRunning()) {
                return;
            }
            
            // Check if player is a participant
            if (!DeathGameMod.getInstance().getGameManager().isParticipant(serverPlayer)) {
                return;
            }
            
            // Skip dead players
            if (serverPlayer.isDead()) {
                return;
            }
            
            // Berlord cleanses items on pickup
            if (Rule6BerlordItems.isBerlord(serverPlayer)) {
                cleanseAllInfectedItems(serverPlayer.getInventory());
                return;
            }
            
            // For other players - check if they have any infected items
            if (hasInfectedItem(serverPlayer.getInventory())) {
                // Cleanse all infected items first (so they're safe when dropped on death)
                cleanseAllInfectedItems(serverPlayer.getInventory());
                // Then kill the player
                rule.killPlayerForViolation(serverPlayer);
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
    
    /**
     * Check if inventory contains any infected items
     */
    private boolean hasInfectedItem(PlayerInventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (Rule6BerlordItems.isItemInfected(stack)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Cleanse all infected items in inventory
     */
    private void cleanseAllInfectedItems(PlayerInventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (Rule6BerlordItems.isItemInfected(stack)) {
                Rule6BerlordItems.cleanseItem(stack);
            }
        }
    }
}
