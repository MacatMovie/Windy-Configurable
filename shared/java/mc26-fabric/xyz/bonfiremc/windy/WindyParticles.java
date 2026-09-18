package xyz.bonfiremc.windy;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class WindyParticles {
    public static final SimpleParticleType WIND = FabricParticleTypes.simple(false);
    public static final SimpleParticleType STRONG_WIND = FabricParticleTypes.simple(false);

    private WindyParticles() {}

    public static void register() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, WindyMod.asResource("wind"), WIND);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, WindyMod.asResource("strong_wind"), STRONG_WIND);
    }
}
