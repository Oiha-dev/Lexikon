package com.oiha.lexikon;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.MultiThreadedJLanguageTool;

public class Lexikon implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    private static JLanguageTool langTool;

    @Override
    public void onInitialize() {
        LOGGER.info("Lexikon mod has been loaded on the server side");
        langTool = new MultiThreadedJLanguageTool(Languages.getLanguageForShortCode("en-GB"));
    }

    public static JLanguageTool getLangTool() {
        return langTool;
    }
}