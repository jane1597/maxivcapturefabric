package com.maxivcapture;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Poke Ball buff: every ball's catch rate is multiplied (Config.ballMultiplier, default 2.0).
 * Poke Ball 1x -> 2x, Great Ball 1.5x -> 3x, Ultra Ball 2x -> 4x ... Master Ball stays a guaranteed catch.
 *
 * How: each ball has a "catch rate modifier" object. We wrap it in a proxy that calls the original and multiplies
 * the number it returns from modifyCatchRate(...). It is all reflection + try/catch, so a Cobblemon change just
 * logs a warning and leaves balls alone. Applies to every ball found (Cobblemon's own and add-on balls).
 */
public final class BallBuff {
    private BallBuff() {}

    /** Wrapper handler; the multiplier is read live from Config, so /maxiv ball takes effect at once. */
    private static final class Handler implements InvocationHandler {
        final Object delegate;
        Handler(Object delegate) { this.delegate = delegate; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result;
            try {
                result = method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            if (method.getName().equals("modifyCatchRate") && result instanceof Float f) {
                float m = (float) Config.ballMultiplier();
                return f * m;
            }
            return result;
        }
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> apply());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> apply());
    }

    private static boolean logged = false;

    public static void apply() {
        try {
            Class<?> ballsC = Class.forName("com.cobblemon.mod.common.api.pokeball.PokeBalls");
            Object inst = ballsC.getField("INSTANCE").get(null);
            List<Object> balls = new ArrayList<>();
            Map<Object, Boolean> seen = new IdentityHashMap<>();
            for (Method m : ballsC.getMethods()) {
                if (m.getParameterCount() != 0 || Modifier.isStatic(m.getModifiers())) continue;
                Object r;
                try {
                    r = m.invoke(inst);
                } catch (Throwable t) {
                    continue;
                }
                Iterable<?> it = r instanceof Map<?, ?> mp ? mp.values() : r instanceof Iterable<?> i ? i : null;
                if (it == null) continue;
                for (Object o : it) {
                    if (o != null && o.getClass().getSimpleName().equals("PokeBall") && seen.put(o, true) == null) balls.add(o);
                }
            }
            int wrapped = 0;
            for (Object ball : balls) {
                if (wrap(ball)) wrapped++;
            }
            if (!logged) {
                logged = true;
                if (wrapped == 0) MaxIvCapture.LOGGER.warn("Ball buff: found {} balls but could not wrap any catch rate modifier (Cobblemon changed?). Balls left alone.", balls.size());
                else MaxIvCapture.LOGGER.info("Ball buff: catch rate x{} on {} Poke Balls.", Config.ballMultiplier(), wrapped);
            }
        } catch (Throwable t) {
            if (!logged) {
                logged = true;
                MaxIvCapture.LOGGER.warn("Ball buff: could not reach Cobblemon's balls: {}", t.toString());
            }
        }
    }

    private static boolean wrap(Object ball) {
        for (Class<?> c = ball.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || !f.getType().isInterface()) continue;
                if (!hasModify(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object cur = f.get(ball);
                    if (cur == null) continue;
                    if (Proxy.isProxyClass(cur.getClass()) && Proxy.getInvocationHandler(cur) instanceof Handler) return true; // already wrapped
                    Object proxy = Proxy.newProxyInstance(f.getType().getClassLoader(), new Class<?>[]{f.getType()}, new Handler(cur));
                    f.set(ball, proxy);
                    return true;
                } catch (Throwable t) {
                    // final field refused? try the next one
                }
            }
        }
        return false;
    }

    private static boolean hasModify(Class<?> type) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals("modifyCatchRate")) return true;
        }
        return false;
    }
}
