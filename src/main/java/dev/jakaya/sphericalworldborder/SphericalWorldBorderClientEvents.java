package dev.jakaya.sphericalworldborder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class SphericalWorldBorderClientEvents {
    private static int particleTick;

    private SphericalWorldBorderClientEvents() {}

    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!isClientFeatureEnabled() || !((Boolean) SphericalWorldBorderConfig.SHOW_BORDER_WARNINGS.get())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (((Boolean) SphericalWorldBorderConfig.OVERWORLD_ONLY.get()) && minecraft.level.dimension() != Level.OVERWORLD) {
            return;
        }

        Boundary boundary = nearestBoundary(player.getX(), player.getZ());
        int warningDistance = (Integer) SphericalWorldBorderConfig.WARNING_DISTANCE.get();
        if (boundary == null || boundary.distance > warningDistance) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int centerX = gui.guiWidth() / 2;
        int topY = 18;
        int panelWidth = Math.min(286, Math.max(180, gui.guiWidth() - 24));
        int left = centerX - panelWidth / 2;
        int right = centerX + panelWidth / 2;

        gui.fill(left, topY, right, topY + 31, 0xA0101010);
        gui.hLine(left + 1, right - 1, topY, boundary.accentColor);
        gui.drawCenteredString(minecraft.font, "Approaching " + boundary.displayName, centerX, topY + 6, 0xFFFFFFFF);
        gui.drawCenteredString(minecraft.font, formatDistance(boundary.distance) + " blocks to crossing", centerX, topY + 18, 0xFFD8D8D8);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!isClientFeatureEnabled() || !((Boolean) SphericalWorldBorderConfig.SHOW_VISIBLE_BORDERS.get())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        if (((Boolean) SphericalWorldBorderConfig.OVERWORLD_ONLY.get()) && level.dimension() != Level.OVERWORLD) {
            return;
        }

        // A gentle shimmer rather than a solid renderer: low-risk with LOD/rendering mods.
        particleTick++;
        if ((particleTick % 5) != 0) {
            return;
        }

        int visibleDistance = (Integer) SphericalWorldBorderConfig.VISIBLE_BORDER_DISTANCE.get();
        int circumference = (Integer) SphericalWorldBorderConfig.CIRCUMFERENCE.get();
        double half = circumference / 2.0D;
        double quarter = circumference / 4.0D;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if ((Boolean) SphericalWorldBorderConfig.WRAP_LONGITUDE.get()) {
            renderXBoundary(level, -half, x, y, z, visibleDistance); // West
            renderXBoundary(level,  half, x, y, z, visibleDistance); // East
        }
        if ((Boolean) SphericalWorldBorderConfig.CROSS_POLES.get()) {
            renderZBoundary(level, -quarter, x, y, z, visibleDistance); // North
            renderZBoundary(level,  quarter, x, y, z, visibleDistance); // South
        }
    }

    private static boolean isClientFeatureEnabled() {
        return (Boolean) SphericalWorldBorderConfig.ENABLED.get();
    }

    private static void renderXBoundary(ClientLevel level, double boundaryX, double playerX, double playerY, double playerZ, int visibleDistance) {
        if (Math.abs(playerX - boundaryX) > visibleDistance) {
            return;
        }
        for (int along = -24; along <= 24; along += 6) {
            for (int up = -4; up <= 16; up += 4) {
                double px = boundaryX;
                double py = playerY + up;
                double pz = playerZ + along;
                level.addParticle(ParticleTypes.END_ROD, px, py, pz, 0.0D, 0.003D, 0.0D);
            }
        }
    }

    private static void renderZBoundary(ClientLevel level, double boundaryZ, double playerX, double playerY, double playerZ, int visibleDistance) {
        if (Math.abs(playerZ - boundaryZ) > visibleDistance) {
            return;
        }
        for (int along = -24; along <= 24; along += 6) {
            for (int up = -4; up <= 16; up += 4) {
                double px = playerX + along;
                double py = playerY + up;
                double pz = boundaryZ;
                level.addParticle(ParticleTypes.END_ROD, px, py, pz, 0.0D, 0.003D, 0.0D);
            }
        }
    }

    private static Boundary nearestBoundary(double x, double z) {
        int circumference = (Integer) SphericalWorldBorderConfig.CIRCUMFERENCE.get();
        if (circumference < 64) {
            return null;
        }

        double half = circumference / 2.0D;
        double quarter = circumference / 4.0D;
        Boundary nearest = null;

        if ((Boolean) SphericalWorldBorderConfig.WRAP_LONGITUDE.get()) {
            nearest = nearer(nearest, new Boundary("West Longitude Seam", Math.abs(x + half), 0xFF5AA9FF));
            nearest = nearer(nearest, new Boundary("East Longitude Seam", Math.abs(half - x), 0xFF5AA9FF));
        }
        if ((Boolean) SphericalWorldBorderConfig.CROSS_POLES.get()) {
            nearest = nearer(nearest, new Boundary("North Pole", Math.abs(z + quarter), 0xFF9ADFFF));
            nearest = nearer(nearest, new Boundary("South Pole", Math.abs(quarter - z), 0xFF9ADFFF));
        }
        return nearest;
    }

    private static Boundary nearer(Boundary current, Boundary candidate) {
        return current == null || candidate.distance < current.distance ? candidate : current;
    }

    private static String formatDistance(double distance) {
        if (distance < 10.0D) {
            return String.format(java.util.Locale.ROOT, "%.1f", Math.max(0.0D, distance));
        }
        return Long.toString(Math.max(0L, Math.round(distance)));
    }

    private static final class Boundary {
        private final String displayName;
        private final double distance;
        private final int accentColor;

        private Boundary(String displayName, double distance, int accentColor) {
            this.displayName = displayName;
            this.distance = distance;
            this.accentColor = accentColor;
        }
    }
}
