package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule05Halal;
import com.deathgame.rule.rules.Rule14NoAttackPlayers;
import com.deathgame.rule.rules.Rule19NoSwitchHostileMob;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class AttackMixin {
    
    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        
        if (!(self instanceof ServerPlayerEntity attacker)) {
            return;
        }
        
        try {
            // Rule 14: Can't attack other participants
            if (target instanceof ServerPlayerEntity victim) {
                Rule14NoAttackPlayers rule14 = (Rule14NoAttackPlayers) DeathGameMod.getInstance()
                    .getRuleManager().getRuleById(14);
                
                if (rule14 != null) {
                    rule14.onPlayerAttack(attacker, victim);
                }
            }
            
            // Rule 5: Halal - track attacks on peaceful animals and hostile mobs
            Rule05Halal rule5 = (Rule05Halal) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(5);
            
            if (rule5 != null) {
                // We don't know the damage yet, so we pass 0 and let the rule track
                rule5.onPlayerAttack(attacker, target, 0);
            }
            
            // Rule 19: Can't switch hostile mobs while previous is alive
            Rule19NoSwitchHostileMob rule19 = (Rule19NoSwitchHostileMob) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(19);
            
            if (rule19 != null) {
                rule19.onPlayerAttack(attacker, target);
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
}
