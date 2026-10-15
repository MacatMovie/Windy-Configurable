package xyz.bonfiremc.windy.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import xyz.bonfiremc.windy.LivingWindCompat;

public class WindParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    /**
     * Render Windy as a world-space vertical ribbon whenever Living Wind is installed.
     * The sprite animation itself runs left -> right, so local +X (the sprite's right edge)
     * is aligned to Living Wind's horizontal travel direction. This deliberately bypasses
     * Minecraft's camera-facing billboard rotation instead of merely changing its mode.
     */
    private static Quaternionf livingWindRotation() {
        double windX = LivingWindCompat.velocityX(false);
        double windZ = LivingWindCompat.velocityZ(false);
        double length = Math.sqrt(windX * windX + windZ * windZ);

        if (length < 1.0E-8D) {
            // A fixed world orientation is preferable to ever falling back to a billboard.
            return new Quaternionf();
        }

        windX /= length;
        windZ /= length;
        // JOML Y rotation maps local +X to (cos(yaw), 0, -sin(yaw)).
        float yaw = (float) Math.atan2(-windZ, windX);
        return new Quaternionf().rotateY(yaw);
    }

    protected WindParticle(ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites, int lifetime) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.hasPhysics = true;
        this.lifetime = lifetime;
        this.scale(50.5F);
        this.setAlpha(0.5F);
        this.setPos(x, y, z);
        this.setSpriteFromAge(sprites);
        this.sprites = sprites;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (LivingWindCompat.isInstalled()) {
            // Do not call super.render here: that path starts from a camera-facing billboard.
            this.renderRotatedQuad(buffer, camera, livingWindRotation(), partialTick);
            return;
        }
        super.render(buffer, camera, partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Normal extends WindParticle {
        private static final int LIFETIME = 50;

        public Normal(ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
            super(world, x, y, z, velocityX, velocityY, velocityZ, sprites, LIFETIME);
        }
    }

    public static class Strong extends WindParticle {
        private static final int LIFETIME = 25;

        public Strong(ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
            super(world, x, y, z, velocityX, velocityY, velocityZ, sprites, LIFETIME);
        }
    }

    public record Factory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return createShiftedParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.sprites, false);
        }
    }

    public record StrongFactory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return createShiftedParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.sprites, true);
        }
    }

    public static Particle createFromAnchor(ClientLevel world, double x, double y, double z, SpriteSet sprites, boolean strong) {
        return createShiftedParticle(world, x, y, z, 0.0D, 0.0D, 0.0D, sprites, strong);
    }

    private static Particle createShiftedParticle(ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites, boolean strong) {
        RandomSource random = world.random;
        int distance = random.nextInt(30) + 40;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double newY = y + random.nextInt(15) + random.nextInt(15);
        double shiftedX = (Math.cos(angle) * distance) + x;
        double shiftedZ = (Math.sin(angle) * distance) + z;
        double resolvedVelocityX = velocityX;
        double resolvedVelocityZ = velocityZ;
        if (LivingWindCompat.isActive()) {
            resolvedVelocityX = LivingWindCompat.velocityX(strong);
            resolvedVelocityZ = LivingWindCompat.velocityZ(strong);
        }
        return strong
                ? new Strong(world, shiftedX, newY, shiftedZ, resolvedVelocityX, velocityY, resolvedVelocityZ, sprites)
                : new Normal(world, shiftedX, newY, shiftedZ, resolvedVelocityX, velocityY, resolvedVelocityZ, sprites);
    }
}
