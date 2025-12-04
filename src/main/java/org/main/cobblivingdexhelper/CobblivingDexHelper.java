package org.main.cobblivingdexhelper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobblivingDexHelper implements ModInitializer {

    // Aggiornato ID Logger
    public static final Logger LOGGER = LoggerFactory.getLogger("cobblivingdexhelper");

    @Override
    public void onInitialize() {

        LOGGER.info("CobblivingDexHelper si sta avviando!");

    }


}