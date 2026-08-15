package dev.jakaya.sphericalworldborder;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SphericalWorldBorderMod.MODID, dist = Dist.CLIENT)
public final class SphericalWorldBorderClientMod {
    public SphericalWorldBorderClientMod() {
        NeoForge.EVENT_BUS.addListener(SphericalWorldBorderClientEvents::onRenderGui);
        NeoForge.EVENT_BUS.addListener(SphericalWorldBorderClientEvents::onClientTick);
    }
}
