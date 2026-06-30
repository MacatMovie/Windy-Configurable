package xyz.bonfiremc.windy.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bonfiremc.windy.WindyConfig;
import xyz.bonfiremc.windy.WindyParticleSpawner;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "doAnimateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
    private void windy$spawnWind(int posX, int posY, int posZ, int range, RandomSource random, Block block, BlockPos.MutableBlockPos blockPos, CallbackInfo ci) {
        ClientLevel world = (ClientLevel) (Object) this;

        if (world.dimension() != Level.OVERWORLD) {
            return;
        }

        WindyConfig config = WindyConfig.get();
        if (!config.spawnWind || blockPos.getY() < config.getActiveMinimumWindHeight()) {
            return;
        }
        if (config.windMustSeeSky && !world.canSeeSky(blockPos)) {
            return;
        }

        double heightMultiplier = config.getHeightSpawnRateMultiplier(blockPos.getY());
        if (heightMultiplier <= 0.0D) {
            return;
        }

        double biomeMultiplier = 1.0D;
        if (config.biomeWind != null && config.biomeWind.enabled) {
            biomeMultiplier = config.getBiomeSpawnRateMultiplier(world.getBiome(blockPos));
            if (biomeMultiplier <= 0.0D) {
                return;
            }
        }

        double baseChancePercent = world.isThundering() ? 0.020D : 0.015D;
        double finalChancePercent = baseChancePercent * heightMultiplier * biomeMultiplier;
        if (random.nextDouble() * 100.0D <= finalChancePercent) {
            boolean useStrongWind = world.isRaining() || world.isThundering();
            if (!WindyParticleSpawner.canSpawn(useStrongWind)) {
                return;
            }

            WindyParticleSpawner.spawn(
                    world,
                    blockPos.getX() + random.nextDouble(),
                    blockPos.getY() + random.nextDouble(),
                    blockPos.getZ() + random.nextDouble(),
                    useStrongWind
            );
        }
    }
}
