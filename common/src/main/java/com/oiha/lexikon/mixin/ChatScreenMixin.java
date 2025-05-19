package com.oiha.lexikon.mixin;

import com.oiha.lexikon.SpellChecker;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Objects;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Unique
    private String lexikon$prevMessage = "";

    @Inject(method = "onEdited" , at = @At("HEAD"))
    private void onEdited(String message, CallbackInfo ci) throws IOException {
        if (!Objects.equals(message, lexikon$prevMessage)) {
            SpellChecker.getInstance().check(message);
            lexikon$prevMessage = message;
        }
    }
}
