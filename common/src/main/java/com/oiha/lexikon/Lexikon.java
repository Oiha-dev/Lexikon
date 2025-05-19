package com.oiha.lexikon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Lexikon {
    public static final String MOD_ID = "lexikon";
    public static final Logger LOGGER = LogManager.getLogger();


    public static void init() {
        LOGGER.info("Lexikon is initializing!");
    }
}
