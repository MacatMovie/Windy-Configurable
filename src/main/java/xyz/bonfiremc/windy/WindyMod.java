package xyz.bonfiremc.windy;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraft.client.particle.SpriteSet;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import xyz.bonfiremc.windy.particle.WindParticle;

import java.nio.file.Path;

@Mod(WindyMod.MOD_ID)
public class WindyMod {
    public static final String MOD_ID = "windy";

    public WindyMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        WindyConfig.load();
        WindyParticles.register(modBus);

        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::registerParticleProviders);
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve("windy-config.toml");
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ConfigScreen.create(parent))
        ));
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
