package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule05Halal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PlayerDamageMixin {
    
    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        
        Entity attacker = source.getAttacker();
        if (attacker == null) {
            return;
        }
        
        try {
            // Rule 5: Track when player is hit by hostile mob
            if (attacker instanceof HostileEntity) {
                Rule05Halal rule = (Rule05Halal) DeathGameMod.getInstance()
                    .getRuleManager().getRuleById(5);
                
                if (rule != null) {
                    long gameTime = player.getServerWorld().getTime();
                    rule.onPlayerHitByHostile(player, attacker, gameTime);
                }
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
}
