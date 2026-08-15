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
    public static final ModConfigSpec.BooleanValue SAFE_TELEPORT;
    public static final ModConfigSpec.IntValue SAFE_TELEPORT_SEARCH_HEIGHT;
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
        builder.comment(" Version: 0.2.2-alpha.1");
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
        builder.comment("and a pole crossing shifts longitude by 500,000 blocks.");

        ENABLED = builder.comment("Master switch for all spherical boundary behaviour.")
                .define("enabled", true);
        OVERWORLD_ONLY = builder.comment("Restrict spherical boundary handling to the Overworld.")
                .comment("Recommended: true. Other dimensions keep their normal Minecraft behaviour.")
                .define("overworldOnly", true);

        builder.comment("").comment("--- PLANET SIZE ---");
        CIRCUMFERENCE = builder.comment("Full planet circumference in blocks.")
                .comment("Default: 1,000,000 blocks.")
                .comment("For Natural Temperature, set equatorial_distance and equator_offset to circumference / 8.")
                .comment("Recommended values are multiples of 8 so all derived boundaries are whole blocks.")
                .defineInRange("circumference", 1_000_000, 64, 30_000_000);

        builder.comment("").comment("--- LONGITUDE ---");
        WRAP_LONGITUDE = builder.comment("Wrap east/west travel across the longitude seam.")
                .comment("Example with C=1,000,000: +500,000 X wraps to -500,000 X and vice versa.")
                .define("wrapLongitude", true);

        builder.comment("").comment("--- POLES ---");
        CROSS_POLES = builder.comment("Enable spherical north/south pole crossing.")
                .comment("Crossing a pole reflects Z back inside the same polar region and shifts X by C/2,")
                .comment("representing a 180-degree longitude change. North/south facing and Z momentum are reflected.")
                .define("crossPoles", true);

        builder.comment("").comment("--- PLAYER BORDER WARNINGS ---");
        SHOW_BORDER_WARNINGS = builder.comment("Show a clean HUD warning when the player gets close to a spherical boundary.")
                .comment("The warning identifies East Longitude Seam, West Longitude Seam, North Pole, or South Pole")
                .comment("and displays the remaining distance in blocks.")
                .define("showBorderWarnings", true);
        WARNING_DISTANCE = builder.comment("Distance in blocks at which the on-screen border warning appears.")
                .defineInRange("warningDistance", 250, 8, 10_000);

        builder.comment("").comment("--- VISIBLE BORDER SHIMMER ---");
        SHOW_VISIBLE_BORDERS = builder.comment("Visualise nearby teleport/crossing boundaries as a subtle client-side particle curtain.")
                .define("showVisibleBorders", true);
        VISIBLE_BORDER_DISTANCE = builder.comment("Distance in blocks at which the particle boundary becomes visible.")
                .comment("Lower values reduce particle activity. Recommended range: 48-128.")
                .defineInRange("visibleBorderDistance", 96, 16, 512);

        builder.comment("").comment("--- SAFE TELEPORT ---");
        SAFE_TELEPORT = builder.comment("Prevent boundary crossings from placing entities inside solid terrain or obvious hazards.")
                .comment("Ordinary walking/jumping requires a verified landing surface or safe water.")
                .comment("Intentional Elytra, swimming and player flight can preserve altitude.")
                .comment("Bedrock is never accepted as a safe landing surface.")
                .comment("If no safe destination is found, the crossing fails closed instead of teleporting into danger.")
                .define("safeTeleport", true);
        SAFE_TELEPORT_SEARCH_HEIGHT = builder.comment("Maximum number of blocks above the destination surface to search for safe free space.")
                .comment("Default: 128. Higher values improve recovery from extreme terrain at a small crossing-time cost.")
                .defineInRange("safeTeleportSearchHeight", 128, 16, 512);

        builder.comment("").comment("--- ENTITIES ---");
        TELEPORT_NON_PLAYERS = builder.comment("Apply boundary handling to root entities as well as players.")
                .comment("Passengers are handled with their root vehicle. Set false for player-only behaviour.")
                .define("teleportNonPlayers", true);
        EXCLUDE_CREATE_ENTITIES = builder.comment("Safety compatibility option for Create.")
                .comment("When true, Create train/contraption entity classes are ignored by generic boundary teleporting.")
                .comment("Keep this enabled unless dedicated Create integration has been tested for your setup.")
                .define("excludeCreateEntities", true);

        SPEC = builder.build();
    }
}
