package dev.jakaya.sphericalworldborder;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SphericalWorldBorderMod.MODID)
public final class SphericalWorldBorderMod {
    public static final String MODID = "sphericalworldborder";

    public SphericalWorldBorderMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SphericalWorldBorderConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(SphericalWorldBorderEvents::onEntityTick);
    }
}
