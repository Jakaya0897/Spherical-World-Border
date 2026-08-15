package dev.jakaya.sphericalworldborder;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SphericalWorldBorderConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue OVERWORLD_ONLY;
    public static final ModConfigSpec.IntValue CIRCUMFERENCE;
    public static final ModConfigSpec.BooleanValue WRAP_LONGITUDE;
    public static final ModConfigSpec.BooleanValue CROSS_POLES;
    public static final ModConfigSpec.BooleanValue TELEPORT_NON_PLAYERS;
    public static final ModConfigSpec.BooleanValue EXCLUDE_CREATE_ENTITIES;
    public static final ModConfigSpec.BooleanValue SHOW_BORDER_WARNINGS;
    public static final ModConfigSpec.IntValue WARNING_DISTANCE;
    public static final ModConfigSpec.BooleanValue SHOW_VISIBLE_BORDERS;
    public static final ModConfigSpec.IntValue VISIBLE_BORDER_DISTANCE;

    private SphericalWorldBorderConfig() {}

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("============================================================");
        builder.comment(" Spherical World Border");
        builder.comment(" Author: Jakaya");
        builder.comment(" Version: 0.2.0-alpha.1");
        builder.comment(" Minecraft: 1.21.1 | Loader: NeoForge");
        builder.comment("============================================================");
        builder.comment("ALPHA SOFTWARE - back up important worlds before testing.");
        builder.comment("This mod simulates globe-like travel while keeping normal Minecraft coordinates.");
        builder.comment("East/west travel wraps around longitude. Crossing a pole reflects latitude");
        builder.comment("back into the same polar region and shifts longitude by 180 degrees.");
        builder.comment("");
        builder.comment("PLANET GEOMETRY");
        builder.comment("C = circumference in blocks");
        builder.comment("West/East seam = -C/2 and +C/2");
        builder.comment("North/South poles = -C/4 and +C/4");
        builder.comment("Natural Temperature equatorial_distance = C/8");
        builder.comment("For the default C=1,000,000: seams are X +/-500,000, poles are Z +/-250,000,");
        builder.comment("and Natural Temperature EQD should be 125,000.");
        builder.comment("============================================================");

        ENABLED = builder
                .comment("Master switch for all spherical boundary behaviour.")
                .define("enabled", true);

        OVERWORLD_ONLY = builder
                .comment("Restrict spherical boundary handling to the Overworld.")
                .comment("Recommended: true. Other dimensions keep their normal Minecraft behaviour.")
                .define("overworldOnly", true);

        builder.comment("");
        builder.comment("--- PLANET SIZE ---");
        builder.comment("Full planet circumference in blocks.");
        builder.comment("Default: 1,000,000 blocks.");
        builder.comment("For Natural Temperature, set equatorial_distance and equator_offset to circumference / 8.");
        builder.comment("Recommended values are multiples of 8 so all derived boundaries are whole blocks.");
        CIRCUMFERENCE = builder.defineInRange("circumference", 1_000_000, 64, 30_000_000);

        builder.comment("");
        builder.comment("--- LONGITUDE ---");
        builder.comment("Wrap east/west travel across the longitude seam.");
        builder.comment("Example with C=1,000,000: +500,000 X wraps to -500,000 X and vice versa.");
        WRAP_LONGITUDE = builder.define("wrapLongitude", true);

        builder.comment("");
        builder.comment("--- POLES ---");
        builder.comment("Enable spherical north/south pole crossing.");
        builder.comment("Crossing a pole reflects Z back inside the same polar region and shifts X by C/2,");
        builder.comment("representing a 180-degree longitude change. North/south facing and Z momentum are reflected.");
        CROSS_POLES = builder.define("crossPoles", true);

        builder.comment("");
        builder.comment("--- PLAYER BORDER WARNINGS ---");
        builder.comment("Show a clean HUD warning when the player gets close to a spherical boundary.");
        builder.comment("The warning identifies East Longitude Seam, West Longitude Seam, North Pole, or South Pole");
        builder.comment("and displays the remaining distance in blocks.");
        SHOW_BORDER_WARNINGS = builder.define("showBorderWarnings", true);

        builder.comment("Distance in blocks at which the on-screen border warning appears.");
        WARNING_DISTANCE = builder.defineInRange("warningDistance", 250, 8, 10_000);

        builder.comment("");
        builder.comment("--- VISIBLE BORDER SHIMMER ---");
        builder.comment("Visualise nearby teleport/crossing boundaries as a subtle client-side particle curtain.");
        builder.comment("This is cosmetic only: it does not create collision, alter chunks, or change world generation.");
        builder.comment("Designed to remain friendly to LOD/rendering mods by using ordinary vanilla particles.");
        SHOW_VISIBLE_BORDERS = builder.define("showVisibleBorders", true);

        builder.comment("Distance in blocks at which the visible boundary shimmer begins rendering.");
        builder.comment("Lower values reduce particle activity. Recommended range: 48-128.");
        VISIBLE_BORDER_DISTANCE = builder.defineInRange("visibleBorderDistance", 96, 16, 512);

        builder.comment("");
        builder.comment("--- ENTITIES ---");
        builder.comment("Apply boundary handling to root entities as well as players.");
        builder.comment("Passengers are handled with their root vehicle. Set false for player-only behaviour.");
        TELEPORT_NON_PLAYERS = builder.define("teleportNonPlayers", true);

        builder.comment("Safety compatibility option for Create.");
        builder.comment("When true, Create train/contraption entity classes are ignored by generic boundary teleporting.");
        builder.comment("Keep this enabled unless dedicated Create integration has been tested for your setup.");
        EXCLUDE_CREATE_ENTITIES = builder.define("excludeCreateEntities", true);

        SPEC = builder.build();
    }
}
