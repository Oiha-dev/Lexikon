package com.oiha.lexikon.mixin;

import com.oiha.lexikon.client.SpellChecker;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow private TextFieldWidget chatField;
    private SpellChecker spellChecker = new SpellChecker();
    private List<int[]> linesList = new ArrayList<>();
    private List<Long> timestampsList = new ArrayList<>();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        spellChecker.checkText(this.chatField);
        List<int[]> lines = spellChecker.lines;
        long currentTime = System.currentTimeMillis();

        // Clear the lists before adding new lines
        linesList.clear();
        timestampsList.clear();

        // Add the current lines to the lines list and the current time to the timestamps list
        for (int[] line : lines) {
            linesList.add(line);
            timestampsList.add(currentTime);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        long currentTime = System.currentTimeMillis();

        // If the line at the front of the list is older than 3 ticks, remove it
        while (!timestampsList.isEmpty() && currentTime - timestampsList.get(0) > 3 * 50) {
            timestampsList.remove(0);
            linesList.remove(0);
        }

        // Draw the lines that are still in the lines list
        for (int[] line : linesList) {
            DrawableHelper.fill(matrices, line[0], line[1], line[0] + line[2], line[1] + line[3], line[4]);
        }
    }
}