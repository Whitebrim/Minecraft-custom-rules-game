package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule05Halal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EntityDeathMixin {
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onEntityDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        try {
            // Notify Rule 5 about entity death (for peaceful animal tracking)
            Rule05Halal rule = (Rule05Halal) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(5);
            
            if (rule != null) {
                rule.onEntityDeath(self);
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
}
