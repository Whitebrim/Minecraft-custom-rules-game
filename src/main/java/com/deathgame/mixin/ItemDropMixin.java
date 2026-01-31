package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule6BerlordItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class ItemDropMixin {
    
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", 
            at = @At("RETURN"))
    private void onDropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, 
                           CallbackInfoReturnable<ItemEntity> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        
        ItemEntity itemEntity = cir.getReturnValue();
        if (itemEntity == null) {
            return;
        }
        
        try {
            Rule6BerlordItems rule = (Rule6BerlordItems) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(6);
            
            if (rule != null) {
                rule.onItemDropped(player, itemEntity);
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
}
