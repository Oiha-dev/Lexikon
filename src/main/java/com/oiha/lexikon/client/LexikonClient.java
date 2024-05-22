package com.oiha.lexikon.client;

import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LexikonClient implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Lexikon mod has been loaded on the client side");
    }
}