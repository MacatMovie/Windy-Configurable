package xyz.bonfiremc.windy;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class WindyParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, WindyMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> WIND = PARTICLE_TYPES.register("wind", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> STRONG_WIND = PARTICLE_TYPES.register("strong_wind", () -> new SimpleParticleType(false));

    public static void register(IEventBus modBus) {
        PARTICLE_TYPES.register(modBus);
    }
}
