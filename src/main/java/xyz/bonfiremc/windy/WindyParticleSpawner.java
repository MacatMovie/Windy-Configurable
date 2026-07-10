package xyz.bonfiremc.windy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import xyz.bonfiremc.windy.particle.WindParticle;

@OnlyIn(Dist.CLIENT)
public final class WindyParticleSpawner {
    private static final int PARTICLE_ATTEMPTS_PER_TICK = 667;
    private static final int PARTICLE_RANGE = 32;

    private static SpriteSet windSprites;
    private static SpriteSet strongWindSprites;

    private WindyParticleSpawner() {}

    public static void setWindSprites(SpriteSet sprites) {
        windSprites = sprites;
    }

    public static void setStrongWindSprites(SpriteSet sprites) {
        strongWindSprites = sprites;
    }

    public static boolean canSpawn(boolean strong) {
        return strong ? strongWindSprites != null : windSprites != null;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel world = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (world == null || player == null || world.dimension() != Level.OVERWORLD) {
            return;
        }

        WindyConfig config = WindyConfig.get();
        if (!config.spawnWind) {
            return;
        }

        boolean useStrongWind = world.isRaining() || world.isThundering();
        if (!canSpawn(useStrongWind)) {
            return;
        }

        RandomSource random = world.getRandom();
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        int playerX = Mth.floor(player.getX());
        int playerY = Mth.floor(player.getY());
        int playerZ = Mth.floor(player.getZ());

        for (int i = 0; i < PARTICLE_ATTEMPTS_PER_TICK; i++) {
            int x = playerX + random.nextInt(PARTICLE_RANGE) - random.nextInt(PARTICLE_RANGE);
            int y = playerY + random.nextInt(PARTICLE_RANGE) - random.nextInt(PARTICLE_RANGE);
            int z = playerZ + random.nextInt(PARTICLE_RANGE) - random.nextInt(PARTICLE_RANGE);
            blockPos.set(x, y, z);

            if (blockPos.getY() < config.getActiveMinimumWindHeight()) {
                continue;
            }
            if (config.windMustSeeSky && !world.canSeeSky(blockPos)) {
                continue;
            }

            double heightMultiplier = config.getHeightSpawnRateMultiplier(blockPos.getY());
            if (heightMultiplier <= 0.0D) {
                continue;
            }

            double biomeMultiplier = 1.0D;
            if (config.biomeWind != null && config.biomeWind.enabled) {
                biomeMultiplier = config.getBiomeSpawnRateMultiplier(world.getBiome(blockPos));
                if (biomeMultiplier <= 0.0D) {
                    continue;
                }
            }

            double baseChancePercent = world.isThundering() ? 0.020D : 0.015D;
            double finalChancePercent = baseChancePercent * heightMultiplier * biomeMultiplier;
            if (random.nextDouble() * 100.0D <= finalChancePercent) {
                spawn(
                        world,
                        blockPos.getX() + random.nextDouble(),
                        blockPos.getY() + random.nextDouble(),
                        blockPos.getZ() + random.nextDouble(),
                        useStrongWind,
                        random
                );
            }
        }
    }

    public static void spawn(ClientLevel world, double x, double y, double z, boolean strong, RandomSource random) {
        SpriteSet sprites = strong ? strongWindSprites : windSprites;
        if (sprites == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.particleEngine == null) {
            return;
        }

        minecraft.particleEngine.add(WindParticle.createFromAnchor(world, x, y, z, sprites, strong, random));
    }
}
