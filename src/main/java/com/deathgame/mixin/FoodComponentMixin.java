package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule7HungerOverflow;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodComponent.class)
public abstract class FoodComponentMixin {

    @Inject(method = "onConsume", at = @At("HEAD"))
    private void onConsumeHead(World world, LivingEntity user, ItemStack stack, ConsumableComponent consumable, CallbackInfo ci) {
        if (world.isClient()) return;
        if (!(user instanceof ServerPlayerEntity player)) return;

        FoodComponent self = (FoodComponent) (Object) this;
        int foodToAdd = self.nutrition();
        int currentFood = player.getHungerManager().getFoodLevel();
        int wouldBe = currentFood + foodToAdd;

        DeathGameMod.LOGGER.info("[Rule7 DEBUG] FoodComponent.onConsume - currentFood: {}, foodToAdd: {}, wouldBe: {}, overflow: {}",
                currentFood, foodToAdd, wouldBe, wouldBe > 20);

        // Only trigger if it would EXCEED 20
        if (wouldBe > 20) {
            Rule7HungerOverflow.setPendingKill(true);
        }
    }

    @Inject(method = "onConsume", at = @At("TAIL"))
    private void onConsumeTail(World world, LivingEntity user, ItemStack stack, ConsumableComponent consumable, CallbackInfo ci) {
        if (world.isClient()) return;
        if (!(user instanceof ServerPlayerEntity player)) return;

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
                DeathGameMod.LOGGER.error("[Rule7] Error killing player", e);
            }
        }
    }
}