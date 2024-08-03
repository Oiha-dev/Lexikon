package com.oiha.lexikon.client;

import com.oiha.lexikon.Lexikon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.languagetool.rules.RuleMatch;

import com.oiha.lexikon.mixin.TextFieldWidgetMixin;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpellChecker {
    private ExecutorService executorService;
    private long lastTickTime = System.currentTimeMillis();
    public List<int[]> lines = new ArrayList<>();

    public List<Object[]> suggestionsOverlay = new ArrayList<>();

    CopyOnWriteArrayList<RuleMatch> spellcheckList = new CopyOnWriteArrayList<>();

    public SpellChecker() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public void checkText(TextFieldWidget chatField) {
        long currentTickTime = System.currentTimeMillis();
        String text = chatField.getText();
        if (!text.isEmpty() && text.charAt(0) != '/') {
            if (currentTickTime - lastTickTime >= 1000) {
                executorService.submit(() -> {
                    try {
                        List<RuleMatch> matches = Lexikon.getLangTool().check(text);
                        spellcheckList.clear();
                        spellcheckList.addAll(matches);
                    } catch (IOException e) {
                        System.out.println("Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                lastTickTime = currentTickTime;
            }
            lines.clear();
            for (RuleMatch match : spellcheckList) {
                int from = match.getFromPos();
                int to = match.getToPos();

                if (from < 0 || to > text.length() || from > to) {
                    continue;
                }

                int Color = 0xFFFF0000; // Red

                // Check if the matched text is in the minecraftNames list or personalDictionary list
                boolean isMinecraftName = Lexikon.minecraftNames.stream().anyMatch(name -> name.equalsIgnoreCase(text.substring(from, to)));
                boolean isPersonalDictionary = Lexikon.personalDictionary.stream().anyMatch(name -> name.equalsIgnoreCase(text.substring(from, to)));

                if (isMinecraftName && match.getSuggestedReplacements().isEmpty() || isPersonalDictionary) { // If the match is part of a Minecraft name and has no suggestions or is part of the personal dictionary
                    continue; // Skip this match if it's part of a Minecraft name
                } else if (isMinecraftName && !match.getSuggestedReplacements().isEmpty()) { // If the match is part of a Minecraft name and has suggestions
                    Color = 0xFFFFAA00; // Yellow
                }

                int scrollOffset = MinecraftClient.getInstance().textRenderer.getWidth(text.substring(0, ((TextFieldWidgetMixin) chatField).getFirstCharacterIndex()));
                int x = MinecraftClient.getInstance().textRenderer.getWidth(text.substring(0, from)) + 4 - scrollOffset;
                int y = MinecraftClient.getInstance().getWindow().getScaledHeight() - 4;
                int width = MinecraftClient.getInstance().textRenderer.getWidth(text.substring(from, to));
                lines.add(new int[]{x, y, width, 1, Color});

                int mouseX = (int) MinecraftClient.getInstance().mouse.getX();
                int mouseY = (int) MinecraftClient.getInstance().mouse.getY();
                int guiScale = (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
                int textHeight = MinecraftClient.getInstance().textRenderer.fontHeight;

                if (mouseX >= x * guiScale && mouseX <= (x + width) * guiScale) {
                    List<String> suggestions = match.getSuggestedReplacements();

                    int boxHeight = textHeight * guiScale;
                    suggestionsOverlay.clear();

                    if (!suggestions.isEmpty()) {
                        for (int i = 0; i < Math.min(3, suggestions.size()); i++) {
                            suggestionsOverlay.add(new Object[]{suggestions.get(i), x, y, width, boxHeight, match.getFromPos(), match.getToPos(), match.getRule().getDescription()});
                        }
                    } else {
                        suggestionsOverlay.add(new Object[]{"No suggestions", x, y, width, boxHeight, match.getFromPos(), match.getToPos(), match.getRule().getDescription()});
                    }
                }
            }
        } else {
            lines.clear();
        }
    }
}