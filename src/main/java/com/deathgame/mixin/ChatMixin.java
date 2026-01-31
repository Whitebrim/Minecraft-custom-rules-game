package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule15NoChat;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ChatMixin {
    
    @Shadow
    public ServerPlayerEntity player;
    
    @Inject(method = "handleDecoratedMessage", at = @At("HEAD"))
    private void onChatMessage(SignedMessage message, CallbackInfo ci) {
        try {
            Rule15NoChat rule = (Rule15NoChat) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(15);
            
            if (rule != null) {
                String content = message.getContent().getString();
                rule.onChatMessage(player, content);
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
}
