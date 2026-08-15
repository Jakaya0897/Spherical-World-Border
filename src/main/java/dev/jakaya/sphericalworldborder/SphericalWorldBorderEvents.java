package dev.jakaya.sphericalworldborder;

import java.util.Set;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class SphericalWorldBorderEvents {
    private static final int MAX_POLE_REFLECTIONS_PER_TICK = 16;

    private SphericalWorldBorderEvents() {}

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!((Boolean) SphericalWorldBorderConfig.ENABLED.get())) {
            return;
        }
        if (!entity.isAlive() || entity.isRemoved() || entity.isPassenger()) {
            return;
        }
        if (((Boolean) SphericalWorldBorderConfig.OVERWORLD_ONLY.get()) && serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!((Boolean) SphericalWorldBorderConfig.TELEPORT_NON_PLAYERS.get()) && !isPlayerClass(entity)) {
            return;
        }
        if (((Boolean) SphericalWorldBorderConfig.EXCLUDE_CREATE_ENTITIES.get()) && isCreateEntity(entity)) {
            return;
        }

        int circumference = (Integer) SphericalWorldBorderConfig.CIRCUMFERENCE.get();
        if (circumference < 64) {
            return;
        }

        double half = circumference / 2.0D;
        double quarter = circumference / 4.0D;
        double west = -half;
        double east = half;
        double north = -quarter;
        double south = quarter;

        double x = entity.getX();
        double z = entity.getZ();
        boolean changed = false;
        boolean poleReflectedOddTimes = false;

        // Spherical pole transform:
        //   z reflects back into the same hemisphere,
        //   x shifts 180 degrees around the planet for each pole crossing.
        if ((Boolean) SphericalWorldBorderConfig.CROSS_POLES.get()) {
            int reflections = 0;
            while ((z < north || z > south) && reflections < MAX_POLE_REFLECTIONS_PER_TICK) {
                if (z < north) {
                    z = 2.0D * north - z;
                    x += half;
                } else if (z > south) {
                    z = 2.0D * south - z;
                    x += half;
                }
                poleReflectedOddTimes = !poleReflectedOddTimes;
                changed = true;
                reflections++;
            }

            // Extremely large one-tick displacement fallback. This is not expected in normal play,
            // but avoids leaving an entity outside the playable latitude range.
            if (z < north || z > south) {
                double latitudeSpan = south - north; // circumference / 2
                double period = latitudeSpan * 2.0D;
                double t = positiveModulo(z - north, period);
                boolean reflected = t > latitudeSpan;
                z = reflected ? south - (t - latitudeSpan) : north + t;
                if (reflected) {
                    x += half;
                    poleReflectedOddTimes = !poleReflectedOddTimes;
                }
                changed = true;
            }
        }

        if ((Boolean) SphericalWorldBorderConfig.WRAP_LONGITUDE.get() && (x < west || x > east)) {
            x = wrapCoordinate(x, west, east);
            changed = true;
        }

        if (!changed) {
            return;
        }

        float yaw = entity.getYRot();
        Vec3 oldMotion = entity.getDeltaMovement();

        if (poleReflectedOddTimes) {
            // Reflection across a pole keeps east/west headings but swaps north/south.
            // Minecraft yaw: south=0, west=90, north=180, east=-90.
            yaw = wrapDegrees(180.0F - yaw);
        }

        boolean teleported = entity.teleportTo(
                serverLevel,
                x,
                entity.getY(),
                z,
                Set.of(),
                yaw,
                entity.getXRot()
        );

        if (teleported && poleReflectedOddTimes) {
            // Match the coordinate reflection: X velocity remains, Z velocity reverses.
            entity.setDeltaMovement(oldMotion.x, oldMotion.y, -oldMotion.z);
        }
    }

    private static boolean isPlayerClass(Entity entity) {
        // Avoids a hard compile dependency on ServerPlayer in this tiny build and still handles subclasses.
        Class<?> type = entity.getClass();
        while (type != null) {
            if ("net.minecraft.server.level.ServerPlayer".equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean isCreateEntity(Entity entity) {
        String name = entity.getClass().getName();
        return name.startsWith("com.simibubi.create.content.trains.")
                || name.startsWith("com.simibubi.create.content.contraptions.");
    }

    private static double wrapCoordinate(double value, double min, double max) {
        if (value >= min && value <= max) {
            return value;
        }
        double width = max - min;
        return min + positiveModulo(value - min, width);
    }

    private static double positiveModulo(double value, double modulus) {
        double result = value % modulus;
        return result < 0.0D ? result + modulus : result;
    }

    private static float wrapDegrees(float degrees) {
        float result = degrees % 360.0F;
        if (result >= 180.0F) {
            result -= 360.0F;
        }
        if (result < -180.0F) {
            result += 360.0F;
        }
        return result;
    }
}
