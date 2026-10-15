package xyz.bonfiremc.windy;

import java.lang.reflect.Method;
import xyz.bonfiremc.windy.particle.WindParticle;

/** Optional, reflection-only bridge so Windy keeps no hard dependency on Living Wind. */
public final class LivingWindCompat {
    private static boolean attempted;
    private static boolean available;
    private static Method hasWindState;
    private static Method localStrength;
    private static Method directionDegrees;

    private LivingWindCompat() {}

    private static void resolve() {
        if (attempted) return;
        attempted = true;

        // Resolve the client wind API independently. Failure of the optional particle-immunity
        // registration must never disable the rest of the Living Wind integration.
        try {
            Class<?> api = Class.forName("com.macatmovie.livingwind.api.ClientWindApi");
            hasWindState = api.getMethod("hasWindState");
            localStrength = api.getMethod("localStrength");
            directionDegrees = api.getMethod("directionDegrees");
            available = true;
        } catch (Throwable ignored) {
            available = false;
            return;
        }

        try {
            Class<?> particleApi = Class.forName("com.macatmovie.livingwind.api.ParticleWindApi");
            particleApi.getMethod("registerImmuneParticleClass", Class.class).invoke(null, WindParticle.class);
        } catch (Throwable ignored) {
            // Older/newer Living Wind builds may expose a different particle API. The visual
            // wind bridge still works, so do not mark the whole integration unavailable.
        }
    }

    /** True when a compatible Living Wind client API is installed, regardless of Windy's toggle. */
    public static boolean isInstalled() {
        resolve();
        return available;
    }

    /** True when Living Wind currently has a synchronized direction we can use for rendering. */
    public static boolean hasWindState() {
        resolve();
        if (!available) return false;
        try {
            return Boolean.TRUE.equals(hasWindState.invoke(null));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isActive() {
        // Resolve even when the visual integration toggle is off so Windy can register its
        // custom particle class as self-managed and avoid Living Wind's generic particle push.
        return WindyConfig.get().livingWind.enabled && hasWindState();
    }

    public static float strength() {
        if (!isActive()) return 0.0f;
        try { return ((Number) localStrength.invoke(null)).floatValue(); }
        catch (Throwable ignored) { return 0.0f; }
    }

    public static float directionDegrees() {
        if (!isActive()) return 0.0f;
        try { return ((Number) directionDegrees.invoke(null)).floatValue(); }
        catch (Throwable ignored) { return 0.0f; }
    }

    public static double spawnRateMultiplier(float strength) {
        WindyConfig.LivingWindIntegration cfg = WindyConfig.get().livingWind;
        if (strength < cfg.minimumStrength) return 0.0D;
        // Around strength 0.5 the density matches classic Windy. Stronger air naturally produces more streaks.
        return Math.max(0.0D, Math.min(4.0D, (strength / 0.5D) * cfg.densityMultiplier));
    }

    public static boolean useStrongParticle(float strength) {
        return strength >= WindyConfig.get().livingWind.strongParticleThreshold;
    }

    /** Fixed visual travel speed: Living Wind chooses direction/density, not Windy's streak speed. */
    public static double velocityX(boolean strong) {
        double radians = Math.toRadians(directionDegrees());
        double speed = strong ? 0.10D : 0.055D;
        return -Math.sin(radians) * speed;
    }

    public static double velocityZ(boolean strong) {
        double radians = Math.toRadians(directionDegrees());
        double speed = strong ? 0.10D : 0.055D;
        return Math.cos(radians) * speed;
    }
}
