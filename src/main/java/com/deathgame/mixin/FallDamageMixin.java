package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule4FallDamageTransfer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class FallDamageMixin {
    
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        
        // Check if this is fall damage
        if (!source.isOf(DamageTypes.FALL)) {
            return;
        }
        
        try {
            Rule4FallDamageTransfer rule = (Rule4FallDamageTransfer) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(4);
            
            if (rule != null && rule.onLethalFallDamage(player, amount)) {
                cir.setReturnValue(false);
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
}
