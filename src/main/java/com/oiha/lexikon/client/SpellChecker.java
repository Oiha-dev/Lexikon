package com.oiha.lexikon.client;

import com.oiha.lexikon.Lexikon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.languagetool.rules.RuleMatch;

import com.oiha.lexikon.mixin.TextFieldWidgetMixin;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpellChecker {
    private ExecutorService executorService;
    private long lastTickTime = System.currentTimeMillis();
    public List<int[]> lines = new ArrayList<>();

    public List<Object[]> suggestionsOverlay = new ArrayList<Object[]>();

    ArrayList<RuleMatch> spellcheckList = new ArrayList<>();

    public SpellChecker() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public void checkText(TextFieldWidget chatField, MatrixStack matrice) {
        /*
         * This method is called every tick but the first part of the code is limited to run once per second
         * This is to prevent the spell checker from running too frequently and causing fps drops
         * I'm using the LanguageTool library to check the text for potential errors and the suggested corrections
         * the errors and suggestions are added to a Arraylist and can be used to draw the red underline and display the suggestions
         */
        long currentTickTime = System.currentTimeMillis();
        String text = chatField.getText();

        if (!text.isEmpty() && text.charAt(0) != '/') {
            if (currentTickTime - lastTickTime >= 1000) {
                executorService.submit(() -> {
                    try {
                        List<RuleMatch> matches = Lexikon.getLangTool().check(text);
                        spellcheckList.clear();
                        for (RuleMatch match : matches) {
                            spellcheckList.add(match);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                lastTickTime = currentTickTime;
            }
            lines.clear();
            /*
             * This part run every tick and is used to draw the red underline under the misspelled words
             * It also displays the suggested corrections when the player hovers over the underlined word
             */
            for (RuleMatch match : spellcheckList) {
                int from = match.getFromPos();
                int to = match.getToPos();

                if (from < 0 || to > text.length() || from > to) {
                    continue;
                }

                int scrollOffset = MinecraftClient.getInstance().textRenderer.getWidth(text.substring(0, ((TextFieldWidgetMixin) chatField).getFirstCharacterIndex()));
                int x = MinecraftClient.getInstance().textRenderer.getWidth(text.substring(0, from)) + 4 - scrollOffset;
                int y = MinecraftClient.getInstance().getWindow().getScaledHeight() - 4;
                int width = MinecraftClient.getInstance().textRenderer.getWidth(text.substring(from, to));
                int height = 1;
                lines.add(new int[]{x, y, width, height, 0xFFFF0000});

                /*
                 * This part is used to draw the suggestions when the player hovers over the underlined word
                 */
                int mouseX = (int) MinecraftClient.getInstance().mouse.getX();
                int mouseY = (int) MinecraftClient.getInstance().mouse.getY();
                int guiScale = (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
                int textHeight = MinecraftClient.getInstance().textRenderer.fontHeight;

                // Debug by printing mouse position
                System.out.println("Mouse X: " + mouseX);
                System.out.println("Mouse Y: " + mouseY);


                if (mouseX >= x*guiScale && mouseX <= (x + width)*guiScale && mouseY <= y*guiScale && mouseY >= (y - textHeight)*guiScale) {
                    List<String> suggestions = match.getSuggestedReplacements();
                    if (!suggestions.isEmpty()) {
                        int boxX = x;
                        int boxY = y;
                        int boxWidth = width;
                        int boxHeight = textHeight * guiScale;
                        suggestionsOverlay.clear();
                        suggestionsOverlay.add(new Object[]{suggestions.get(0), boxX, boxY, boxWidth, boxHeight});

                    }
                }
            }
        } else {
            // Clear the lines list when the text is empty
            lines.clear();
        }
    }
}