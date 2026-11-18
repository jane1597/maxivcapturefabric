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

    /** Master switch for everything that touches the battle engine. Turn off to keep the mod away from the battle engine. */
    public static volatile boolean luckyBattles = true;

    public static volatile boolean alwaysCrit = true;
    public static volatile boolean maxMultiHit = true;

    public static volatile boolean alwaysHit = true;
    public static volatile boolean maxRideStats = true;

    public static volatile boolean guaranteedEffects = true;

    public static volatile boolean doubleItemEffects = true;

    /** Pasture Loot drops per hour per Pokemon (its default is 3). 0 = leave Pasture Loot alone. */
    public static volatile int pastureDropsPerHour = 360;

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
            luckyBattles = Boolean.parseBoolean(p.getProperty("luckyBattles", "true").trim());
            alwaysCrit = Boolean.parseBoolean(p.getProperty("alwaysCrit", "true").trim());
            maxMultiHit = Boolean.parseBoolean(p.getProperty("maxMultiHit", "true").trim());
            alwaysHit = Boolean.parseBoolean(p.getProperty("alwaysHit", "true").trim());
            guaranteedEffects = Boolean.parseBoolean(p.getProperty("guaranteedEffects", "true").trim());
            doubleItemEffects = Boolean.parseBoolean(p.getProperty("doubleItemEffects", "true").trim());
            pastureDropsPerHour = Math.max(0, Math.min(3600, Integer.parseInt(p.getProperty("pastureDropsPerHour", "360").trim())));
            maxRideStats = Boolean.parseBoolean(p.getProperty("maxRideStats", "true").trim());
        } catch (Exception e) {
            MaxIvCapture.LOGGER.warn("Bad config, using defaults: {}", e.toString());
        }
        save();
    }

    public static synchronized void save() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("maxivcapture.properties");
        Properties p = new Properties();
        p.setProperty("cropGrowthMultiplier", String.valueOf(cropGrowthMultiplier));
        p.setProperty("guaranteedPokemonDrops", String.valueOf(guaranteedPokemonDrops));
        p.setProperty("luckyBattles", String.valueOf(luckyBattles));
        p.setProperty("alwaysCrit", String.valueOf(alwaysCrit));
        p.setProperty("maxMultiHit", String.valueOf(maxMultiHit));
        p.setProperty("alwaysHit", String.valueOf(alwaysHit));
        p.setProperty("maxRideStats", String.valueOf(maxRideStats));
        p.setProperty("guaranteedEffects", String.valueOf(guaranteedEffects));
        p.setProperty("doubleItemEffects", String.valueOf(doubleItemEffects));
        p.setProperty("pastureDropsPerHour", String.valueOf(pastureDropsPerHour));
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "Max IV Capture. pastureDropsPerHour: Pasture Loot drops per hour per Pokemon (0 = do not change). cropGrowthMultiplier: 1 (normal) to 100. guaranteedPokemonDrops: true = every Pokemon drops its whole drop table.");
        } catch (IOException ignored) {
        }
    }

    /** Used by the /maxiv command. Returns false if the name is unknown. */
    public static boolean set(String name, boolean v) {
        switch (name) {
            case "luckyBattles" -> luckyBattles = v;
            case "alwaysCrit" -> alwaysCrit = v;
            case "maxMultiHit" -> maxMultiHit = v;
            case "alwaysHit" -> alwaysHit = v;
            case "guaranteedEffects" -> guaranteedEffects = v;
            case "doubleItemEffects" -> doubleItemEffects = v;
            case "maxRideStats" -> maxRideStats = v;
            case "guaranteedPokemonDrops" -> guaranteedPokemonDrops = v;
            default -> { return false; }
        }
        save();
        return true;
    }

    public static final String[] TOGGLES = {"luckyBattles", "alwaysCrit", "maxMultiHit", "alwaysHit", "guaranteedEffects", "doubleItemEffects", "maxRideStats", "guaranteedPokemonDrops"};

    public static boolean get(String name) {
        return switch (name) {
            case "luckyBattles" -> luckyBattles;
            case "alwaysCrit" -> alwaysCrit;
            case "maxMultiHit" -> maxMultiHit;
            case "alwaysHit" -> alwaysHit;
            case "guaranteedEffects" -> guaranteedEffects;
            case "doubleItemEffects" -> doubleItemEffects;
            case "maxRideStats" -> maxRideStats;
            case "guaranteedPokemonDrops" -> guaranteedPokemonDrops;
            default -> false;
        };
    }

    public static int cropGrowth() { return cropGrowthMultiplier; }
    public static boolean luckyBattles() { return luckyBattles; }
    public static boolean alwaysCrit() { return alwaysCrit; }
    public static boolean maxMultiHit() { return maxMultiHit; }
    public static boolean alwaysHit() { return alwaysHit; }
    public static boolean maxRideStats() { return maxRideStats; }
    public static boolean guaranteedEffects() { return guaranteedEffects; }
    public static boolean doubleItemEffects() { return doubleItemEffects; }
    public static int pastureDropsPerHour() { return pastureDropsPerHour; }
    public static boolean allDrops() { return guaranteedPokemonDrops; }
}
