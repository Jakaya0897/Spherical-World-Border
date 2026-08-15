package dev.jakaya.sphericalworldborder;

import java.util.Locale;

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
        if (!isClientFeatureEnabled() || !SphericalWorldBorderConfig.SHOW_BORDER_WARNINGS.get()) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) return;
        if (SphericalWorldBorderConfig.OVERWORLD_ONLY.get() && minecraft.level.dimension() != Level.OVERWORLD) return;

        Boundary nearest = nearestBoundary(player.getX(), player.getZ());
        int warningDistance = SphericalWorldBorderConfig.WARNING_DISTANCE.get();
        if (nearest == null || nearest.distance > warningDistance) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int centerX = graphics.guiWidth() / 2;
        int top = 18;
        int width = Math.min(286, Math.max(180, graphics.guiWidth() - 24));
        int left = centerX - width / 2;
        int right = centerX + width / 2;

        graphics.fill(left, top, right, top + 31, 0xA0101010);
        graphics.hLine(left + 1, right - 1, top, nearest.accentColor);
        graphics.drawCenteredString(minecraft.font, "Approaching " + nearest.displayName, centerX, top + 6, -1);
        graphics.drawCenteredString(minecraft.font, formatDistance(nearest.distance) + " blocks to crossing", centerX, top + 18, 0xFFD8D8D8);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!isClientFeatureEnabled() || !SphericalWorldBorderConfig.SHOW_VISIBLE_BORDERS.get()) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;
        if (SphericalWorldBorderConfig.OVERWORLD_ONLY.get() && level.dimension() != Level.OVERWORLD) return;

        particleTick++;
        if (particleTick % 5 != 0) return;

        int visibleDistance = SphericalWorldBorderConfig.VISIBLE_BORDER_DISTANCE.get();
        int circumference = SphericalWorldBorderConfig.CIRCUMFERENCE.get();
        double half = circumference / 2.0D;
        double quarter = circumference / 4.0D;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (SphericalWorldBorderConfig.WRAP_LONGITUDE.get()) {
            renderXBoundary(level, -half, x, y, z, visibleDistance);
            renderXBoundary(level, half, x, y, z, visibleDistance);
        }
        if (SphericalWorldBorderConfig.CROSS_POLES.get()) {
            renderZBoundary(level, -quarter, x, y, z, visibleDistance);
            renderZBoundary(level, quarter, x, y, z, visibleDistance);
        }
    }

    private static boolean isClientFeatureEnabled() {
        return SphericalWorldBorderConfig.ENABLED.get();
    }

    private static void renderXBoundary(ClientLevel level, double boundaryX, double playerX, double playerY, double playerZ, int visibleDistance) {
        if (Math.abs(playerX - boundaryX) > visibleDistance) return;
        for (int dz = -24; dz <= 24; dz += 6) {
            for (int dy = -4; dy <= 16; dy += 4) {
                level.addParticle(ParticleTypes.END_ROD, boundaryX, playerY + dy, playerZ + dz, 0.0D, 0.003D, 0.0D);
            }
        }
    }

    private static void renderZBoundary(ClientLevel level, double boundaryZ, double playerX, double playerY, double playerZ, int visibleDistance) {
        if (Math.abs(playerZ - boundaryZ) > visibleDistance) return;
        for (int dx = -24; dx <= 24; dx += 6) {
            for (int dy = -4; dy <= 16; dy += 4) {
                level.addParticle(ParticleTypes.END_ROD, playerX + dx, playerY + dy, boundaryZ, 0.0D, 0.003D, 0.0D);
            }
        }
    }

    private static Boundary nearestBoundary(double x, double z) {
        int circumference = SphericalWorldBorderConfig.CIRCUMFERENCE.get();
        if (circumference < 64) return null;

        double half = circumference / 2.0D;
        double quarter = circumference / 4.0D;
        Boundary nearest = null;

        if (SphericalWorldBorderConfig.WRAP_LONGITUDE.get()) {
            nearest = nearer(nearest, new Boundary("West Longitude Seam", Math.abs(x + half), 0xFF5AA9FF));
            nearest = nearer(nearest, new Boundary("East Longitude Seam", Math.abs(half - x), 0xFF5AA9FF));
        }
        if (SphericalWorldBorderConfig.CROSS_POLES.get()) {
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
            return String.format(Locale.ROOT, "%.1f", Math.max(0.0D, distance));
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
