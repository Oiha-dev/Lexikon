package com.oiha.lexikon.neoforge.client;

import com.oiha.lexikon.Lexikon;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@Mod("lexikon")
public class LexikonNeoForgeClient {

    @SubscribeEvent
    public static void onClientJoinServer(ClientPlayerNetworkEvent.LoggingIn event) {
        Lexikon.initLexikon();
    }
}