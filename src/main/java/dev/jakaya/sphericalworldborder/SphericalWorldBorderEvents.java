package dev.jakaya.sphericalworldborder;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class SphericalWorldBorderEvents {
    private static final int MAX_POLE_REFLECTIONS_PER_TICK = 16;
    private static final double BOUNDARY_EXIT_CLEARANCE = 2.0D;
    private static final double FALLBACK_INSET = 2.0D;

    // A surface fallback deliberately starts a little above the heightmap result.
    // This is safer around snow layers, uneven terrain, overhangs and modded terrain.
    private static final int SURFACE_CLEARANCE = 1;

    // A swimmer should remain in water if practical rather than being placed inside
    // the mirrored column (which may be an iceberg, cliff or cave wall).
    private static final int WATER_HORIZONTAL_SEARCH_RADIUS = 8;
    private static final int WATER_VERTICAL_SEARCH_RADIUS = 8;
    private static final int SURFACE_HORIZONTAL_SEARCH_RADIUS = 6;

    private SphericalWorldBorderEvents() {}

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;

        if (!SphericalWorldBorderConfig.ENABLED.get()) return;
        if (!entity.isAlive() || entity.isRemoved() || entity.isPassenger()) return;
        if (SphericalWorldBorderConfig.OVERWORLD_ONLY.get() && level.dimension() != Level.OVERWORLD) return;
        if (!SphericalWorldBorderConfig.TELEPORT_NON_PLAYERS.get() && !isPlayerClass(entity)) return;
        if (SphericalWorldBorderConfig.EXCLUDE_CREATE_ENTITIES.get() && isCreateEntity(entity)) return;

        int circumference = SphericalWorldBorderConfig.CIRCUMFERENCE.get();
        if (circumference < 64) return;

        double half = circumference / 2.0D;
        double quarter = circumference / 4.0D;
        double west = -half;
        double east = half;
        double north = -quarter;
        double south = quarter;

        double oldX = entity.getX();
        double oldY = entity.getY();
        double oldZ = entity.getZ();
        double newX = oldX;
        double newZ = oldZ;
        boolean crossed = false;
        boolean reflectNorthSouth = false;
        Crossing crossing = Crossing.NONE;

        if (SphericalWorldBorderConfig.CROSS_POLES.get()) {
            int reflections = 0;
            while ((newZ < north || newZ > south) && reflections < MAX_POLE_REFLECTIONS_PER_TICK) {
                if (newZ < north) {
                    if (crossing == Crossing.NONE) crossing = Crossing.NORTH;
                    newZ = 2.0D * north - newZ;
                    newX += half;
                } else if (newZ > south) {
                    if (crossing == Crossing.NONE) crossing = Crossing.SOUTH;
                    newZ = 2.0D * south - newZ;
                    newX += half;
                }
                reflectNorthSouth = !reflectNorthSouth;
                crossed = true;
                reflections++;
            }

            if (newZ < north || newZ > south) {
                double polarSpan = south - north;
                double doubleSpan = polarSpan * 2.0D;
                double offset = positiveModulo(newZ - north, doubleSpan);
                boolean secondHalf = offset > polarSpan;
                newZ = secondHalf ? south - (offset - polarSpan) : north + offset;
                if (secondHalf) {
                    newX += half;
                    reflectNorthSouth = !reflectNorthSouth;
                }
                crossed = true;
            }
        }

        if (SphericalWorldBorderConfig.WRAP_LONGITUDE.get() && (newX < west || newX > east)) {
            if (crossing == Crossing.NONE) crossing = newX < west ? Crossing.WEST : Crossing.EAST;
            newX = wrapCoordinate(newX, west, east);
            crossed = true;
        }

        if (!crossed) return;

        // Never place an entity directly back on a trigger plane. At a very shallow
        // approach angle, tiny floating-point movement around the boundary can otherwise
        // satisfy the crossing test again on the next tick and make the entity jitter or
        // appear stuck to the teleport line. Move successful crossings a couple of blocks
        // into the destination side before running the safe-destination search.
        newX = applyCrossingClearanceX(crossing, newX, west, east);
        newZ = applyCrossingClearanceZ(crossing, newZ, north, south);

        float yaw = entity.getYRot();
        Vec3 oldMotion = entity.getDeltaMovement();
        if (reflectNorthSouth) yaw = wrapDegrees(180.0F - yaw);

        double newY = oldY;
        boolean verticalAdjusted = false;

        if (SphericalWorldBorderConfig.SAFE_TELEPORT.get()) {
            boolean sourceInWater = isWaterAtEntity(level, entity, oldX, oldY, oldZ);
            SafeDestination safe = findSafeDestination(level, entity, newX, oldY, newZ, false, sourceInWater);
            if (safe != null) {
                // Nearby water/surface searches are allowed to move horizontally. Clamp
                // the final candidate away from the trigger plane as well, then verify
                // that the clamped position is still genuinely safe. If it is not, fail
                // over to the existing source-side fallback instead of risking a re-trigger.
                SafeDestination cleared = enforceCrossingClearance(safe, crossing, west, east, north, south);
                if (isClearedDestinationSafe(level, entity, cleared, sourceInWater)) {
                    newX = cleared.x;
                    newY = cleared.y;
                    newZ = cleared.z;
                    verticalAdjusted = Math.abs(newY - oldY) > 0.001D;
                } else {
                    safe = null;
                }
            }

            if (safe == null) {
                SafeDestination fallback = findSafeDestination(
                        level,
                        entity,
                        fallbackX(crossing, oldX, west, east),
                        oldY,
                        fallbackZ(crossing, oldZ, north, south),
                        true,
                        sourceInWater
                );
                if (fallback == null) return;

                newX = fallback.x;
                newY = fallback.y;
                verticalAdjusted = true;
                newZ = fallback.z;
                yaw = entity.getYRot();
                reflectNorthSouth = false;
            }
        }

        boolean teleported = entity.teleportTo(level, newX, newY, newZ, Set.of(), yaw, entity.getXRot());

        if (teleported && reflectNorthSouth) {
            entity.setDeltaMovement(oldMotion.x, verticalAdjusted ? 0.0D : oldMotion.y, -oldMotion.z);
        } else if (teleported && verticalAdjusted) {
            entity.setDeltaMovement(oldMotion.x, 0.0D, oldMotion.z);
        }

        if (teleported && verticalAdjusted) entity.resetFallDistance();
    }

    private static SafeDestination findSafeDestination(
            ServerLevel level,
            Entity entity,
            double x,
            double y,
            double z,
            boolean fallbackSearch,
            boolean sourceInWater
    ) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - Math.max(2, (int) Math.ceil(entity.getBbHeight())) - 1;

        // Water crossings preserve the medium first, not merely the numerical Y.
        // This prevents an ocean-level player being accepted inside a dry cave at the
        // same Y on the mirrored side of the world.
        if (sourceInWater) {
            SafeDestination water = findNearbyWaterDestination(level, entity, x, y, z, minY, maxY);
            if (water != null) return water;
        } else if (isIntentionalFreeFlight(entity) && isSafeAt(level, entity, x, y, z, false)) {
            // Elytra / creative flight should keep its altitude when the destination
            // volume is genuinely clear.
            return new SafeDestination(x, y, z);
        }

        SafeDestination surface = findSurfaceDestination(level, entity, x, z, minY, maxY);
        if (surface != null) return surface;

        // Last resort: bias UP from the original altitude. Never search downward into
        // caves. If upward space cannot be proven safe, fail closed and do not teleport.
        if (fallbackSearch) {
            int searchHeight = SphericalWorldBorderConfig.SAFE_TELEPORT_SEARCH_HEIGHT.get();
            int originalY = clamp((int) Math.floor(y), minY, maxY);
            for (int offset = 0; offset <= searchHeight; offset++) {
                int candidateY = originalY + offset;
                if (candidateY > maxY) break;
                if (isSafeAt(level, entity, x, candidateY, z, true)) {
                    return new SafeDestination(x, candidateY, z);
                }
            }
        }

        return null;
    }

    private static SafeDestination findNearbyWaterDestination(
            ServerLevel level,
            Entity entity,
            double x,
            double y,
            double z,
            int minY,
            int maxY
    ) {
        int baseY = clamp((int) Math.floor(y), minY, maxY);

        // Prefer same height, then above, then only a small amount below. Upward wins
        // every tie so ocean/iceberg crossings naturally tend toward open surface water.
        for (int vertical = 0; vertical <= WATER_VERTICAL_SEARCH_RADIUS; vertical++) {
            if (vertical == 0) {
                SafeDestination same = searchWaterRing(level, entity, x, baseY, z, minY, maxY);
                if (same != null) return same;
                continue;
            }

            int upY = baseY + vertical;
            if (upY <= maxY) {
                SafeDestination up = searchWaterRing(level, entity, x, upY, z, minY, maxY);
                if (up != null) return up;
            }

            // Only modest downward correction is allowed for water continuity. A dry
            // cave can never qualify because isWaterDestination requires water.
            int downY = baseY - vertical;
            if (downY >= minY) {
                SafeDestination down = searchWaterRing(level, entity, x, downY, z, minY, maxY);
                if (down != null) return down;
            }
        }

        return null;
    }

    private static SafeDestination searchWaterRing(
            ServerLevel level,
            Entity entity,
            double x,
            int y,
            double z,
            int minY,
            int maxY
    ) {
        if (y < minY || y > maxY) return null;

        // Exact mirrored point first.
        if (isWaterDestination(level, entity, x, y, z)) {
            return new SafeDestination(x, y, z);
        }

        // Then expand horizontally. Searching just the perimeter at each radius keeps
        // the number of probes bounded while still finding open water around icebergs.
        for (int radius = 1; radius <= WATER_HORIZONTAL_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                SafeDestination north = waterCandidate(level, entity, x + dx, y, z - radius);
                if (north != null) return north;
                SafeDestination south = waterCandidate(level, entity, x + dx, y, z + radius);
                if (south != null) return south;
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                SafeDestination west = waterCandidate(level, entity, x - radius, y, z + dz);
                if (west != null) return west;
                SafeDestination east = waterCandidate(level, entity, x + radius, y, z + dz);
                if (east != null) return east;
            }
        }

        return null;
    }

    private static SafeDestination waterCandidate(ServerLevel level, Entity entity, double x, int y, double z) {
        return isWaterDestination(level, entity, x, y, z) ? new SafeDestination(x, y, z) : null;
    }

    private static SafeDestination findSurfaceDestination(
            ServerLevel level,
            Entity entity,
            double x,
            double z,
            int minY,
            int maxY
    ) {
        int searchHeight = SphericalWorldBorderConfig.SAFE_TELEPORT_SEARCH_HEIGHT.get();

        SafeDestination exact = findSafeSurfaceInColumn(level, entity, x, z, minY, maxY, searchHeight);
        if (exact != null) return exact;

        // If the mirrored column is an iceberg, cliff, tree or other awkward terrain,
        // try nearby columns rather than descending into the same column's caves.
        for (int radius = 1; radius <= SURFACE_HORIZONTAL_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                SafeDestination north = findSafeSurfaceInColumn(level, entity, x + dx, z - radius, minY, maxY, searchHeight);
                if (north != null) return north;
                SafeDestination south = findSafeSurfaceInColumn(level, entity, x + dx, z + radius, minY, maxY, searchHeight);
                if (south != null) return south;
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                SafeDestination west = findSafeSurfaceInColumn(level, entity, x - radius, z + dz, minY, maxY, searchHeight);
                if (west != null) return west;
                SafeDestination east = findSafeSurfaceInColumn(level, entity, x + radius, z + dz, minY, maxY, searchHeight);
                if (east != null) return east;
            }
        }

        return null;
    }

    private static SafeDestination findSafeSurfaceInColumn(
            ServerLevel level,
            Entity entity,
            double x,
            double z,
            int minY,
            int maxY,
            int searchHeight
    ) {
        int blockX = floorToInt(x);
        int blockZ = floorToInt(z);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        int startY = clamp(surfaceY + SURFACE_CLEARANCE, minY, maxY);
        int upperY = Math.min(maxY, startY + searchHeight);

        for (int candidateY = startY; candidateY <= upperY; candidateY++) {
            if (isSafeAt(level, entity, x, candidateY, z, true)) {
                return new SafeDestination(x, candidateY, z);
            }
        }
        return null;
    }

    private static boolean isWaterDestination(ServerLevel level, Entity entity, double x, double y, double z) {
        if (!isSafeAt(level, entity, x, y, z, false)) return false;
        return isWaterAtEntity(level, entity, x, y, z);
    }

    private static boolean isWaterAtEntity(ServerLevel level, Entity entity, double x, double y, double z) {
        BlockPos feet = BlockPos.containing(x, y + 0.05D, z);
        BlockPos middle = BlockPos.containing(x, y + Math.max(0.25D, entity.getBbHeight() * 0.5D), z);
        return level.getFluidState(feet).is(FluidTags.WATER)
                || level.getFluidState(middle).is(FluidTags.WATER);
    }

    private static boolean isSafeAt(ServerLevel level, Entity entity, double x, double y, double z, boolean requireSupport) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - Math.max(2, (int) Math.ceil(entity.getBbHeight())) - 1;
        if (y < minY || y > maxY) return false;

        AABB currentBox = entity.getBoundingBox();
        AABB destinationBox = currentBox.move(x - entity.getX(), y - entity.getY(), z - entity.getZ());

        if (!level.noCollision(entity, destinationBox)) return false;

        if (requireSupport) {
            boolean hasSolidSupport = !level.noCollision(entity, destinationBox.move(0.0D, -0.125D, 0.0D));
            BlockPos supportProbe = BlockPos.containing(x, y - 0.05D, z);
            boolean hasSafeWater = level.getFluidState(supportProbe).is(FluidTags.WATER);
            if (!hasSolidSupport && !hasSafeWater) return false;
        }

        BlockPos feet = BlockPos.containing(x, y + 0.05D, z);
        BlockPos upperBody = BlockPos.containing(x, y + Math.max(0.5D, entity.getBbHeight() * 0.85D), z);
        if (level.getFluidState(feet).is(FluidTags.LAVA) || level.getFluidState(upperBody).is(FluidTags.LAVA)) return false;

        BlockPos supportPos = BlockPos.containing(x, y - 0.05D, z);
        BlockState support = level.getBlockState(supportPos);
        return !isDangerousSupport(support);
    }

    private static boolean isIntentionalFreeFlight(Entity entity) {
        if (entity instanceof LivingEntity living && living.isFallFlying()) return true;
        return entity instanceof Player player && player.getAbilities().flying;
    }

    private static boolean isDangerousSupport(BlockState state) {
        return state.is(Blocks.BEDROCK)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(BlockTags.FIRE)
                || state.is(BlockTags.CAMPFIRES);
    }

    private static double applyCrossingClearanceX(Crossing crossing, double x, double west, double east) {
        return switch (crossing) {
            // Crossing EAST wraps to the WEST edge, so push eastward into the map.
            case EAST -> Math.max(x, west + BOUNDARY_EXIT_CLEARANCE);
            // Crossing WEST wraps to the EAST edge, so push westward into the map.
            case WEST -> Math.min(x, east - BOUNDARY_EXIT_CLEARANCE);
            default -> x;
        };
    }

    private static double applyCrossingClearanceZ(Crossing crossing, double z, double north, double south) {
        return switch (crossing) {
            // Pole reflection remains in the same polar coordinate region. Push away
            // from the pole in the direction the reflected movement now travels.
            case NORTH -> Math.max(z, north + BOUNDARY_EXIT_CLEARANCE);
            case SOUTH -> Math.min(z, south - BOUNDARY_EXIT_CLEARANCE);
            default -> z;
        };
    }

    private static SafeDestination enforceCrossingClearance(
            SafeDestination destination,
            Crossing crossing,
            double west,
            double east,
            double north,
            double south
    ) {
        return new SafeDestination(
                applyCrossingClearanceX(crossing, destination.x, west, east),
                destination.y,
                applyCrossingClearanceZ(crossing, destination.z, north, south)
        );
    }

    private static boolean isClearedDestinationSafe(
            ServerLevel level,
            Entity entity,
            SafeDestination destination,
            boolean sourceInWater
    ) {
        if (sourceInWater) {
            return isWaterDestination(level, entity, destination.x, destination.y, destination.z);
        }
        return isSafeAt(
                level,
                entity,
                destination.x,
                destination.y,
                destination.z,
                !isIntentionalFreeFlight(entity)
        );
    }

    private static double fallbackX(Crossing crossing, double originalX, double west, double east) {
        return switch (crossing) {
            case WEST -> west + FALLBACK_INSET;
            case EAST -> east - FALLBACK_INSET;
            default -> originalX;
        };
    }

    private static double fallbackZ(Crossing crossing, double originalZ, double north, double south) {
        return switch (crossing) {
            case NORTH -> north + FALLBACK_INSET;
            case SOUTH -> south - FALLBACK_INSET;
            default -> originalZ;
        };
    }

    private static boolean isPlayerClass(Entity entity) {
        Class<?> type = entity.getClass();
        while (type != null) {
            if ("net.minecraft.server.level.ServerPlayer".equals(type.getName())) return true;
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean isCreateEntity(Entity entity) {
        String className = entity.getClass().getName();
        return className.startsWith("com.simibubi.create.content.trains.")
                || className.startsWith("com.simibubi.create.content.contraptions.");
    }

    private static double wrapCoordinate(double value, double min, double max) {
        if (value >= min && value <= max) return value;
        double width = max - min;
        return min + positiveModulo(value - min, width);
    }

    private static double positiveModulo(double value, double modulus) {
        double result = value % modulus;
        return result < 0.0D ? result + modulus : result;
    }

    private static int floorToInt(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float wrapDegrees(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }

    private enum Crossing {
        NONE, NORTH, SOUTH, EAST, WEST
    }

    private static final class SafeDestination {
        private final double x;
        private final double y;
        private final double z;

        private SafeDestination(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
