package com.oiha.lexikon;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;

import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.MultiThreadedJLanguageTool;

import java.util.ArrayList;
import java.util.List;

public class Lexikon implements ModInitializer {
    private static final List<String> minecraftNames = new ArrayList<>();
    public static final Logger LOGGER = LogManager.getLogger();
    private static JLanguageTool langTool;
    @Override
    public void onInitialize() {
        for(Item item : Registry.ITEM){
            minecraftNames.add(item.getName().getString());}
        LOGGER.info(minecraftNames.size());
        LOGGER.info("Lexikon mod has been loaded on the server side");
        langTool = new MultiThreadedJLanguageTool(Languages.getLanguageForShortCode("en-GB"));
    }

    public static JLanguageTool getLangTool() {
        return langTool;
    }
}