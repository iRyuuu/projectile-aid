package com.projectileaid;

import com.projectileaid.screen.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registers the Projectile Aid config screen with Fabric Mod Menu.
 * This class is only instantiated when ModMenu is present at runtime.
 */
public class ProjectileAidModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
