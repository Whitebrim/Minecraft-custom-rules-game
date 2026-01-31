package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule7HungerOverflow;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EatFoodMixin {
    
    @Inject(method = "eatFood", at = @At("TAIL"))
    private void onEatFoodTail(World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        
        // Check if HungerMixin flagged this as an overflow
        if (Rule7HungerOverflow.isPendingKill()) {
            Rule7HungerOverflow.clearPendingKill();
            
            try {
                Rule7HungerOverflow rule = (Rule7HungerOverflow) DeathGameMod.getInstance()
                    .getRuleManager().getRuleById(7);
                
                if (rule != null && rule.shouldKillPlayer(player)) {
                    DeathGameMod.LOGGER.info("[Rule7 DEBUG] Killing player for hunger overflow");
                    rule.killPlayerForViolation(player);
                }
            } catch (Exception e) {
                // Ignore if game not initialized
            }
        }
    }
}
