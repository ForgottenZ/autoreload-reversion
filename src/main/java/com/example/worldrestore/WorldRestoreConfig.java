package com.example.worldrestore;

import net.minecraftforge.common.ForgeConfigSpec;

public final class WorldRestoreConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Values VALUES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        VALUES = new Values(builder);
        SPEC = builder.build();
    }

    private WorldRestoreConfig() {
    }

    public static final class Values {
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.ConfigValue<String> templateZipPath;
        public final ForgeConfigSpec.BooleanValue failHardIfMissing;
        public final ForgeConfigSpec.BooleanValue preserveServerConfigDir;

        private Values(ForgeConfigSpec.Builder builder) {
            builder.push("world_restore");

            enabled = builder
                    .comment("Enable world restore on every server start.")
                    .define("enabled", true);

            templateZipPath = builder
                    .comment("Path to the template zip. Relative paths are resolved from the server directory.")
                    .define("templateZipPath", "world_template.zip");

            failHardIfMissing = builder
                    .comment("If true, server startup fails when template zip is missing.")
                    .define("failHardIfMissing", true);

            preserveServerConfigDir = builder
                    .comment("If true, keep world/serverconfig when restoring.")
                    .define("preserveServerConfigDir", false);

            builder.pop();
        }
    }
}
