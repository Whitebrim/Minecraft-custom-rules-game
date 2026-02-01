package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule17NoSimultaneousMining;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class MiningMixin {
    
    @Shadow
    @Final
    protected ServerPlayerEntity player;
    
    @Inject(method = "processBlockBreakingAction", at = @At("HEAD"))
    private void onBlockBreakingAction(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
        // Track when player starts or continues mining
        if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK || 
            action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
            
            try {
                Rule17NoSimultaneousMining rule17 = (Rule17NoSimultaneousMining) DeathGameMod.getInstance()
                    .getRuleManager().getRuleById(17);
                
                if (rule17 != null) {
                    long gameTime = player.getServerWorld().getTime();
                    
                    if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                        rule17.onMiningStart(player, gameTime);
                    } else {
                        rule17.onMiningStop(player);
                    }
                }
            } catch (Exception e) {
                // Ignore if game not initialized
            }
        }
    }
}
