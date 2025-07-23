package com.oiha.lexikon.neoforge;

import com.oiha.lexikon.Lexikon;
import net.neoforged.fml.common.Mod;

@Mod(Lexikon.MOD_ID)
public final class LexikonNeoForge {
    public LexikonNeoForge() {
        // Run our common setup.
        Lexikon.init();
    }
}
