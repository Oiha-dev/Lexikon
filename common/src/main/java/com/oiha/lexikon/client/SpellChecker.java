package com.oiha.lexikon.client;

import com.oiha.lexikon.Lexikon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import org.languagetool.JLanguageTool;
import org.languagetool.rules.RuleMatch;

import com.oiha.lexikon.mixin.EditBoxMixin;

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

    public void checkText(EditBox chatField) {
        long currentTickTime = System.currentTimeMillis();
        String text = chatField.getValue();
        // TODO: Make the config
        if ((!text.isEmpty() && (text.charAt(0) != '/' /*|| ModConfig.spellcheckerInCommands*/)) /*&& ModConfig.spellcheckerEnabled*/) {
            if (currentTickTime - lastTickTime >= 1000) {
                executorService.submit(() -> {
                    try {
                        JLanguageTool langTool = Lexikon.getLangTool();
                        List<RuleMatch> matches = langTool.check(text);
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

                // TODO: Make the config
                //int Color = ModConfig.underlineColor.getRGB(); // Red
                int Color = 0xFF0000;


                // Check if the matched text is in the minecraftNames list or personalDictionary list
                // TODO: Make Dictionary
                boolean isMinecraftName = false; //Lexikon.minecraftNames.stream().anyMatch(name -> name.equalsIgnoreCase(text.substring(from, to)));
                boolean isPersonalDictionary = false; //Lexikon.personalDictionary.stream().anyMatch(name -> name.equalsIgnoreCase(text.substring(from, to)));

                if (isMinecraftName && match.getSuggestedReplacements().isEmpty() || isPersonalDictionary) { // If the match is part of a Minecraft name and has no suggestions or is part of the personal dictionary
                    continue; // Skip this match if it's part of a Minecraft name
                } else if (isMinecraftName && !match.getSuggestedReplacements().isEmpty()) { // If the match is part of a Minecraft name and has suggestions
                    // TODO: Make the config
                    //Color = ModConfig.underineMinecraftColor.getRGB(); // Yellow
                    Color = 0xFFFF00; // Yellow
                }

                int scrollOffset = Minecraft.getInstance().font.width(text.substring(0, ((EditBoxMixin) chatField).getFirstCharacterIndex()));
                int x = Minecraft.getInstance().font.width(text.substring(0, from)) + 4 - scrollOffset;
                int y = Minecraft.getInstance().getWindow().getGuiScaledHeight() - 4;
                int width = Minecraft.getInstance().font.width(text.substring(from, to));
                lines.add(new int[]{x, y, width, 1, Color});

                int mouseX = (int) Minecraft.getInstance().mouseHandler.xpos();
                int guiScale = (int) Minecraft.getInstance().getWindow().getGuiScale();
                int textHeight = Minecraft.getInstance().font.lineHeight;

                if ((mouseX >= x * guiScale && mouseX <= (x + width) * guiScale) || match.getFromPos() <= chatField.getCursorPosition() && match.getToPos() >= chatField.getCursorPosition()) {
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