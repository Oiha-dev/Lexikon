package com.oiha.lexikon.mixin;

import com.oiha.lexikon.client.SpellChecker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ArrayList;

import static net.minecraft.client.gui.DrawableHelper.fill;
import static net.minecraft.client.gui.DrawableHelper.drawTextWithShadow;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow
    private TextFieldWidget chatField;
    private SpellChecker spellChecker = new SpellChecker();
    private List<int[]> linesList = new ArrayList<>();
    private List<Long> timestampsList = new ArrayList<>();
    private Object[] currentSuggestionBox = null;
    private List<String> currentSuggestions = new ArrayList<>();
    private int selectedSuggestionIndex = 0;
    private int currentErrorStart = -1;
    private int currentErrorEnd = -1;
    private boolean suggestionApplied = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!suggestionApplied) {
            spellChecker.checkText(this.chatField);
            List<int[]> lines = spellChecker.lines;
            long currentTime = System.currentTimeMillis();
            /*
             * This part run every tick and is used to draw the red underline under the misspelled words
             * It also displays the suggested corrections when the player hovers over the underlined word
             * The lines and timestamps are used to keep track of the red underlines and remove them after some time
             */

            linesList.clear();
            timestampsList.clear();

            for (int[] line : lines) {
                linesList.add(line);
                timestampsList.add(currentTime);
            }

            updateCurrentSuggestions();
        } else {
            suggestionApplied = false;
        }
    }

    private void updateCurrentSuggestions() {
        /*
         * This method is called every tick and is used to update the suggestions list
         * It checks if the mouse is hovering over a suggestion box and updates the suggestions list accordingly
         */
        MinecraftClient client = MinecraftClient.getInstance();
        int mouseX = (int) (client.mouse.getX() / client.getWindow().getScaleFactor());
        int mouseY = (int) (client.mouse.getY() / client.getWindow().getScaleFactor());

        currentSuggestionBox = null;
        currentSuggestions.clear();
        currentErrorStart = -1;
        currentErrorEnd = -1;

        for (Object[] suggestionBox : spellChecker.suggestionsOverlay) {
            if (isMouseOverSuggestion(mouseX, mouseY, suggestionBox)) {
                currentSuggestionBox = suggestionBox;
                updateSuggestionsList(suggestionBox);
                int startIndex = (int) suggestionBox[5];
                int endIndex = (int) suggestionBox[6];
                if (startIndex != -1) {
                    currentErrorStart = startIndex;
                    currentErrorEnd = endIndex;
                }
                break;
            }
        }
    }

    private void updateSuggestionsList(Object[] suggestionBox) {
        /*
         * This method is called every tick and is used to update the suggestions list
         * It updates the suggestions list based on the current suggestion box
         */
        currentSuggestions.clear();

        for (int i = Math.min(2, spellChecker.suggestionsOverlay.size() - 1); i >= 1; i--) {
            Object[] nextSuggestion = spellChecker.suggestionsOverlay.get(i);
            currentSuggestions.add(((String) nextSuggestion[0]).trim());
        }

        String suggestion = (String) suggestionBox[0];
        currentSuggestions.add(suggestion.trim());
    }

    private boolean isMouseOverSuggestion(int mouseX, int mouseY, Object[] suggestion) {
        /*
         * This method is used to check if the mouse is hovering over a suggestion box
         */
        int boxX = (int) suggestion[1];
        int boxY = (int) suggestion[2];
        int boxWidth = (int) suggestion[3];
        int boxHeight = (MinecraftClient.getInstance().textRenderer.fontHeight + 6) * Math.min(3, spellChecker.suggestionsOverlay.size());

        return mouseX >= boxX && mouseX <= (boxX + boxWidth) &&
                mouseY >= (boxY - boxHeight) && mouseY <= boxY;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        /*
         * This method is called every frame and is used to render the red underlines and the suggestions
         * It also removes the red underlines after some time
         */
        long currentTime = System.currentTimeMillis();

        while (!timestampsList.isEmpty() && currentTime - timestampsList.get(0) > 3 * 50) {
            timestampsList.remove(0);
            linesList.remove(0);
        }

        for (int[] line : linesList) {
            fill(matrices, line[0], line[1], line[0] + line[2], line[1] + line[3], line[4]);
        }

        if (!suggestionApplied) {
            renderSuggestions(matrices);
        }
    }

    private void renderSuggestions(MatrixStack matrices) {
        /*
         * This method is called every frame and is used to render the suggestions
         */
        if (currentSuggestionBox == null || currentSuggestions.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int boxX = (int) currentSuggestionBox[1];
        int boxY = (int) currentSuggestionBox[2] - (client.textRenderer.fontHeight + 6) * currentSuggestions.size() - 6;
        int maxWidth = 0;

        for (String suggestion : currentSuggestions) {
            maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(suggestion));
        }

        int boxWidth = Math.max((int) currentSuggestionBox[3], maxWidth + 8);
        int boxHeight = (client.textRenderer.fontHeight + 6) * currentSuggestions.size();

        int screenWidth = client.getWindow().getScaledWidth();
        boxX = Math.min(boxX, screenWidth - boxWidth) - 4;
        boxY = Math.max(boxY, 0) - 4;

        fill(matrices, boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000);

        fill(matrices, boxX, boxY, boxX + boxWidth, boxY + 1, 0xFFFFFFFF);
        fill(matrices, boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
        fill(matrices, boxX, boxY, boxX + 1, boxY + boxHeight, 0xFFFFFFFF);
        fill(matrices, boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);

        for (int i = 0; i < currentSuggestions.size(); i++) {
            int textY = boxY + 3 + i * (client.textRenderer.fontHeight + 6);
            int textColor = (i == selectedSuggestionIndex) ? 0xFFFF00 : 0xFFFFFF;
            if (i == selectedSuggestionIndex) {
                fill(matrices, boxX + 1, textY - 1, boxX + boxWidth - 1, textY + client.textRenderer.fontHeight + 1, 0x80808080);
            }
            drawTextWithShadow(matrices, client.textRenderer, Text.of(currentSuggestions.get(i)), boxX + 4, textY, textColor);
        }
    }

    private void applyCorrection(String correction) {
        /*
         * This method is called when the player selects a suggestion
         * It replaces the misspelled word with the selected suggestion
         */
        if (currentErrorStart >= 0 && currentErrorEnd >= 0 && currentErrorEnd <= chatField.getText().length()) {
            String currentText = chatField.getText();
            String correctedText = currentText.substring(0, currentErrorStart) + correction + currentText.substring(currentErrorEnd);
            chatField.setText(correctedText);
            chatField.setCursor(currentErrorStart + correction.length());

            // Clear suggestions and reset error bounds
            clearSuggestions();

            // Set flag to indicate a suggestion was just applied
            suggestionApplied = true;

            // Force an immediate update of the spell checker
            spellChecker.checkText(this.chatField);
        }
    }

    private void clearSuggestions() {
        currentSuggestions.clear();
        currentErrorStart = -1;
        currentErrorEnd = -1;
        selectedSuggestionIndex = 0;
        spellChecker.suggestionsOverlay.clear();
        currentSuggestionBox = null;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        /*
         * This method is called when the player presses a key
         * It handles the key events for the suggestions
         */
        if (!currentSuggestions.isEmpty()) {
            if (keyCode == 265) {  // Up arrow
                selectedSuggestionIndex = (selectedSuggestionIndex - 1 + currentSuggestions.size()) % currentSuggestions.size();
                cir.setReturnValue(true);
            } else if (keyCode == 264) {  // Down arrow
                selectedSuggestionIndex = (selectedSuggestionIndex + 1) % currentSuggestions.size();
                cir.setReturnValue(true);
            } else if (keyCode == 258) {  // Tab
                if (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < currentSuggestions.size()) {
                    String selectedSuggestion = currentSuggestions.get(selectedSuggestionIndex);
                    applyCorrection(selectedSuggestion);
                }
                cir.setReturnValue(true);
            }
        }
    }
}