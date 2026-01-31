package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule02NoPickupDuplicates;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemPickupMixin {
    
    @Shadow
    public abstract ItemStack getStack();
    
    @Unique
    private boolean deathgame_shouldKill = false;
    
    @Unique
    private ServerPlayerEntity deathgame_playerToKill = null;
    
    /**
     * Check BEFORE pickup - if player already has this item, mark for kill
     */
    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void onPlayerCollisionHead(PlayerEntity player, CallbackInfo ci) {
        deathgame_shouldKill = false;
        deathgame_playerToKill = null;
        
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        ItemStack stack = this.getStack();
        if (stack.isEmpty()) {
            return;
        }
        
        try {
            Rule02NoPickupDuplicates rule = (Rule02NoPickupDuplicates) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(2);
            
            if (rule != null && rule.shouldKillOnPickup(serverPlayer, stack)) {
                deathgame_shouldKill = true;
                deathgame_playerToKill = serverPlayer;
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
    
    /**
     * Kill player AFTER pickup completes
     */
    @Inject(method = "onPlayerCollision", at = @At("TAIL"))
    private void onPlayerCollisionTail(PlayerEntity player, CallbackInfo ci) {
        if (deathgame_shouldKill && deathgame_playerToKill != null) {
            try {
                Rule02NoPickupDuplicates rule = (Rule02NoPickupDuplicates) DeathGameMod.getInstance()
                    .getRuleManager().getRuleById(2);
                
                if (rule != null) {
                    rule.killPlayerForViolation(deathgame_playerToKill);
                }
            } catch (Exception e) {
                // Ignore
            }
            deathgame_shouldKill = false;
            deathgame_playerToKill = null;
        }
    }
}
