package com.oiha.lexikon.fabric;

import com.oiha.lexikon.Lexikon;
import net.fabricmc.api.ModInitializer;

public final class LexikonFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Lexikon.init();
    }
}
