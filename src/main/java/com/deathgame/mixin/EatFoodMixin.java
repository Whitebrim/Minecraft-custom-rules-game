package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule01EatLookingAtBlock;
import com.deathgame.rule.rules.Rule05Halal;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class EatFoodMixin {

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void onFinishUsing(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient()) return;
        if (!(user instanceof ServerPlayerEntity player)) return;

        // Check if this item is food
        if (stack.get(DataComponentTypes.FOOD) == null) return;

        try {
            // Rule 5: Halal - can't eat pork
            Rule05Halal rule5 = (Rule05Halal) DeathGameMod.getInstance()
                    .getRuleManager().getRuleById(5);
            if (rule5 != null) {
                rule5.onPlayerEat(player, stack.getItem());
            }

            // Rule 1: Can't eat while looking at blocks
            Rule01EatLookingAtBlock rule1 = (Rule01EatLookingAtBlock) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(1);
            if (rule1 != null) {
                rule1.onPlayerEat(player);
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
}
