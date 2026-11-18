package com.maxivcapture;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Faster pasture drops. Cobbleverse's pasture drops come from the "Pasture Loot" mod, which has a setting
 * drops.per_hour in config/pastureloot.toml (default 3). This sets that number for you, then reloads it.
 * The drops themselves are 100% because DropAll already changes every Pokemon drop table.
 */
public class PastureTweak {
    private static final Pattern LINE = Pattern.compile("(?m)^(\\s*(?:drops\\s*\\.\\s*)?per_hour\\s*=\\s*)([^\\r\\n#]*?)(\\s*(?:#.*)?)$");

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(PastureTweak::apply);
    }

    private static void apply(MinecraftServer server) {
        int perHour = Config.pastureDropsPerHour();
        if (perHour <= 0) return;
        Path file = FabricLoader.getInstance().getConfigDir().resolve("pastureloot.toml");
        if (!Files.exists(file)) {
            MaxIvCapture.LOGGER.info("Pasture drops: config/pastureloot.toml not found (Pasture Loot not installed?), skipped.");
            return;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Matcher m = LINE.matcher(text);
            if (!m.find()) {
                MaxIvCapture.LOGGER.warn("Pasture drops: no per_hour line in pastureloot.toml, skipped.");
                return;
            }
            String current = m.group(2).trim();
            if (!current.equals(perHour + ".0") && !current.equals(String.valueOf(perHour))) {
                String out = m.replaceFirst(Matcher.quoteReplacement(m.group(1) + perHour + ".0" + m.group(3)));
                Files.writeString(file, out, StandardCharsets.UTF_8);
                MaxIvCapture.LOGGER.info("Pasture drops: set drops.per_hour to {} (was {}).", perHour, current);
            }
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "pastureloot reload");
            } catch (Throwable t) {
                MaxIvCapture.LOGGER.warn("Pasture drops: could not run /pastureloot reload ({}). Run it yourself once.", t.toString());
            }
        } catch (Exception e) {
            MaxIvCapture.LOGGER.warn("Pasture drops: could not edit pastureloot.toml: {}", e.toString());
        }
    }
}
