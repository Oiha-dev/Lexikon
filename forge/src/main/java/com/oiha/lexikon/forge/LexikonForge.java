package com.oiha.lexikon.forge;

import com.oiha.lexikon.Lexikon;
import net.minecraftforge.fml.common.Mod;

@Mod(Lexikon.MOD_ID)
public final class LexikonForge {
    public LexikonForge() {
        // Run our common setup.
        Lexikon.init();
    }
}
