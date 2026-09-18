package xyz.bonfiremc.windy;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import xyz.bonfiremc.windy.particle.WindParticle;

public class WindyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WindyConfig.load();

        ParticleFactoryRegistry.getInstance().register(WindyParticles.WIND, sprites -> {
            WindyParticleSpawner.setWindSprites(sprites);
            return new WindParticle.Factory(sprites);
        });

        ParticleFactoryRegistry.getInstance().register(WindyParticles.STRONG_WIND, sprites -> {
            WindyParticleSpawner.setStrongWindSprites(sprites);
            return new WindParticle.StrongFactory(sprites);
        });
    }
}
