package com.oiha.lexikon.mixin;

import com.oiha.lexikon.client.SpellChecker;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow private TextFieldWidget chatField;
    private long lastTickTime = System.currentTimeMillis();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        long currentTickTime = System.currentTimeMillis();
        if (currentTickTime - lastTickTime >= 1000) {
            String currentText = this.chatField.getText();
            new SpellChecker(currentText);
            lastTickTime = currentTickTime;
        }
        int xCoordinate = getXCoordinateOfWord("hello");
        System.out.println("The index of the first character is " + xCoordinate);
    }

    private int getXCoordinateOfWord(String word) {
    /* 1. Get the current text in the chat field
       2. Get the index of the first character drawn on the screen
       3. Look if the word is present in the rendered text, if not return -1
       4. If the word is present, return the x-coordinate of the first character of the word
     */

    }
}