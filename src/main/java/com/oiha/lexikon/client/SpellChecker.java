package com.oiha.lexikon.client;

import com.oiha.lexikon.Lexikon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.MultiThreadedJLanguageTool;
import org.languagetool.rules.RuleMatch;

import com.oiha.lexikon.mixin.TextFieldWidgetMixin;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class SpellChecker {
    private ExecutorService executorService;
    private long lastTickTime = System.currentTimeMillis();
    public List<int[]> lines = new ArrayList<>();

    public List<Object[]> suggestionsOverlay = new ArrayList<>();

    ArrayList<RuleMatch> spellcheckList = new ArrayList<>();

    public SpellChecker() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public void checkText(TextFieldWidget chatField) {
        long currentTickTime = System.currentTimeMillis();
        String text = chatField.getText();
        System.out.println("1 - Text: " + text);
        if (!text.isEmpty() && text.charAt(0) != '/') {
            System.out.println("2 - Text is not empty and not a command");
            if (currentTickTime - lastTickTime >= 1000) {
                System.out.println("3 - Enough time has passed since last check");
                executorService.submit(() -> {
                    System.out.println("4 - Inside executor service");
                    try {
                        System.out.println("5 - Before LanguageTool check");
                        List<RuleMatch> matches = Lexikon.getLangTool().check(text);
                        System.out.println("6 - After LanguageTool check");
                        spellcheckList.clear();
                        System.out.println("7 - Matches found: " + matches.size());
                        for (RuleMatch match : matches) {
                            spellcheckList.add(match);
                        }
                    } catch (IOException e) {
                        System.out.println("Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                lastTickTime = currentTickTime;
            }
            lines.clear();
            for (RuleMatch match : spellcheckList) {
                System.out.println("8 - Processing match: " + match.getMessage());
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

                int mouseX = (int) MinecraftClient.getInstance().mouse.getX();
                int mouseY = (int) MinecraftClient.getInstance().mouse.getY();
                int guiScale = (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
                int textHeight = MinecraftClient.getInstance().textRenderer.fontHeight;

                if (mouseX >= x*guiScale && mouseX <= (x + width)*guiScale) {
                    List<String> suggestions = match.getSuggestedReplacements();

                    int boxX = x;
                    int boxY = y;
                    int boxWidth = width;
                    int boxHeight = textHeight * guiScale;
                    suggestionsOverlay.clear();

                    if (!suggestions.isEmpty()) {
                        for (int i = 0; i < Math.min(3, suggestions.size()); i++) {
                            suggestionsOverlay.add(new Object[]{suggestions.get(i), boxX, boxY, boxWidth, boxHeight, match.getFromPos(), match.getToPos(), match.getRule().getDescription()});
                        }
                    } else {
                        suggestionsOverlay.add(new Object[]{"No suggestions", boxX, boxY, boxWidth, boxHeight, match.getFromPos(), match.getToPos(), match.getRule().getDescription()});
                    }
                }
            }
        } else {
            lines.clear();
        }
    }
}