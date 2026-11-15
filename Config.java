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

    public static volatile boolean alwaysCrit = true;
    public static volatile boolean maxMultiHit = true;

    public static volatile boolean alwaysHit = true;
    public static volatile boolean maxRideStats = true;

    public static volatile boolean guaranteedEffects = true;

    public static volatile boolean doubleItemEffects = true;

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
            alwaysCrit = Boolean.parseBoolean(p.getProperty("alwaysCrit", "true").trim());
            maxMultiHit = Boolean.parseBoolean(p.getProperty("maxMultiHit", "true").trim());
            alwaysHit = Boolean.parseBoolean(p.getProperty("alwaysHit", "true").trim());
            guaranteedEffects = Boolean.parseBoolean(p.getProperty("guaranteedEffects", "true").trim());
            doubleItemEffects = Boolean.parseBoolean(p.getProperty("doubleItemEffects", "true").trim());
            maxRideStats = Boolean.parseBoolean(p.getProperty("maxRideStats", "true").trim());
        } catch (Exception e) {
            MaxIvCapture.LOGGER.warn("Bad config, using defaults: {}", e.toString());
        }
        p.setProperty("cropGrowthMultiplier", String.valueOf(cropGrowthMultiplier));
        p.setProperty("guaranteedPokemonDrops", String.valueOf(guaranteedPokemonDrops));
        p.setProperty("alwaysCrit", String.valueOf(alwaysCrit));
        p.setProperty("maxMultiHit", String.valueOf(maxMultiHit));
        p.setProperty("alwaysHit", String.valueOf(alwaysHit));
        p.setProperty("maxRideStats", String.valueOf(maxRideStats));
        p.setProperty("guaranteedEffects", String.valueOf(guaranteedEffects));
        p.setProperty("doubleItemEffects", String.valueOf(doubleItemEffects));
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "Max IV Capture. cropGrowthMultiplier: 1 (normal) to 100. guaranteedPokemonDrops: true = every Pokemon drops its whole drop table.");
        } catch (IOException ignored) {
        }
    }

    public static int cropGrowth() { return cropGrowthMultiplier; }
    public static boolean alwaysCrit() { return alwaysCrit; }
    public static boolean maxMultiHit() { return maxMultiHit; }
    public static boolean alwaysHit() { return alwaysHit; }
    public static boolean maxRideStats() { return maxRideStats; }
    public static boolean guaranteedEffects() { return guaranteedEffects; }
    public static boolean doubleItemEffects() { return doubleItemEffects; }
    public static boolean allDrops() { return guaranteedPokemonDrops; }
}
