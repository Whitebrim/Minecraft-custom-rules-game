package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule06NoBreakBelowIfOthersHave;
import com.deathgame.rule.rules.Rule17NoSimultaneousMining;
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
            Rule06NoBreakBelowIfOthersHave rule6 = (Rule06NoBreakBelowIfOthersHave) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(6);
            
            if (rule6 != null) {
                rule6.onBlockBroken(serverPlayer, pos, state);
            }
            
            // Rule 17: Can't mine simultaneously with other players
            Rule17NoSimultaneousMining rule17 = (Rule17NoSimultaneousMining) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(17);
            
            if (rule17 != null) {
                long gameTime = serverPlayer.getServerWorld().getTime();
                rule17.onBlockBreak(serverPlayer, gameTime);
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
}
