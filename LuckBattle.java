package com.maxivcapture;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import kotlin.Unit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Lucky player: when a battle starts, the mod sends a small script to Cobblemon's battle engine (Pokemon Showdown)
 * so that every damaging move used by a PLAYER's Pokemon always crits and multi-hit moves (Water Shuriken,
 * Bullet Seed, Fury Attack ...) always hit their maximum number of times. Wild and NPC Pokemon are not changed.
 *
 * Everything is done with reflection and try/catch so the mod can never crash a battle; if something does not
 * match, a line in latest.log says what.
 */
public final class LuckBattle {
    private LuckBattle() {}

    public static void register() {
        CobblemonEvents.BATTLE_STARTED_POST.subscribe(Priority.NORMAL, event -> {
            try {
                if (Config.alwaysCrit() || Config.maxMultiHit() || Config.alwaysHit()) {
                    Object battle = event.getClass().getMethod("getBattle").invoke(event);
                    apply(battle);
                }
            } catch (Throwable t) {
                MaxIvCapture.LOGGER.warn("Lucky battle: could not start: {}", t.toString());
            }
            return Unit.INSTANCE;
        });
    }

    private static void apply(Object battle) throws Exception {
        Object actors = battle.getClass().getMethod("getActors").invoke(battle);
        List<String> ids = new ArrayList<>();
        for (Object actor : (Iterable<?>) actors) {
            if (actor.getClass().getSimpleName().contains("Player")) {
                Object id = actor.getClass().getMethod("getShowdownId").invoke(actor);
                if (id != null) ids.add("\"" + id + "\"");
            }
        }
        if (ids.isEmpty()) return; // wild vs wild / npc vs npc: nothing to do

        String script = "(function(){var b=battle,a=b.actions;var IDS=[" + String.join(",", ids) + "];"
                + "a.__luckIds=IDS;if(a.__luckInstalled)return;a.__luckInstalled=1;"
                + "var ACC=" + Config.alwaysHit() + ",CRIT=" + Config.alwaysCrit() + ",MULTI=" + Config.maxMultiHit() + ";"
                + "var o=a.useMoveInner;"
                + "a.useMoveInner=function(m,p,op){try{if(p&&p.side&&this.__luckIds.indexOf(p.side.id)>=0){"
                + "var am=(m&&typeof m.hit==='number')?m:this.dex.getActiveMove(m);"
                + "if(ACC)am.accuracy=true;if(am.category!=='Status'){if(CRIT)am.willCrit=true;"
                + "if(MULTI&&Array.isArray(am.multihit))am.multihit=am.multihit[1];}m=am;}}catch(e){}"
                + "return o.call(this,m,p,op);};})()";

        Method send = null;
        for (Method m : battle.getClass().getMethods()) {
            if (m.getName().equals("writeShowdownAction") && m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == String[].class) {
                send = m;
                break;
            }
        }
        if (send == null) {
            MaxIvCapture.LOGGER.warn("Lucky battle: Cobblemon has no writeShowdownAction(String...) here, luck not applied.");
            return;
        }
        send.invoke(battle, (Object) new String[]{">eval " + script});
        MaxIvCapture.LOGGER.info("Lucky battle: luck sent to the battle engine for player side(s) {}", ids);
    }
}
