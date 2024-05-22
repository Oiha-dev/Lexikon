package com.oiha.lexikon.client;

import com.oiha.lexikon.Lexikon;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpellChecker {
    private ExecutorService executorService;

    public SpellChecker() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public SpellChecker(String text) {
        this();
        checkText(text);
    }

    public void checkText(String text) {
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
    }
}