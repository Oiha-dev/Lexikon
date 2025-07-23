package com.oiha.lexikon.mixin;

import com.oiha.lexikon.Lexikon;
import com.oiha.lexikon.client.ModConfig;
import com.oiha.lexikon.client.SpellChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.components.EditBox;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow
    protected EditBox input;
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
    private static final ResourceLocation SPELLCHECK_ICON = ResourceLocation.fromNamespaceAndPath("lexikon", "textures/gui/" + ModConfig.iconStyle + ".png");
    @Unique
    private static final int ICON_SIZE = 13;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void renderSuggestions(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        clearSuggestions();

        spellChecker.checkText(this.input);
        List<int[]> lines = spellChecker.lines;
        linesList.clear();
        linesList.addAll(lines);
        updateCurrentSuggestions();

        //This is Claude's code, thank you Claude :)
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        onRender(guiGraphics);
        guiGraphics.pose().popPose();
    }

    @Unique
    private void updateCurrentSuggestions() {
        /*
         * This method is called every tick and is used to update the suggestions list
         * It checks if the mouse is hovering over a suggestion box and updates the suggestions list accordingly
         */
        Minecraft client = Minecraft.getInstance();
        int mouseX = (int) (client.mouseHandler.xpos() / client.getWindow().getGuiScale());
        int mouseY = (int) (client.mouseHandler.ypos() / client.getWindow().getGuiScale());

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
        int boxHeight = (Minecraft.getInstance().font.lineHeight + 4) * Math.min(3, spellChecker.suggestionsOverlay.size()) + 9 + ICON_SIZE;

        return (mouseX >= boxX && mouseX <= (boxX + boxWidth) &&
                mouseY >= (boxY - boxHeight) && mouseY <= boxY) || (int)suggestion[5] <= input.getCursorPosition() && (int)suggestion[6] >= input.getCursorPosition();
    }

    private void onRender(GuiGraphics guiGraphics) {
        /*
         * This method is called every frame and is used to render the red underlines and the suggestions
         * It also removes the red underlines after some time
         */

        for (int[] line : linesList) {
            int x1 = max(line[0], 4);
            int x2 = min(line[0] + line[2], Minecraft.getInstance().getWindow().getGuiScaledWidth() - 4); // This prevents the underline from going off chat box
            if (ModConfig.underlineStyle.equals("Straight")) {
                guiGraphics.fill(x1, line[1], x2, line[1] + line[3], line[4]);
            }
            else if (ModConfig.underlineStyle.equals("Wavy")) {
                boolean up = true;
                for (int i = line[0]; i < x2; i += 2) {
                    int yOffset = up ? -1 : 0;
                    guiGraphics.fill(i, line[1] + yOffset, i + 2, line[1] + yOffset + line[3], line[4]);
                    up = !up;
                }
            }
            else if (ModConfig.underlineStyle.equals("Dotted")) {
                for (int i = line[0]; i < x2; i += 2) {
                    guiGraphics.fill(i, line[1], i + 1, line[1] + line[3], line[4]);
                }
            }
        }

        renderSuggestions(guiGraphics);

        if (RuleDescription != null && showRuleDescription &&
                currentErrorStart >= 0 && currentErrorEnd <= input.getValue().length() &&
                currentErrorStart < currentErrorEnd &&
                lastRuleWord.equals(input.getValue().substring(currentErrorStart, currentErrorEnd))) {
            renderRuleDescription(guiGraphics);
        }

        if (ModConfig.flagButtonEnabled) {
            drawFlagIcon(guiGraphics, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 18 - 3, Minecraft.getInstance().getWindow().getGuiScaledHeight() - 15 - 12, 18, 12, ModConfig.currentLanguage);
        }
    }

    @Unique
    private void applyCorrection(String correction) {
        /*
         * This method is called when the player selects a suggestion
         * It replaces the misspelled word with the selected suggestion
         */
        if (!Objects.equals(correction, "No suggestions")){
            if (currentErrorStart >= 0 && currentErrorEnd >= 0 && currentErrorEnd <= input.getValue().length()) {
                String currentText = input.getValue();
                String correctedText = currentText.substring(0, currentErrorStart) + correction + currentText.substring(currentErrorEnd);
                input.setValue(correctedText);
                input.setCursorPosition(currentErrorStart + correction.length());
                input.setHighlightPos(input.getCursorPosition());

                // Clear suggestions and reset error bounds
                clearSuggestions();

                // Force an immediate update of the spell checker
                spellChecker.checkText(this.input);
            }
        }
    }

    @Unique
    private void renderSuggestions(GuiGraphics guiGraphics) {
        if (currentSuggestionBox == null || currentSuggestions.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        int boxX = (int) currentSuggestionBox[1];
        int boxY = (int) currentSuggestionBox[2] - (client.font.lineHeight + 4) * currentSuggestions.size();
        int maxWidth = 0;

        for (String suggestion : currentSuggestions) {
            maxWidth = max(maxWidth, client.font.width(suggestion));
        }

        int boxWidth = max((int) currentSuggestionBox[3], maxWidth + 8);
        int boxHeight = (client.font.lineHeight + 4) * currentSuggestions.size();

        int screenWidth = client.getWindow().getGuiScaledWidth();
        boxX = Math.min(boxX, screenWidth - boxWidth) - 4;
        boxY = max(boxY, 0) - 10;

        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, ModConfig.suggestionBackgroundColor.getRGB());
        int OutlineColor = ModConfig.outlineEnabled ? ModConfig.outlineColor.getRGB() : 0x00000000;
        guiGraphics.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, OutlineColor);
        guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, OutlineColor);
        guiGraphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, OutlineColor);

        if (!ModConfig.dictionaryEnabled) {
            guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, OutlineColor); // Top border
        }

        // Render the spellcheck icon
        if (ModConfig.dictionaryEnabled) {
            guiGraphics.fill(boxX + 1, boxY, boxX + ICON_SIZE + 1, boxY - ICON_SIZE + 1, ModConfig.suggestionBackgroundColor.getRGB()); // Background
            drawPNGIcon(guiGraphics, boxX + 1, boxY - ICON_SIZE + 2, ICON_SIZE, ICON_SIZE);
            guiGraphics.fill(boxX, boxY, boxX + 1, boxY - ICON_SIZE + 1, OutlineColor); // Horizontal line
            guiGraphics.fill(boxX, boxY - ICON_SIZE + 1, boxX + ICON_SIZE + 1, boxY - ICON_SIZE + 2, OutlineColor); // Upper line
            guiGraphics.fill(boxX + ICON_SIZE + 1, boxY, boxX + ICON_SIZE + 2, boxY - ICON_SIZE + 1, OutlineColor);// Horizontal line 2
            guiGraphics.fill(boxX + ICON_SIZE + 1, boxY, boxX + boxWidth, boxY + 1, OutlineColor); // Bottom line
        }

        for (int i = 0; i < currentSuggestions.size(); i++) {
            int textY = boxY + 11 + i * (client.font.lineHeight + 4);
            int textColor = (i == selectedSuggestionIndex) ? ModConfig.chosenSuggestionColor.getRGB() : ModConfig.suggestionColor.getRGB();
            if (i == selectedSuggestionIndex) {
                guiGraphics.fill(boxX + 1, textY - 10, boxX + boxWidth - 1, textY + client.font.lineHeight - 8, 0x80808080);
            }
            guiGraphics.drawString(client.font, Component.literal(currentSuggestions.get(i)), boxX + 4, textY - 8, textColor, true);
        }
    }

    @Unique
    private void renderRuleDescription(GuiGraphics guiGraphics) {
        if (currentSuggestionBox == null || currentSuggestions.isEmpty()) return;
        /*
         * This method is called when the player right clicks on a suggestion
         * It displays the rule description above the suggestions
         */

        Minecraft client = Minecraft.getInstance();
        int boxX = (int) currentSuggestionBox[1] - 4;
        int boxY = (int) currentSuggestionBox[2] - (client.font.lineHeight + 4) * currentSuggestions.size() - ICON_SIZE * 2 - 11;
        int boxWidth = client.font.width(RuleDescription) + 7;
        int boxHeight = (client.font.lineHeight + 4);
        guiGraphics.fill(boxX, boxY, boxX + boxWidth + 1, boxY + boxHeight + 1, ModConfig.suggestionBackgroundColor.getRGB()); // Background
        int OutlineColor = ModConfig.outlineEnabled ? ModConfig.outlineColor.getRGB() : 0x00000000;
        guiGraphics.fill(boxX, boxY, boxX + boxWidth + 1, boxY + 1, OutlineColor); // Bottom line
        guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, OutlineColor); // Left line
        guiGraphics.fill(boxX + boxWidth, boxY, boxX + boxWidth + 1, boxY + boxHeight, OutlineColor); // Right line
        guiGraphics.fill(boxX, boxY + boxHeight, boxX + boxWidth + 1, boxY + boxHeight + 1, OutlineColor); // Top line

        guiGraphics.drawString(client.font, Component.literal(RuleDescription), boxX + 4, boxY + 3, ModConfig.suggestionColor.getRGB(), true);
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
            Minecraft client = Minecraft.getInstance();
            int boxX = (int) currentSuggestionBox[1];
            int boxY = (int) currentSuggestionBox[2] - (client.font.lineHeight + 3) * (currentSuggestions.size() + 1) + 1;
            int maxWidth = 0;

            for (String suggestion : currentSuggestions) {
                maxWidth = max(maxWidth, client.font.width(suggestion));
            }

            int boxWidth = max((int) currentSuggestionBox[3], maxWidth + 8);
            int boxHeight = (client.font.lineHeight + 3) * (currentSuggestions.size());

            int screenWidth = client.getWindow().getGuiScaledWidth();
            boxX = Math.min(boxX, screenWidth - boxWidth) - 4;
            boxY = max(boxY, 0);

            // Check if the click is on the icon
            if (mouseY <= boxY && mouseY >= boxY - ICON_SIZE) {
                String errorWord = input.getValue().substring(currentErrorStart, currentErrorEnd);

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
                int selectedIndex = (int) ((mouseY - (boxY + 1)) / (client.font.lineHeight + 4));
                if (selectedIndex >= 0 && selectedIndex < currentSuggestions.size()) {
                    if (button == 0) {
                        // Left click to apply suggestion
                        applyCorrection(currentSuggestions.get(selectedIndex));
                        cir.setReturnValue(true);
                    } else if (button == 1) {
                        // Right click to show the rule description above the suggestions
                        RuleDescription = (String) spellChecker.suggestionsOverlay.get(selectedIndex)[7];
                        showRuleDescription = true;
                        lastRuleWord = input.getValue().substring(currentErrorStart, currentErrorEnd);
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Unique
    private void drawPNGIcon(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        try {
            InputStream stream = Minecraft.getInstance().getResourceManager().getResource(SPELLCHECK_ICON).get().open();
            NativeImage image = NativeImage.read(stream);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int color = image.getPixelRGBA(i, j);
                    if (color == 0xFFFFFFFF) {
                        guiGraphics.fill(x + i, y + j, x + i + 1, y + j + 1, ModConfig.dictionaryIconColor.getRGB());
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    @Unique
    private void drawFlagIcon(GuiGraphics guiGraphics, int x, int y, int width, int height, String flag) {
        ResourceLocation flagIdentifier = ResourceLocation.fromNamespaceAndPath("lexikon", "textures/flag/" + ModConfig.ISOLanguages.get(ModConfig.possibleLanguages.indexOf(flag)).toLowerCase() + ".png");

        guiGraphics.blit(flagIdentifier, x, y, 0, 0, width, height, width, height);
    }
}