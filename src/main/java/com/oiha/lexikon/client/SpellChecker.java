package com.oiha.lexikon.client;

import com.oiha.lexikon.Lexikon;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpellChecker {
    private ExecutorService executorService;
    private long lastTickTime = System.currentTimeMillis();

    public SpellChecker() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public void checkText(String text) {
        /*
            * This method is called every tick but the first part of the code is limited to run once per second
            * This is to prevent the spell checker from running too frequently and causing fps drops
            * I'm using the LanguageTool library to check the text for potential errors and the suggested corrections
         */
        long currentTickTime = System.currentTimeMillis();
        if (currentTickTime - lastTickTime >= 1000) {
            executorService.submit(() -> {
                try {
                    List<RuleMatch> matches = Lexikon.getLangTool().check(text);
                    for (RuleMatch match : matches) {
                        System.out.println("Potential error at characters " +
                                match.getFromPos() + "-" + match.getToPos() + ": " +
                                match.getMessage());
                        System.out.println("Suggested correction(s): " +
                                match.getSuggestedReplacements());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            lastTickTime = currentTickTime;
            /*
                * This part run every tick and is used to draw the red underline under the misspelled words
                * It also displays the suggested corrections when the player hovers over the underlined word
             */
            
        }

    }
}