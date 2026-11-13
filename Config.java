package com.maxivcapture;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Settings, saved in config/maxivcapture.properties (created on first start). */
public final class Config {
    public static volatile int cropGrowthMultiplier = 1;
    public static volatile boolean guaranteedPokemonDrops = true;

    private Config() {}

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("maxivcapture.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    p.load(in);
                }
            }
            cropGrowthMultiplier = Math.max(1, Math.min(100, Integer.parseInt(p.getProperty("cropGrowthMultiplier", "1").trim())));
            guaranteedPokemonDrops = Boolean.parseBoolean(p.getProperty("guaranteedPokemonDrops", "true").trim());
        } catch (Exception e) {
            MaxIvCapture.LOGGER.warn("Bad config, using defaults: {}", e.toString());
        }
        p.setProperty("cropGrowthMultiplier", String.valueOf(cropGrowthMultiplier));
        p.setProperty("guaranteedPokemonDrops", String.valueOf(guaranteedPokemonDrops));
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "Max IV Capture. cropGrowthMultiplier: 1 (normal) to 100. guaranteedPokemonDrops: true = every Pokemon drops its whole drop table.");
        } catch (IOException ignored) {
        }
    }

    public static int cropGrowth() { return cropGrowthMultiplier; }
    public static boolean allDrops() { return guaranteedPokemonDrops; }
}
