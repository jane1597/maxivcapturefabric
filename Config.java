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
    /** About how long one Cobblemon berry growth stage takes without help (seconds). Only scales the boost. */
    public static volatile int cobblemonStageSeconds = 300;
    /** Catch rate multiplier applied on top of every Poke Ball (1 = vanilla). */
    /** Party-wide exp share on knock-out, and how much of the earned exp the non-participants get (0.5 = half). */
    public static volatile boolean expShare = true;
    public static volatile double expShareRatio = 0.5;
    public static volatile double ballMultiplier = 2.0;
    public static volatile int cropGrowthMultiplier = 1;
    public static volatile boolean guaranteedPokemonDrops = true;

    /** Master switch for everything that touches the battle engine. Battle luck for the Pokemon of player side p1 (needs Mega Showdown). */
    /** Ticks to wait after a battle starts before the luck is sent (20 ticks = 1 second). */
    public static volatile int luckDelayTicks = 100;
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
            cobblemonStageSeconds = Math.max(5, Math.min(7200, Integer.parseInt(p.getProperty("cobblemonStageSeconds", "300").trim())));
            if (!"5".equals(p.getProperty("configVersion", "").trim())) p.setProperty("expShareRatio", "0.5"); // 1.1.17: default is 50% now
            expShare = Boolean.parseBoolean(p.getProperty("expShare", "true").trim());
            expShareRatio = Math.max(0.0, Math.min(5.0, Double.parseDouble(p.getProperty("expShareRatio", "0.5").trim())));
            ballMultiplier = Math.max(1.0, Math.min(20.0, Double.parseDouble(p.getProperty("ballMultiplier", "2.0").trim())));
            cropGrowthMultiplier = Math.max(1, Math.min(100, Integer.parseInt(p.getProperty("cropGrowthMultiplier", "1").trim())));
            guaranteedPokemonDrops = Boolean.parseBoolean(p.getProperty("guaranteedPokemonDrops", "true").trim());
            luckyBattles = Boolean.parseBoolean(p.getProperty("luckyBattles", "true").trim());
            // Config version 4: new luck delivery (Showdown actions script, no more >eval). Reset the old saved value once.
            if (Integer.parseInt(p.getProperty("configVersion", "0").trim()) < 4) luckyBattles = true;
            luckDelayTicks = Math.max(0, Math.min(1200, Integer.parseInt(p.getProperty("luckDelayTicks", "100").trim())));
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
        StringBuilder s = new StringBuilder();
        s.append("# ==========================================================================\n");
        s.append("#  Max IV Capture - settings\n");
        s.append("#  Edit, save, then run /maxiv reload in game (or restart). true/false or numbers.\n");
        s.append("#  You can also change most of these in game: /maxiv status, /maxiv set <name> <true|false>, /maxiv ball <x>\n");
        s.append("# ==========================================================================\n\n");

        s.append("# ---------- Pokeballs ----------\n");
        s.append("# Catch rate multiplier on top of EVERY ball. 1 = vanilla, 2 = double (Poke 1x->2x, Great 1.5x->3x ...). Range 1-20.\n");
        s.append("ballMultiplier=").append(ballMultiplier).append("\n\n");

        s.append("# ---------- Exp. Share ----------\n");
        s.append("# true = when one of your Pokemon earns battle exp, the other healthy Pokemon in your party get a share too.\n");
        s.append("expShare=").append(expShare).append("\n");
        s.append("# How much of the winner's exp the others get. 0.5 = half (500 exp -> 250), 1.0 = full. Range 0-5.\n");
        s.append("expShareRatio=").append(expShareRatio).append("\n\n");

        s.append("# ---------- Battle luck (your Pokemon only; needs Mega Showdown) ----------\n");
        s.append("# Master switch for everything below. false = the mod never touches battles.\n");
        s.append("luckyBattles=").append(luckyBattles).append("\n");
        s.append("# Every attack is a critical hit.\n");
        s.append("alwaysCrit=").append(alwaysCrit).append("\n");
        s.append("# Moves never miss.\n");
        s.append("alwaysHit=").append(alwaysHit).append("\n");
        s.append("# Multi-hit moves (Fury Attack, Bullet Seed ...) always hit the maximum number of times.\n");
        s.append("maxMultiHit=").append(maxMultiHit).append("\n");
        s.append("# Every 'chance' move effect always happens (flinch, burn, paralysis, stat drops ...), incl. King's Rock / Razor Fang.\n");
        s.append("guaranteedEffects=").append(guaranteedEffects).append("\n");
        s.append("# Held items get stronger: Choice Band, Leftovers etc. doubled; Quick Claw and Focus Band always activate.\n");
        s.append("doubleItemEffects=").append(doubleItemEffects).append("\n\n");

        s.append("# ---------- Capture / drops / rides ----------\n");
        s.append("# Drops: every Pokemon drops its WHOLE drop table (100% chance for every item).\n");
        s.append("guaranteedPokemonDrops=").append(guaranteedPokemonDrops).append("\n");
        s.append("# Ride stats of every Pokemon set to the maximum.\n");
        s.append("maxRideStats=").append(maxRideStats).append("\n\n");

        s.append("# ---------- Farming ----------\n");
        s.append("# Crop growth speed. 1 = normal, 10 = ten times faster. Range 1-100. Works for vanilla crops and Cobblemon berries/apricorns.\n");
        s.append("cropGrowthMultiplier=").append(cropGrowthMultiplier).append("\n");
        s.append("# About how many seconds one Cobblemon berry growth stage takes unboosted (only scales the boost; lower = faster berries).\n");
        s.append("cobblemonStageSeconds=").append(cobblemonStageSeconds).append("\n");
        s.append("# Pasture Loot drops per hour per pastured Pokemon (its own default is 3). 0 = do not touch Pasture Loot's file.\n");
        s.append("pastureDropsPerHour=").append(pastureDropsPerHour).append("\n\n");

        s.append("# ---------- Internal (leave alone) ----------\n");
        s.append("configVersion=5\n");
        s.append("luckDelayTicks=").append(luckDelayTicks).append("\n");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, s.toString(), java.nio.charset.StandardCharsets.UTF_8);
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
            case "expShare" -> expShare = v;
            case "maxRideStats" -> maxRideStats = v;
            case "guaranteedPokemonDrops" -> guaranteedPokemonDrops = v;
            default -> { return false; }
        }
        save();
        try { LuckBattle.push(); } catch (Throwable ignored) { }
        return true;
    }

    public static final String[] TOGGLES = {"luckyBattles", "alwaysCrit", "maxMultiHit", "alwaysHit", "guaranteedEffects", "doubleItemEffects", "expShare", "maxRideStats", "guaranteedPokemonDrops"};

    public static boolean get(String name) {
        return switch (name) {
            case "luckyBattles" -> luckyBattles;
            case "alwaysCrit" -> alwaysCrit;
            case "maxMultiHit" -> maxMultiHit;
            case "alwaysHit" -> alwaysHit;
            case "guaranteedEffects" -> guaranteedEffects;
            case "doubleItemEffects" -> doubleItemEffects;
            case "expShare" -> expShare;
            case "maxRideStats" -> maxRideStats;
            case "guaranteedPokemonDrops" -> guaranteedPokemonDrops;
            default -> false;
        };
    }

    public static int cobblemonStageSeconds() { return cobblemonStageSeconds; }
    public static boolean expShare() { return expShare; }
    public static double expShareRatio() { return expShareRatio; }
    public static double ballMultiplier() { return ballMultiplier; }
    public static void setBallMultiplier(double v) { ballMultiplier = Math.max(1.0, Math.min(20.0, v)); save(); }
    public static int cropGrowth() { return cropGrowthMultiplier; }
    public static int luckDelayTicks() { return luckDelayTicks; }
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
