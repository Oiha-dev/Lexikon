package com.oiha.lexikon.client;

import com.oiha.lexikon.Lexikon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.languagetool.rules.RuleMatch;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpellChecker {
    private ExecutorService executorService;
    private long lastTickTime = System.currentTimeMillis();
    public List<int[]> lines = new ArrayList<>();
    ArrayList<RuleMatch> spellcheckList = new ArrayList<>();

    public SpellChecker() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public void checkText(TextFieldWidget chatField) {
        /*
         * This method is called every tick but the first part of the code is limited to run once per second
         * This is to prevent the spell checker from running too frequently and causing fps drops
         * I'm using the LanguageTool library to check the text for potential errors and the suggested corrections
         * the errors and suggestions are added to a Arraylist and can be used to draw the red underline and display the suggestions
         */
        long currentTickTime = System.currentTimeMillis();
        String text = chatField.getText();

        if (!text.isEmpty()) {
            if (currentTickTime - lastTickTime >= 1000) {
                executorService.submit(() -> {
                    try {
                        List<RuleMatch> matches = Lexikon.getLangTool().check(text);
                        spellcheckList.clear();
                        for (RuleMatch match : matches) {
                            spellcheckList.add(match);
                        }
                        System.out.println(spellcheckList);
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

                int x = MinecraftClient.getInstance().textRenderer.getWidth(text.substring(0, from)) + 4;
                int y = MinecraftClient.getInstance().getWindow().getScaledHeight() - (4 * MinecraftClient.getInstance().options.guiScale) + MinecraftClient.getInstance().textRenderer.fontHeight;
                int width = MinecraftClient.getInstance().textRenderer.getWidth(text.substring(from, to));
                int height = 1;
                lines.add(new int[]{x, y, width, height, 0xFFFF0000});
            }
        } else {
            // Clear the lines list when the text is empty
            lines.clear();
        }
    }
}