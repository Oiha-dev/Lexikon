package com.oiha.lexikon;

import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpellChecker {
    private static final SpellChecker INSTANCE = new SpellChecker();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final JLanguageTool langTool;
    private volatile String latestMessage = null;
    private volatile boolean running = false;

    private SpellChecker() {
        langTool = new JLanguageTool(Languages.getLanguageForShortCode("en-GB"));
    }

    public static SpellChecker getInstance() {
        return INSTANCE;
    }

    public void check(String message) {
        latestMessage = message;
        if (!running) {
            running = true;
            executor.submit(this::process);
        }
    }

    private void process() {
        String toCheck;
        while ((toCheck = latestMessage) != null) {
            latestMessage = null;
            try {
                List<RuleMatch> matches = langTool.check(toCheck);
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
        }
        running = false;
    }
}