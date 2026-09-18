package xyz.bonfiremc.windy;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.particle.SpriteSet;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import xyz.bonfiremc.windy.particle.WindParticle;

import java.nio.file.Path;

@Mod(value = WindyMod.MOD_ID, dist = Dist.CLIENT)
public class WindyMod {
    public static final String MOD_ID = "windy";

    public WindyMod(IEventBus modBus, ModContainer modContainer) {
        WindyConfig.load();
        WindyParticles.register(modBus);

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, parent) -> ConfigScreen.create(parent));
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::registerParticleProviders);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve("windy-config.toml");
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // Kept as a client setup hook in case future client-only registrations are added.
    }

    private void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(WindyParticles.WIND.get(), this::registerWindProvider);
        event.registerSpriteSet(WindyParticles.STRONG_WIND.get(), this::registerStrongWindProvider);
    }

    private WindParticle.Factory registerWindProvider(SpriteSet sprites) {
        WindyParticleSpawner.setWindSprites(sprites);
        return new WindParticle.Factory(sprites);
    }

    private WindParticle.StrongFactory registerStrongWindProvider(SpriteSet sprites) {
        WindyParticleSpawner.setStrongWindSprites(sprites);
        return new WindParticle.StrongFactory(sprites);
    }
}
