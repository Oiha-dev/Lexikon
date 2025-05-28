package com.oiha.lexikon.forge.client;

import com.oiha.lexikon.Lexikon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "lexikon")
public class LexikonForgeClient {

    @SubscribeEvent
    public static void onClientJoinServer(ClientPlayerNetworkEvent.LoggingIn event) {
        Lexikon.initLexikon();
    }
}