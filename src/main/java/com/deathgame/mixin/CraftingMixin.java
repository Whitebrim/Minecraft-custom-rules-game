package com.deathgame.mixin;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.rules.Rule10NoCraftDuplicates;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class CraftingMixin {
    
    @Unique
    private boolean deathgame_shouldKill = false;
    
    @Unique
    private ServerPlayerEntity deathgame_playerToKill = null;
    
    @Inject(method = "internalOnSlotClick", at = @At("HEAD"))
    private void onSlotClickHead(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        deathgame_shouldKill = false;
        deathgame_playerToKill = null;
        
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        ScreenHandler self = (ScreenHandler) (Object) this;
        
        // Check if this is a crafting screen (crafting table or player inventory)
        boolean isCraftingScreen = self instanceof CraftingScreenHandler || self instanceof PlayerScreenHandler;
        if (!isCraftingScreen) {
            return;
        }
        
        // Check if the slot exists and is a crafting result slot
        if (slotIndex < 0 || slotIndex >= self.slots.size()) {
            return;
        }
        
        Slot slot = self.slots.get(slotIndex);
        if (!(slot instanceof CraftingResultSlot)) {
            return;
        }
        
        // Check if there's actually an item to take
        ItemStack resultStack = slot.getStack();
        if (resultStack.isEmpty()) {
            return;
        }
        
        try {
            // Rule 10: Can't craft items that another player has
            Rule10NoCraftDuplicates rule = (Rule10NoCraftDuplicates) DeathGameMod.getInstance()
                .getRuleManager().getRuleById(10);
            
            if (rule != null && rule.shouldKillOnCraft(serverPlayer, resultStack)) {
                deathgame_shouldKill = true;
                deathgame_playerToKill = serverPlayer;
            }
        } catch (Exception e) {
            // Ignore if game not initialized
        }
    }
    
    @Inject(method = "internalOnSlotClick", at = @At("TAIL"))
    private void onSlotClickTail(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (deathgame_shouldKill && deathgame_playerToKill != null) {
            try {
                Rule10NoCraftDuplicates rule = (Rule10NoCraftDuplicates) DeathGameMod.getInstance()
                    .getRuleManager().getRuleById(10);
                
                if (rule != null) {
                    rule.killPlayerForViolation(deathgame_playerToKill);
                }
            } catch (Exception e) {
                // Ignore
            }
            deathgame_shouldKill = false;
            deathgame_playerToKill = null;
        }
    }
}
