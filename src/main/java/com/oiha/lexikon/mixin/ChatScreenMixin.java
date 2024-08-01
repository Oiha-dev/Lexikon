package com.oiha.lexikon.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oiha.lexikon.client.SpellChecker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ArrayList;

import static net.minecraft.client.gui.DrawableHelper.*;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow
    protected TextFieldWidget chatField;
    @Unique
    private SpellChecker spellChecker = new SpellChecker();
    @Unique
    private List<int[]> linesList = new ArrayList<>();
    @Unique
    private List<Long> timestampsList = new ArrayList<>();
    @Unique
    private Object[] currentSuggestionBox = null;
    @Unique
    private List<String> currentSuggestions = new ArrayList<>();
    @Unique
    private int selectedSuggestionIndex = 0;
    @Unique
    private int currentErrorStart = 0;
    @Unique
    private int currentErrorEnd = 0;
    @Unique
    private String RuleDescription = null;
    @Unique
    private boolean showRuleDescription = false;
    @Unique
    private String lastRuleWord = "";
    @Unique
    private static final Identifier SPELLCHECK_ICON = new Identifier("lexikon:textures/gui/floppydisk.png");
    @Unique
    private static final int ICON_SIZE = 13;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        clearSuggestions();
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
    }

    @Unique
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
        currentErrorStart = 0;
        currentErrorEnd = 0;

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

    @Unique
    private void updateSuggestionsList(Object[] suggestionBox) {
        /*
         * This method is called every tick and is used to update the suggestions list
         * It updates the suggestions list based on the current suggestion box
         */

        for (int i = Math.min(2, spellChecker.suggestionsOverlay.size() - 1); i >= 1; i--) {
            Object[] nextSuggestion = spellChecker.suggestionsOverlay.get(i);
            currentSuggestions.add(((String) nextSuggestion[0]).trim());
        }

        String suggestion = (String) suggestionBox[0];
        currentSuggestions.add(suggestion.trim());
    }

    @Unique
    private boolean isMouseOverSuggestion(int mouseX, int mouseY, Object[] suggestion) {
        /*
         * This method is used to check if the mouse is hovering over a suggestion box
         */
        int boxX = (int) suggestion[1];
        int boxY = (int) suggestion[2];
        int boxWidth = (int) suggestion[3];
        int boxHeight = (MinecraftClient.getInstance().textRenderer.fontHeight + 4) * Math.min(3, spellChecker.suggestionsOverlay.size()) + 9 + ICON_SIZE;

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

        renderSuggestions(matrices);

        if (RuleDescription != null && showRuleDescription && lastRuleWord.equals(chatField.getText().substring(currentErrorStart, currentErrorEnd))) {
            renderRuleDescription(matrices);
        }
    }

    @Unique
    private void applyCorrection(String correction) {
        /*
         * This method is called when the player selects a suggestion
         * It replaces the misspelled word with the selected suggestion
         */
        if (correction != "No suggestions"){
            if (currentErrorStart >= 0 && currentErrorEnd >= 0 && currentErrorEnd <= chatField.getText().length()) {
                String currentText = chatField.getText();
                String correctedText = currentText.substring(0, currentErrorStart) + correction + currentText.substring(currentErrorEnd);
                chatField.setText(correctedText);
                chatField.setCursor(currentErrorStart + correction.length());

                // Clear suggestions and reset error bounds
                clearSuggestions();

                // Force an immediate update of the spell checker
                spellChecker.checkText(this.chatField);
            }
        }
    }

    @Unique
    private void renderSuggestions(MatrixStack matrices) {
        if (currentSuggestionBox == null || currentSuggestions.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int boxX = (int) currentSuggestionBox[1];
        int boxY = (int) currentSuggestionBox[2] - (client.textRenderer.fontHeight + 4) * currentSuggestions.size();
        int maxWidth = 0;

        for (String suggestion : currentSuggestions) {
            maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(suggestion));
        }

        int boxWidth = Math.max((int) currentSuggestionBox[3], maxWidth + 8);
        int boxHeight = (client.textRenderer.fontHeight + 4) * currentSuggestions.size();

        int screenWidth = client.getWindow().getScaledWidth();
        boxX = Math.min(boxX, screenWidth - boxWidth) - 4;
        boxY = Math.max(boxY, 0) - 10;

        fill(matrices, boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000);
        fill(matrices, boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
        fill(matrices, boxX, boxY, boxX + 1, boxY + boxHeight, 0xFFFFFFFF);
        fill(matrices, boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
        //fill(matrices, boxX, boxY, boxX + boxWidth, boxY + 1, 0xFFFFFFFF); // Top border

        // Render the spellcheck icon
        fill(matrices, boxX + 1, boxY, boxX + ICON_SIZE + 1, boxY - ICON_SIZE + 1, 0xE0000000); // Background
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, SPELLCHECK_ICON);
        drawTexture(matrices, boxX + 1, boxY - ICON_SIZE + 2, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE); // Icon
        fill(matrices, boxX, boxY, boxX + 1, boxY - ICON_SIZE + 1, 0xFFFFFFFF); // Horizontal line
        fill(matrices, boxX, boxY - ICON_SIZE + 1, boxX + ICON_SIZE + 1, boxY - ICON_SIZE + 2, 0xFFFFFFFF); // Upper line
        fill(matrices, boxX + ICON_SIZE + 1 , boxY, boxX + ICON_SIZE + 2, boxY - ICON_SIZE + 1, 0xFFFFFFFF);// Horizontal line 2
        fill(matrices, boxX + ICON_SIZE + 1, boxY, boxX + boxWidth, boxY + 1, 0xFFFFFFFF); // Bottom line

        for (int i = 0; i < currentSuggestions.size(); i++) {
            int textY = boxY + 11 + i * (client.textRenderer.fontHeight + 4);
            int textColor = (i == selectedSuggestionIndex) ? 0xFFFF00 : 0xFFFFFF;
            if (i == selectedSuggestionIndex) {
                fill(matrices, boxX + 1, textY - 10, boxX + boxWidth - 1, textY + client.textRenderer.fontHeight - 8, 0x80808080);
            }
            drawTextWithShadow(matrices, client.textRenderer, Text.of(currentSuggestions.get(i)), boxX + 4, textY - 8, textColor);
        }
    }

    @Unique
    private void renderRuleDescription(MatrixStack matrices) {
        if (currentSuggestionBox == null || currentSuggestions.isEmpty()) return;
        /*
         * This method is called when the player right clicks on a suggestion
         * It displays the rule description above the suggestions
         */

        MinecraftClient client = MinecraftClient.getInstance();
        int boxX = (int) currentSuggestionBox[1] - 4;
        int boxY = (int) currentSuggestionBox[2] - (client.textRenderer.fontHeight + 4) * currentSuggestions.size() - ICON_SIZE * 2 - 11;
        int boxWidth = client.textRenderer.getWidth(RuleDescription) + 7;
        int boxHeight = (client.textRenderer.fontHeight + 4);
        fill(matrices, boxX, boxY, boxX + boxWidth + 1, boxY + boxHeight + 1, 0xE0000000); // Background
        fill(matrices, boxX, boxY, boxX + boxWidth + 1, boxY + 1, 0xFFFFFFFF); // Bottom line
        fill(matrices, boxX, boxY, boxX + 1, boxY + boxHeight, 0xFFFFFFFF); // Left line
        fill(matrices, boxX + boxWidth, boxY, boxX + boxWidth + 1, boxY + boxHeight, 0xFFFFFFFF); // Right line
        fill(matrices, boxX, boxY + boxHeight, boxX + boxWidth + 1, boxY + boxHeight + 1, 0xFFFFFFFF); // Top line

        drawTextWithShadow(matrices, client.textRenderer, Text.of(RuleDescription), boxX + 4, boxY + 3, 0xFFFFFF);
    }

    @Unique
    private void clearSuggestions() {
        currentSuggestions.clear();
        currentErrorStart = 0;
        currentErrorEnd = 0;
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
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (currentSuggestionBox != null && !currentSuggestions.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            int boxX = (int) currentSuggestionBox[1];
            int boxY = (int) currentSuggestionBox[2] - (client.textRenderer.fontHeight + 3) * (currentSuggestions.size() + 1) + 1;
            int maxWidth = 0;

            for (String suggestion : currentSuggestions) {
                maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(suggestion));
            }

            int boxWidth = Math.max((int) currentSuggestionBox[3], maxWidth + 8);
            int boxHeight = (client.textRenderer.fontHeight + 3) * (currentSuggestions.size());

            int screenWidth = client.getWindow().getScaledWidth();
            boxX = Math.min(boxX, screenWidth - boxWidth) - 4;
            boxY = Math.max(boxY, 0);

            // Check if the click is on the icon
            if (mouseY <= boxY && mouseY >= boxY - ICON_SIZE) {
                String errorWord = chatField.getText().substring(currentErrorStart, currentErrorEnd);
                System.out.println(errorWord);
                List<String> minecraftNames = new ArrayList<>();
                for(Item item : Registry.ITEM){
                    minecraftNames.add(item.getName().getString());
                }

                System.out.println(minecraftNames.size());


                for (String name : minecraftNames) {
                    System.out.println(name);
                }
                cir.setReturnValue(true);
                return;
            }

            if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + boxHeight) {
                // Handle suggestion selection
                int selectedIndex = (int) ((mouseY - (boxY + 1)) / (client.textRenderer.fontHeight + 4));
                if (selectedIndex >= 0 && selectedIndex < currentSuggestions.size()) {
                    if (button == 0) {
                        // Left click to apply suggestion
                        applyCorrection(currentSuggestions.get(selectedIndex));
                        cir.setReturnValue(true);
                    } else if (button == 1) {
                        // Right click to show the rule description above the suggestions
                        RuleDescription = (String) spellChecker.suggestionsOverlay.get(selectedIndex)[7];
                        showRuleDescription = true;
                        lastRuleWord = chatField.getText().substring(currentErrorStart, currentErrorEnd);
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}