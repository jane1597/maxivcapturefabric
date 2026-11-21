package com.maxivcapture;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Exp. Share for the whole party, like the newer Pokemon games: when a Pokemon of yours earns battle experience,
 * every other (not fainted) Pokemon in the same player's party gets a share of it (default 50%).
 *
 * How: we listen to Cobblemon's "experience gained (pre)" event. All gains of one knock-out arrive in the same tick,
 * so they are collected per player and handled at the end of that tick: the largest single gain (times
 * Config.expShareRatio) goes to every party member that did not get anything. Our own grants use a "sidemod"
 * source and are ignored by the listener, so nothing loops. Everything is reflection + try/catch.
 */
public final class ExpShare {
    private ExpShare() {}

    private static final class Gain {
        int max;
        final Set<Object> got = new HashSet<>();
        Object owner; // ServerPlayer
    }

    private static final Map<UUID, Gain> GAINS = new HashMap<>();
    private static boolean subscribed = false;
    private static Object SERVER = null;

    public static void register() {
        try {
            Class<?> events = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents");
            Object obs = events.getField("EXPERIENCE_GAINED_EVENT_PRE").get(null);
            Class<?> prio = Class.forName("com.cobblemon.mod.common.api.Priority");
            Object normal = prio.getField("LOWEST").get(null); // last, so other mods have already changed the amount
            Method sub = null;
            for (Method m : obs.getClass().getMethods()) {
                if (m.getName().equals("subscribe") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == prio) {
                    sub = m;
                    break;
                }
            }
            if (sub == null) throw new NoSuchMethodException("subscribe(Priority, handler)");
            kotlin.jvm.functions.Function1<Object, kotlin.Unit> handler = e -> {
                try {
                    onGain(e);
                } catch (Throwable t) {
                    MaxIvCapture.LOGGER.warn("Exp share: {}", t.toString());
                }
                return kotlin.Unit.INSTANCE;
            };
            sub.invoke(obs, normal, handler);
            subscribed = true;
            MaxIvCapture.LOGGER.info("Exp share: party-wide experience is on.");
        } catch (Throwable t) {
            MaxIvCapture.LOGGER.warn("Exp share: could not hook Cobblemon's experience event ({}). Feature off.", t.toString());
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            SERVER = server;
            flush();
        });
    }

    private static Object call(Object o, String name) throws Exception {
        return o.getClass().getMethod(name).invoke(o);
    }

    private static void onGain(Object event) throws Exception {
        if (!Config.expShare()) return;
        try {
            if ((Boolean) call(event, "isCanceled")) return;
        } catch (NoSuchMethodException ignored) {
        }
        Object source = call(event, "getSource");
        if (source == null || !source.getClass().getSimpleName().contains("Battle")) return; // ignore our own and non-battle xp
        Object pokemon = call(event, "getPokemon");
        Object xpObj = call(event, "getExperience");
        int xp = xpObj instanceof Number n ? n.intValue() : 0;
        if (xp <= 0) return;
        Object owner = null;
        try {
            owner = call(pokemon, "getOwnerPlayer");
        } catch (Throwable ignored) {
        }
        if (owner == null && SERVER != null) {
            try {
                Object uuid = call(pokemon, "getOwnerUUID");
                Object plist = call(SERVER, "getPlayerList");
                owner = plist.getClass().getMethod("getPlayer", UUID.class).invoke(plist, uuid);
            } catch (Throwable ignored) {
            }
        }
        if (owner == null) return;
        UUID id = (UUID) call(owner, "getUUID");
        Gain g = GAINS.computeIfAbsent(id, k -> new Gain());
        g.owner = owner;
        g.max = Math.max(g.max, xp);
        g.got.add(pokemon);
    }

    private static void flush() {
        if (GAINS.isEmpty()) return;
        Map<UUID, Gain> copy = new HashMap<>(GAINS);
        GAINS.clear();
        for (Gain g : copy.values()) {
            try {
                award(g);
            } catch (Throwable t) {
                MaxIvCapture.LOGGER.warn("Exp share: could not give party experience: {}", t.toString());
            }
        }
    }

    private static void award(Gain g) throws Exception {
        int xp = (int) Math.round(g.max * Config.expShareRatio());
        if (xp <= 0) return;
        Class<?> cob = Class.forName("com.cobblemon.mod.common.Cobblemon");
        Object inst = cob.getField("INSTANCE").get(null);
        Object storage = call(inst, "getStorage");
        Method getParty = null;
        for (Method m : storage.getClass().getMethods()) {
            if (m.getName().equals("getParty") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInstance(g.owner)) {
                getParty = m;
                break;
            }
        }
        if (getParty == null) throw new NoSuchMethodException("getParty(ServerPlayer)");
        Object party = getParty.invoke(storage, g.owner);

        Class<?> srcC = Class.forName("com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource");
        Constructor<?> ctor = srcC.getConstructor(String.class);
        Object source = ctor.newInstance("maxivcapture");

        int given = 0;
        for (Object p : (Iterable<?>) party) {
            if (p == null || g.got.contains(p)) continue;
            boolean fainted = (Boolean) call(p, "isFainted");
            if (fainted) continue;
            Method withPlayer = null, plain = null;
            for (Method m : p.getClass().getMethods()) {
                if (m.getName().equals("addExperienceWithPlayer") && m.getParameterCount() == 3) withPlayer = m;
                if (m.getName().equals("addExperience") && m.getParameterCount() == 2) plain = m;
            }
            if (withPlayer != null) withPlayer.invoke(p, g.owner, source, xp);   // shows level-up / evolution like normal
            else if (plain != null) plain.invoke(p, source, xp);
            else throw new NoSuchMethodException("addExperience(source, xp)");
            given++;
        }
        if (given > 0) {
            try {
                Object msg = Class.forName("net.minecraft.network.chat.Component").getMethod("literal", String.class)
                        .invoke(null, "§aExp. Share: " + given + " other Pokémon got " + xp + " exp.");
                g.owner.getClass().getMethod("sendSystemMessage", Class.forName("net.minecraft.network.chat.Component")).invoke(g.owner, msg);
            } catch (Throwable ignored) {
            }
        }
    }
}
