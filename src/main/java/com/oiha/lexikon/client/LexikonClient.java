package com.oiha.lexikon.client;

import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.oiha.lexikon.Lexikon.createFileIfNotExists;
import static com.oiha.lexikon.client.ModConfig.load;
import static com.oiha.lexikon.client.ModConfig.save;

public class LexikonClient implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitializeClient() {
        if (createFileIfNotExists("config/Lexikon/config.json")) {
            save();
        }
        load();

        LOGGER.info("Lexikon mod has been loaded on the client side");
    }
}