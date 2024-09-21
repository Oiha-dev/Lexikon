package com.oiha.lexikon.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.oiha.lexikon.Lexikon;
import com.oiha.lexikon.client.ModConfig;
import com.oiha.lexikon.client.SpellChecker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

import static com.oiha.lexikon.client.ModConfig.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
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
    private static final Identifier SPELLCHECK_ICON = new Identifier("lexikon:textures/gui/"+ ModConfig.iconStyle + ".bmp");
    @Unique
    private static final int ICON_SIZE = 13;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        /*
         * This part run every tick and is used to draw the red underline under the misspelled words
         * It also displays the suggested corrections when the player hovers over the underlined word
         */
        clearSuggestions();
        spellChecker.checkText(this.chatField);
        List<int[]> lines = spellChecker.lines;
        linesList.clear();

        linesList.addAll(lines);
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
         * or if the chat cursor is on a misspelled word
         */

        int boxX = (int) suggestion[1];
        int boxY = (int) suggestion[2];
        int boxWidth = (int) suggestion[3];
        int boxHeight = (MinecraftClient.getInstance().textRenderer.fontHeight + 4) * Math.min(3, spellChecker.suggestionsOverlay.size()) + 9 + ICON_SIZE;

        return (mouseX >= boxX && mouseX <= (boxX + boxWidth) &&
                mouseY >= (boxY - boxHeight) && mouseY <= boxY) || (int)suggestion[5] <= chatField.getCursor() && (int)suggestion[6] >= chatField.getCursor();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) throws InstantiationException, IllegalAccessException {
        /*
         * This method is called every frame and is used to render the red underlines and the suggestions
         * It also removes the red underlines after some time
         */

        for (int[] line : linesList) {
            int x1 = max(line[0], 4);
            int x2 = min(line[0] + line[2], MinecraftClient.getInstance().getWindow().getScaledWidth() - 4); // This prevents the underline from going off chat box
            if (underlineStyle.equals("Straight")) {
                fill(matrices, x1, line[1], x2, line[1] + line[3], line[4]);
            }
            else if (underlineStyle.equals("Wavy")) {
                boolean up = true;
                for (int i = line[0]; i < x2; i += 2) {
                    int yOffset = up ? -1 : 0;
                    fill(matrices, i, line[1] + yOffset, i + 2, line[1] + yOffset + line[3], line[4]);
                    up = !up;
                }
            }
            else if (underlineStyle.equals("Dotted")) {
                for (int i = line[0]; i < x2; i += 2) {
                    fill(matrices, i, line[1], i + 1, line[1] + line[3], line[4]);
                }
            }
        }

        renderSuggestions(matrices);

        if (RuleDescription != null && showRuleDescription &&
                currentErrorStart >= 0 && currentErrorEnd <= chatField.getText().length() &&
                currentErrorStart < currentErrorEnd &&
                lastRuleWord.equals(chatField.getText().substring(currentErrorStart, currentErrorEnd))) {
            renderRuleDescription(matrices);
        }

        if (ModConfig.flagButtonEnabled) {
            drawFlagIcon(matrices, MinecraftClient.getInstance().getWindow().getScaledWidth() - 18 - 3, MinecraftClient.getInstance().getWindow().getScaledHeight() - 15 - 12, 18, 12, ModConfig.currentLanguage);
        }
    }

    @Unique
    private void applyCorrection(String correction) {
        /*
         * This method is called when the player selects a suggestion
         * It replaces the misspelled word with the selected suggestion
         */
        if (!Objects.equals(correction, "No suggestions")){
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
            maxWidth = max(maxWidth, client.textRenderer.getWidth(suggestion));
        }

        int boxWidth = max((int) currentSuggestionBox[3], maxWidth + 8);
        int boxHeight = (client.textRenderer.fontHeight + 4) * currentSuggestions.size();

        int screenWidth = client.getWindow().getScaledWidth();
        boxX = Math.min(boxX, screenWidth - boxWidth) - 4;
        boxY = max(boxY, 0) - 10;
        

        fill(matrices, boxX, boxY, boxX + boxWidth, boxY + boxHeight, ModConfig.suggestionBackgroundColor.getRGB());
        int OutlineColor = ModConfig.outlineEnabled ? ModConfig.outlineColor.getRGB() : 0x00000000;
        fill(matrices, boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, OutlineColor);
        fill(matrices, boxX, boxY, boxX + 1, boxY + boxHeight, OutlineColor);
        fill(matrices, boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, OutlineColor);


        if (!ModConfig.dictionaryEnabled) {
            fill(matrices, boxX, boxY, boxX + boxWidth, boxY + 1, OutlineColor); // Top border
        }

        // Render the spellcheck icon
        if (ModConfig.dictionaryEnabled) {
            fill(matrices, boxX + 1, boxY, boxX + ICON_SIZE + 1, boxY - ICON_SIZE + 1, ModConfig.suggestionBackgroundColor.getRGB()); // Background
            drawBmpIcon(matrices, boxX + 1, boxY - ICON_SIZE + 2, ICON_SIZE, ICON_SIZE);
            fill(matrices, boxX, boxY, boxX + 1, boxY - ICON_SIZE + 1, OutlineColor); // Horizontal line
            fill(matrices, boxX, boxY - ICON_SIZE + 1, boxX + ICON_SIZE + 1, boxY - ICON_SIZE + 2, OutlineColor); // Upper line
            fill(matrices, boxX + ICON_SIZE + 1, boxY, boxX + ICON_SIZE + 2, boxY - ICON_SIZE + 1, OutlineColor);// Horizontal line 2
            fill(matrices, boxX + ICON_SIZE + 1, boxY, boxX + boxWidth, boxY + 1, OutlineColor); // Bottom line
        }

        for (int i = 0; i < currentSuggestions.size(); i++) {
            int textY = boxY + 11 + i * (client.textRenderer.fontHeight + 4);
            int textColor = (i == selectedSuggestionIndex) ? ModConfig.chosenSuggestionColor.getRGB() : ModConfig.suggestionColor.getRGB();
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
        fill(matrices, boxX, boxY, boxX + boxWidth + 1, boxY + boxHeight + 1, ModConfig.suggestionBackgroundColor.getRGB()); // Background
        int OutlineColor = ModConfig.outlineEnabled ? ModConfig.outlineColor.getRGB() : 0x00000000;
        fill(matrices, boxX, boxY, boxX + boxWidth + 1, boxY + 1, OutlineColor); // Bottom line
        fill(matrices, boxX, boxY, boxX + 1, boxY + boxHeight, OutlineColor); // Left line
        fill(matrices, boxX + boxWidth, boxY, boxX + boxWidth + 1, boxY + boxHeight, OutlineColor); // Right line
        fill(matrices, boxX, boxY + boxHeight, boxX + boxWidth + 1, boxY + boxHeight + 1, OutlineColor); // Top line

        drawTextWithShadow(matrices, client.textRenderer, Text.of(RuleDescription), boxX + 4, boxY + 3, ModConfig.suggestionColor.getRGB());
    }

    @Unique
    private void clearSuggestions() {
        currentSuggestions.clear();
        currentErrorStart = 0;
        currentErrorEnd = 0;
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
    public void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) throws IOException {
        if (currentSuggestionBox != null && !currentSuggestions.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            int boxX = (int) currentSuggestionBox[1];
            int boxY = (int) currentSuggestionBox[2] - (client.textRenderer.fontHeight + 3) * (currentSuggestions.size() + 1) + 1;
            int maxWidth = 0;

            for (String suggestion : currentSuggestions) {
                maxWidth = max(maxWidth, client.textRenderer.getWidth(suggestion));
            }

            int boxWidth = max((int) currentSuggestionBox[3], maxWidth + 8);
            int boxHeight = (client.textRenderer.fontHeight + 3) * (currentSuggestions.size());

            int screenWidth = client.getWindow().getScaledWidth();
            boxX = Math.min(boxX, screenWidth - boxWidth) - 4;
            boxY = max(boxY, 0);

            // Check if the click is on the icon
            if (mouseY <= boxY && mouseY >= boxY - ICON_SIZE) {
                String errorWord = chatField.getText().substring(currentErrorStart, currentErrorEnd);

                // Save the word to the personal dictionary if it is not already there
                if (!Lexikon.personalDictionary.contains(errorWord)) {
                    Lexikon.personalDictionary.add(errorWord);
                    FileWriter writer = new FileWriter("config/Lexikon/personalDictionary.txt", true);
                    writer.write(errorWord + "\n");
                    writer.close();
                }

                cir.setReturnValue(true);
                return;
            }

            if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + boxHeight) {
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

    @Unique
    private void drawBmpIcon(MatrixStack matrices, int x, int y, int width, int height) {
        // Load the icon texture from a bmp file and only draw the white pixels with the specified color
        try {
            InputStream stream = MinecraftClient.getInstance().getResourceManager().getResource(SPELLCHECK_ICON).get().getInputStream();
            NativeImage image = NativeImage.read(stream);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int color = image.getColor(i, j);
                    if (color == 0xFFFFFFFF) {
                        fill(matrices, x + i, y + j, x + i + 1, y + j + 1, ModConfig.dictionaryIconColor.getRGB());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Unique
    private void drawFlagIcon(MatrixStack matrices, int x, int y, int width, int height, String flag) {
        MinecraftClient client = MinecraftClient.getInstance();
        Identifier flagIdentifier = new Identifier("lexikon:textures/flag/" + ISOLanguages.get(possibleLanguages.indexOf(flag)).toLowerCase() + ".png");

        try {
            client.getResourceManager().getResource(flagIdentifier);

            client.getTextureManager().bindTexture(flagIdentifier);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, flagIdentifier);

            DrawableHelper.drawTexture(matrices, x, y, 0, 0, width, height, width, height);

            RenderSystem.disableBlend();
        } catch (Exception e) {
            Lexikon.LOGGER.error("Failed to render flag icon: " + flagIdentifier, e);
        }
    }
}