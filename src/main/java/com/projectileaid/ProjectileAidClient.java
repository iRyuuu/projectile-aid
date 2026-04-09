package com.projectileaid;

import com.projectileaid.config.ModConfig;
import com.projectileaid.render.TrajectoryRenderer;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectileAidClient implements ClientModInitializer {

    public static final String MOD_ID = "projectile-aid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        TrajectoryRenderer.register();
        LOGGER.info("Projectile Aid initialised.");
    }
}
