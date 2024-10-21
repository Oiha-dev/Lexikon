package com.oiha.lexikon;

import com.oiha.lexikon.client.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.MultiThreadedJLanguageTool;

import java.io.*;
import java.util.*;

public class Lexikon implements ModInitializer {
    public static final List<String> minecraftNames = new ArrayList<>();
    public static final List<String> personalDictionary = new ArrayList<>();
    public static final Logger LOGGER = LogManager.getLogger();
    public static JLanguageTool langTool;

    @Override
    public void onInitialize() {
        // Load the configuration
        ModConfig.load();

        // Set the language tool based on the loaded language from the configuration
        String language = "en-US";
        if (ModConfig.currentLanguage != null) {
            int languageIndex = ModConfig.possibleLanguages.indexOf(ModConfig.currentLanguage);
            if (languageIndex != -1) {
                language = ModConfig.ISOLanguages.get(languageIndex);
            } else {
                LOGGER.error("Invalid language in config: " + ModConfig.currentLanguage);
            }
        }
        langTool = new MultiThreadedJLanguageTool(Languages.getLanguageForShortCode(language));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            File file = new File("config/Lexikon/minecraftDictionary.txt");
            if (!file.exists()) {
                minecraftNames.clear();
                for (Item item : Registries.ITEM) {
                    List<String> splitNames = List.of(item.getName().getString().split(" "));
                    minecraftNames.addAll(splitNames);
                }
                for (Block block : Registries.BLOCK) {
                    List<String> splitNames = List.of(block.getName().getString().split(" "));
                    minecraftNames.addAll(splitNames);
                }
                for (EntityType<?> entityType : Registries.ENTITY_TYPE) {
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
            } else {
                minecraftNames.clear();
                minecraftNames.addAll(Arrays.asList(readFileContent("config/Lexikon/minecraftDictionary.txt")));
            }

            File personalDictionaryFile = new File("config/Lexikon/personalDictionary.txt");
            if (!personalDictionaryFile.exists()) {
                createFileIfNotExists("config/Lexikon/personalDictionary.txt");
            } else {
                personalDictionary.clear();
                personalDictionary.addAll(Arrays.asList(readFileContent("config/Lexikon/personalDictionary.txt")));
            }
        });

        LOGGER.info("Lexikon mod has been loaded on the server side");
    }

    public static Boolean createFileIfNotExists(String path) {
        File directory = new File("config/Lexikon");
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                LOGGER.error("Failed to create Lexikon directory");
            }
        }
        File file = new File(path);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    LOGGER.error("Failed to create " + path);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to create " + path, e);
            }
            return true;
        }
        return false;
    }

    public static void updateFile(String path, List<String> Entry) {
        File file = new File(path);
        try (FileWriter writer = new FileWriter(file)) {
            for (String name : Entry) {
                writer.write(name + System.lineSeparator());
            }
            LOGGER.info("minecraftNames.txt has been updated with " + Entry.size() + " names.");
        } catch (IOException e) {
            LOGGER.error("Failed to update minecraftNames.txt", e);
        }
    }

    public static String[] readFileContent(String filePath) {
        List<String> lines = new ArrayList<>();
        File file = new File(filePath);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("File does not exist.");
        }

        return lines.toArray(new String[0]);
    }

    public static JLanguageTool getLangTool() {
        return langTool;
    }
    public static void updateLanguageTool(String language) {
        langTool = new MultiThreadedJLanguageTool(Languages.getLanguageForShortCode(language));
    }
}