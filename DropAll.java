package com.maxivcapture;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Makes every Pokemon drop everything in its drop table, every time.
 *
 * Cobblemon rolls a Pokemon's drop table like this: it aims for "amount" drops and rolls each entry's
 * "percentage". We set "amount" to the number of entries and every percentage to 100, so everything drops.
 * This is done with reflection so the mod keeps compiling and loading even if Cobblemon renames something;
 * if that happens the log says what was found instead of crashing.
 */
public class DropAll {
    public static void register() {
        DropAll d = new DropAll();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> d.apply());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, rm, ok) -> d.apply());
    }

    private final Set<Object> done = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean warned = false;

    private void apply() {
        if (!Config.allDrops()) return;
        int changed = 0;
        int species = 0;
        try {
            Class<?> reg = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies");
            Object inst = reg.getField("INSTANCE").get(null);
            Object all = reg.getMethod("getSpecies").invoke(inst);
            for (Object sp : (Collection<?>) all) {
                species++;
                Object table = get(sp, "drops");
                if (table == null || done.contains(table)) continue;
                Object entries = get(table, "entries");
                if (!(entries instanceof List<?> list)) {
                    warnOnce(table, "entries");
                    continue;
                }
                if (list.isEmpty()) {
                    done.add(table);
                    continue;
                }
                boolean ok = setAmount(table, list.size());
                for (Object entry : list) {
                    ok &= setPercentage(entry);
                }
                if (ok) {
                    done.add(table);
                    changed++;
                } else {
                    warnOnce(table, "amount/percentage");
                }
            }
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                MaxIvCapture.LOGGER.warn("Guaranteed drops: could not reach Cobblemon's species list: {}", t.toString());
            }
            return;
        }
        if (changed > 0) {
            MaxIvCapture.LOGGER.info("Guaranteed drops: {} of {} Pokemon species now drop everything.", changed, species);
        }
    }

    // ---------- reflection helpers ----------

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static Object get(Object o, String name) {
        try {
            String m = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            Method g = o.getClass().getMethod(m);
            return g.invoke(o);
        } catch (Throwable ignored) {
        }
        try {
            Field f = findField(o.getClass(), name);
            return f == null ? null : f.get(o);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean setAmount(Object table, int n) {
        try {
            Field f = findField(table.getClass(), "amount");
            if (f == null) return false;
            Class<?> t = f.getType();
            if (t == int.class) {
                f.setInt(table, n);
                return true;
            }
            if (t == Integer.class) {
                f.set(table, n);
                return true;
            }
            // kotlin.ranges.IntRange(first, last)
            Constructor<?> c = t.getConstructor(int.class, int.class);
            f.set(table, c.newInstance(n, n));
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    private static boolean setPercentage(Object entry) {
        try {
            Field f = findField(entry.getClass(), "percentage");
            if (f == null) return true; // an entry type without a percentage always drops
            Class<?> t = f.getType();
            if (t == double.class) f.setDouble(entry, 100.0);
            else if (t == float.class) f.setFloat(entry, 100.0f);
            else if (t == Double.class) f.set(entry, 100.0);
            else if (t == Float.class) f.set(entry, 100.0f);
            else if (t == int.class) f.setInt(entry, 100);
            else return false;
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    private void warnOnce(Object table, String what) {
        if (warned) return;
        warned = true;
        StringBuilder sb = new StringBuilder();
        for (Class<?> k = table.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            sb.append(k.getName()).append(" fields:");
            for (Field f : k.getDeclaredFields()) sb.append(' ').append(f.getName()).append(':').append(f.getType().getSimpleName());
            sb.append(" | ");
        }
        MaxIvCapture.LOGGER.warn("Guaranteed drops: could not change {}. Send this line to the mod author: {}", what, sb);
    }
}
