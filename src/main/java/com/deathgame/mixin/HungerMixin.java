package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule7HungerOverflow;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public abstract class HungerMixin {
    
    @Shadow
    private int foodLevel;
    
    @Inject(method = "eat", at = @At("HEAD"))
    private void onEat(FoodComponent foodComponent, CallbackInfo ci) {
        int foodToAdd = foodComponent.nutrition();
        int currentFood = this.foodLevel; // This is BEFORE the food is added
        int wouldBe = currentFood + foodToAdd;
        
        DeathGameMod.LOGGER.info("[Rule7 DEBUG] currentFood: {}, foodToAdd: {}, wouldBe: {}, overflow: {}", 
            currentFood, foodToAdd, wouldBe, wouldBe > 20);
        
        // Only trigger if it would EXCEED 20
        if (wouldBe > 20) {
            // Store the pending kill - will be processed by Rule7
            Rule7HungerOverflow.setPendingKill(true);
        }
    }
}
