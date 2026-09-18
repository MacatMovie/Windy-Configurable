package xyz.bonfiremc.windy;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import xyz.bonfiremc.windy.particle.WindParticle;

public final class WindyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WindyConfig.load();

        ParticleProviderRegistry.getInstance().register(WindyParticles.WIND, sprites -> {
            WindyParticleSpawner.setWindSprites(sprites);
            return new WindParticle.Factory(sprites);
        });

        ParticleProviderRegistry.getInstance().register(WindyParticles.STRONG_WIND, sprites -> {
            WindyParticleSpawner.setStrongWindSprites(sprites);
            return new WindParticle.StrongFactory(sprites);
        });

        ClientTickEvents.END_CLIENT_TICK.register(WindyParticleSpawner::onClientTick);
    }
}
