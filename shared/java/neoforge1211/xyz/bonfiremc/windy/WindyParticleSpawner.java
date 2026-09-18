package xyz.bonfiremc.windy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import xyz.bonfiremc.windy.particle.WindParticle;

@OnlyIn(Dist.CLIENT)
public final class WindyParticleSpawner {
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

    public static void spawn(ClientLevel world, double x, double y, double z, boolean strong) {
        SpriteSet sprites = strong ? strongWindSprites : windSprites;
        if (sprites == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.particleEngine == null) {
            return;
        }

        minecraft.particleEngine.add(WindParticle.createFromAnchor(world, x, y, z, sprites, strong));
    }
}
