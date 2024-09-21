package com.oiha.lexikon.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;

import java.awt.Color;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

import static com.oiha.lexikon.Lexikon.*;

public class ModConfig {
    private static final String CONFIG_FILE = "config/Lexikon/config.json";
    private static final int CONFIG_VERSION = 1;

    // Esthetic Options
    public static Color suggestionBackgroundColor = new Color(-536870912, true);
    public static Color suggestionColor = Color.WHITE;
    public static Color chosenSuggestionColor = Color.YELLOW;
    public static Color dictionaryIconColor = Color.WHITE;
    public static String iconStyle = "book";
    public static boolean outlineEnabled = true;
    public static Color outlineColor = Color.WHITE;
    public static Color underlineColor = Color.RED;
    public static Color underineMinecraftColor = new Color(0xFFAA00);
    public static String underlineStyle = "Straight";

    // Technical Options

    public static String previousLanguage = "English (GB)";
    public static String currentLanguage = "English (GB)";

    public static boolean spellcheckerEnabled = true;
    public static boolean spellcheckerInCommands = false;
    public static boolean flagButtonEnabled = false;
    public static boolean dictionaryEnabled = true;

    public static final List<String> minecraftNames = new ArrayList<>();

    public static final List<String> ISOLanguages = new ArrayList<>(){{
        add("en-GB");
        add("en-US");
        add("fr-FR");
        add("es-ES");
        add("de-DE");
        add("it-IT");
        add("nl-NL");
        add("pt-PT");
        add("ca-ES");
    }};
    public static final List<String> possibleLanguages = new ArrayList<>(){{
        add("English (GB)");
        add("English (US)");
        add("French");
        add("Spanish");
        add("German");
        add("Italian");
        add("Dutch");
        add("Portuguese");
        add("Catalan");
    }};


    public static void save() {
        Map<String, Object> config = new HashMap<>();
        config.put("version", CONFIG_VERSION);
        config.put("suggestionBackgroundColor", suggestionBackgroundColor.getRGB());
        config.put("suggestionColor", suggestionColor.getRGB());
        config.put("chosenSuggestionColor", chosenSuggestionColor.getRGB());
        config.put("dictionaryIconColor", dictionaryIconColor.getRGB());
        config.put("iconStyle", iconStyle);
        config.put("outlineEnabled", outlineEnabled);
        config.put("outlineColor", outlineColor.getRGB());
        config.put("flaggedWordColor", underlineColor.getRGB());
        config.put("flaggedMinecraftWordColor", underineMinecraftColor.getRGB());
        config.put("spellcheckerEnabled", spellcheckerEnabled);
        config.put("spellcheckerInCommands", spellcheckerInCommands);
        config.put("flagButtonEnabled", flagButtonEnabled);
        config.put("dictionaryEnabled", dictionaryEnabled);
        config.put("underlineStyle", underlineStyle);
        config.put("language", currentLanguage);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(config);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (previousLanguage != currentLanguage) {
            updateLanguageTool(ISOLanguages.get(possibleLanguages.indexOf(currentLanguage)));
            previousLanguage = currentLanguage;
        }

    }

    public static void load() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            save(); // Create default config file
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> config = gson.fromJson(reader, type);

            suggestionBackgroundColor = new Color(((Double) config.get("suggestionBackgroundColor")).intValue(), true);
            suggestionColor = new Color(((Double) config.get("suggestionColor")).intValue());
            chosenSuggestionColor = new Color(((Double) config.get("chosenSuggestionColor")).intValue());
            dictionaryIconColor = new Color(((Double) config.get("dictionaryIconColor")).intValue());
            iconStyle = (String) config.get("iconStyle");
            outlineEnabled = (Boolean) config.get("outlineEnabled");
            outlineColor = new Color(((Double) config.get("outlineColor")).intValue());
            underlineColor = new Color(((Double) config.get("flaggedWordColor")).intValue());
            underineMinecraftColor = new Color(((Double) config.get("flaggedMinecraftWordColor")).intValue());
            spellcheckerEnabled = (Boolean) config.get("spellcheckerEnabled");
            spellcheckerInCommands = (Boolean) config.get("spellcheckerInCommands");
            flagButtonEnabled = (Boolean) config.get("flagButtonEnabled");
            dictionaryEnabled = (Boolean) config.get("dictionaryEnabled");
            underlineStyle = (String) config.get("underlineStyle");
            currentLanguage = (String) config.get("language");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reloadDictionary() {
                /*
                This code will run when a player joins a server/single player to be
                sure that all mods are loaded to get their blocks/items
                */
            minecraftNames.clear();
            for (Item item : Registry.ITEM) {
                List<String> splitNames = List.of(item.getName().getString().split(" "));
                minecraftNames.addAll(splitNames);
            }
            for (Block block : Registry.BLOCK) {
                List<String> splitNames = List.of(block.getName().getString().split(" "));
                minecraftNames.addAll(splitNames);
            }
            for (Enchantment enchantment : Registry.ENCHANTMENT) {
                String strEnchantment = enchantment.getName(0).getString();
                List<String> splitNames = List.of(strEnchantment.substring(0, strEnchantment.length() - 20).split(" "));
                minecraftNames.addAll(splitNames);
            }
            for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
                List<String> splitNames = List.of(entityType.getName().getString().split(" "));
                minecraftNames.addAll(splitNames);
            }

            Set<String> uniqueNames = new HashSet<>(minecraftNames);
            minecraftNames.clear();
            minecraftNames.addAll(uniqueNames);

            Iterator<String> iterator = minecraftNames.iterator();
            while (iterator.hasNext()) {
                String name = iterator.next();
                try {
                    if (langTool.check(name).isEmpty()) {
                        iterator.remove();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            createFileIfNotExists("config/Lexikon/minecraftDictionary.txt");
            updateFile("config/Lexikon/minecraftDictionary.txt", minecraftNames);
    }
}