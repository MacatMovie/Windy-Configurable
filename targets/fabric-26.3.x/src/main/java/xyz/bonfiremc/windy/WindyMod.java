package xyz.bonfiremc.windy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public final class WindyMod implements ModInitializer {
    public static final String MOD_ID = "windy";

    @Override
    public void onInitialize() {
        WindyParticles.register();
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("windy-config.toml");
    }
}
