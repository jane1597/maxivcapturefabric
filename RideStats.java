package com.maxivcapture;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sets every Cobblemon ride stat (SPEED, ACCELERATION, SKILL, JUMP, STAMINA) of every species to 100.
 * Only players ride Pokemon, so in practice this is "player Pokemon have max ride stats".
 * Done with reflection and logged, so a Cobblemon change can never crash the game.
 */
public final class RideStats {
    private static final Set<String> NAMES = Set.of("SPEED", "ACCELERATION", "SKILL", "JUMP", "STAMINA");
    private static int changed;
    private static final List<String> unknown = new ArrayList<>();

    private RideStats() {}

    public static void apply() {
        if (!Config.maxRideStats()) return;
        changed = 0;
        unknown.clear();
        int species = 0;
        try {
            Class<?> reg = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies");
            Object inst = reg.getField("INSTANCE").get(null);
            Object all = reg.getMethod("getSpecies").invoke(inst);
            for (Object sp : (Collection<?>) all) {
                species++;
                Object riding = null;
                try {
                    riding = sp.getClass().getMethod("getRiding").invoke(sp);
                } catch (Throwable t) {
                    Field f = field(sp.getClass(), "riding");
                    if (f != null) riding = f.get(sp);
                }
                if (riding != null) walk(riding, 0, new IdentityHashMap<>());
            }
        } catch (Throwable t) {
            MaxIvCapture.LOGGER.warn("Max ride stats: could not reach Cobblemon species: {}", t.toString());
            return;
        }
        if (changed > 0) {
            MaxIvCapture.LOGGER.info("Max ride stats: set {} ride stat values to 100 across {} species.", changed, species);
        } else {
            MaxIvCapture.LOGGER.warn("Max ride stats: nothing changed ({} species). Value types seen: {}", species, unknown);
        }
    }

    private static Field field(Class<?> c, String n) {
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(n);
                f.setAccessible(true);
                return f;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean cobbled(Object o) {
        return o.getClass().getName().startsWith("com.cobblemon.");
    }

    private static void walk(Object o, int depth, Map<Object, Boolean> seen) {
        if (o == null || depth > 8 || seen.put(o, true) != null) return;
        try {
            if (o instanceof Map<?, ?> m) {
                handleMap(m, null, null, depth, seen);
                return;
            }
            if (o instanceof Collection<?> c) {
                for (Object e : new ArrayList<>(c)) walk(e, depth + 1, seen);
                return;
            }
            if (!cobbled(o)) return;
            for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
                for (Field f : k.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    Object v = f.get(o);
                    if (v instanceof Map<?, ?> m) {
                        handleMap(m, o, f, depth, seen);
                    } else {
                        walk(v, depth + 1, seen);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void handleMap(Map<?, ?> map, Object holder, Field holderField, int depth, Map<Object, Boolean> seen) {
        boolean isStatMap = false;
        for (Object k : map.keySet()) {
            if (k != null && NAMES.contains(k.toString().toUpperCase())) {
                isStatMap = true;
                break;
            }
        }
        if (!isStatMap) {
            for (Object v : new ArrayList<>(map.values())) walk(v, depth + 1, seen);
            return;
        }
        Map<Object, Object> copy = new LinkedHashMap<>();
        boolean any = false;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            Object nv = e.getValue();
            if (e.getKey() != null && NAMES.contains(e.getKey().toString().toUpperCase())) {
                Object made = hundredLike(e.getValue());
                if (made != null) {
                    nv = made;
                    any = true;
                    changed++;
                }
            }
            copy.put(e.getKey(), nv);
        }
        if (!any) return;
        try {
            ((Map) map).putAll(copy); // works when the map is mutable
        } catch (Throwable t) {
            if (holder != null && holderField != null) {
                try {
                    holderField.set(holder, copy); // immutable map: replace it on its owner
                } catch (Throwable t2) {
                    changed -= copy.size();
                }
            }
        }
    }

    /** Builds a "100" value of the same type as the old one. */
    private static Object hundredLike(Object old) {
        if (old == null) return null;
        Class<?> t = old.getClass();
        try {
            if (t == Integer.class) return 100;
            if (t == Double.class) return 100.0;
            if (t == Float.class) return 100.0f;
            for (Constructor<?> c : t.getConstructors()) { // kotlin IntRange(first, last)
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 2 && p[0] == int.class && p[1] == int.class) return c.newInstance(100, 100);
            }
        } catch (Throwable ignored) {
        }
        if (unknown.size() < 5) unknown.add(t.getName());
        return null;
    }
}
