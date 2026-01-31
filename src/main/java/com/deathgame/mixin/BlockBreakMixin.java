package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule06NoBreakBelowIfOthersHave;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockBreakMixin {
    
    @Inject(method = "onBreak", at = @At("HEAD"))
    private void onBlockBreak(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<BlockState> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        try {
            // Rule 6: Can't break blocks below if others have them
            Rule06NoBreakBelowIfOthersHave rule = (Rule06NoBreakBelowIfOthersHave) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(6);
            
            if (rule != null) {
                rule.onBlockBroken(serverPlayer, pos, state);
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
}
