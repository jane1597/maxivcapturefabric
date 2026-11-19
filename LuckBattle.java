package com.maxivcapture;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.lang.reflect.Method;

/**
 * Lucky player (always crit, always hit, max multi-hit, guaranteed move effects, doubled held-item effects)
 * for the Pokemon of the player on side p1.
 *
 * HOW: no more ">eval" into a running battle (that froze battles on Cobblemon 1.8.4). Instead this uses the same
 * door Mega Showdown uses for its own data-pack scripts: Showdown's "actions" script object. We hand a small
 * script to Showdown's receiveScriptData("actions", ...) on the Showdown thread; every battle created afterwards
 * picks it up when it builds its BattleActions. Needs Mega Showdown (Cobbleverse has it) because that mod
 * provides receiveScriptData. Everything is reflection + try/catch: if something does not match, latest.log says
 * what and battles are left alone.
 */
public final class LuckBattle {
    private LuckBattle() {}

    // @X@ are replaced by true/false. Single line, no "//" comments (Mega Showdown flattens newlines).
    private static final String JS =
            "(function(){"
            + "var A=(typeof scripts!=='undefined'&&scripts.Scripts&&scripts.Scripts.actions)?scripts.Scripts.actions:{};"
            + "var C={crit:@CRIT@,acc:@ACC@,eff:@EFF@,multi:@MULTI@,items:@ITEMS@};"
            + "function mine(p){try{return !!(p&&p.side&&p.side.id==='p1');}catch(e){return false;}}"
            + "function base(n){var f=A[n];if(f&&f.__luck)f=f.__orig;return f;}"
            + "function patch(b){if(!C.items||!b||b.__luckItems)return;b.__luckItems=1;"
            + "var cm=b.chainModify;b.chainModify=function(n,d){try{var ef=this.effect,t=this.effectState&&this.effectState.target;"
            + "if(ef&&ef.effectType==='Item'&&mine(t)){var m=Array.isArray(n)?n[0]/(n[1]?n[1]:1):n/(d?d:1);if(m>1){n=[Math.round((1+(m-1)*2)*4096),4096];d=1;}}}catch(e){}return cm.call(this,n,d);};"
            + "var hl=b.heal;b.heal=function(dm,t,s,e){try{var ef=e?e:this.effect,tt=t?t:(this.event&&this.event.target);"
            + "if(ef&&ef.effectType==='Item'&&mine(tt)&&dm>0)dm=dm*2;}catch(e2){}return hl.call(this,dm,t,s,e);};}"
            + "var pg=base('getDamage'),pu=base('useMoveInner');"
            + "var gd=function(s,t,m,q){try{if(C.crit&&mine(s)&&m&&typeof m==='object'&&m.category!=='Status')m.willCrit=true;patch(this.battle);}catch(e){}"
            + "var f=pg?pg:Object.getPrototypeOf(this).getDamage;return f.call(this,s,t,m,q);};gd.__luck=1;gd.__orig=pg;"
            + "var um=function(){var a=Array.prototype.slice.call(arguments);try{var m=a[0],p=a[1];if(mine(p)){"
            + "var am=(m&&typeof m.hit==='number')?m:this.dex.getActiveMove(m);"
            + "if(C.acc)am.accuracy=true;"
            + "if(C.eff){if(am.secondary&&am.secondary.chance)am.secondary.chance=100;"
            + "if(am.secondaries)for(var i=0;i<am.secondaries.length;i++){if(am.secondaries[i]&&am.secondaries[i].chance)am.secondaries[i].chance=100;}"
            + "if(am.self&&am.self.chance)am.self.chance=100;}"
            + "if(am.category!=='Status'){if(C.crit)am.willCrit=true;if(C.multi&&Array.isArray(am.multihit))am.multihit=am.multihit[1];}a[0]=am;}"
            + "patch(this.battle);}catch(e){}"
            + "var f=pu?pu:Object.getPrototypeOf(this).useMoveInner;return f.apply(this,a);};um.__luck=1;um.__orig=pu;"
            + "return {getDamage:gd,useMoveInner:um};})()";

    public static String script() {
        boolean on = Config.luckyBattles();
        return JS.replace("@CRIT@", String.valueOf(on && Config.alwaysCrit()))
                .replace("@ACC@", String.valueOf(on && Config.alwaysHit()))
                .replace("@EFF@", String.valueOf(on && Config.guaranteedEffects()))
                .replace("@MULTI@", String.valueOf(on && Config.maxMultiHit()))
                .replace("@ITEMS@", String.valueOf(on && Config.doubleItemEffects()));
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> push());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, rm, ok) -> push());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> push());
    }

    private static boolean loggedOk = false;

    /** Hands the script to Showdown (also called after /maxiv set). Safe to call any number of times. */
    public static void push() {
        try {
            Class<?> cob = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object inst = cob.getField("INSTANCE").get(null);
            Object thread = inst.getClass().getMethod("getShowdownThread").invoke(inst);
            Method queue = null;
            for (Method m : thread.getClass().getMethods()) {
                if (m.getName().equals("queue") && m.getParameterCount() == 1) {
                    queue = m;
                    break;
                }
            }
            if (queue == null) {
                MaxIvCapture.LOGGER.warn("Lucky battle: Cobblemon's Showdown thread has no queue(...) here, luck not applied.");
                return;
            }
            final String js = script().replace("\n", " ");
            kotlin.jvm.functions.Function1<Object, kotlin.Unit> task = service -> {
                try {
                    Object ctx = null;
                    try {
                        ctx = service.getClass().getField("context").get(service);
                    } catch (NoSuchFieldException e) {
                        java.lang.reflect.Field f = service.getClass().getDeclaredField("context");
                        f.setAccessible(true);
                        ctx = f.get(service);
                    }
                    Class<?> ctxC = Class.forName("org.graalvm.polyglot.Context");
                    Class<?> valC = Class.forName("org.graalvm.polyglot.Value");
                    Object bindings = ctxC.getMethod("getBindings", String.class).invoke(ctx, "js");
                    Object fn = valC.getMethod("getMember", String.class).invoke(bindings, "receiveScriptData");
                    boolean isNull = fn == null || (Boolean) valC.getMethod("isNull").invoke(fn);
                    if (isNull) {
                        MaxIvCapture.LOGGER.warn("Lucky battle: Showdown has no receiveScriptData (Mega Showdown missing?), luck not applied.");
                        return kotlin.Unit.INSTANCE;
                    }
                    valC.getMethod("execute", Object[].class).invoke(fn, (Object) new Object[]{"actions", js});
                    if (!loggedOk) {
                        loggedOk = true;
                        MaxIvCapture.LOGGER.info("Lucky battle: luck script handed to Showdown (applies to battles that start from now on, player side p1).");
                    }
                } catch (Throwable t) {
                    MaxIvCapture.LOGGER.warn("Lucky battle: could not hand the script to Showdown: {}", String.valueOf(t.getCause() != null ? t.getCause() : t));
                }
                return kotlin.Unit.INSTANCE;
            };
            queue.invoke(thread, task);
        } catch (Throwable t) {
            MaxIvCapture.LOGGER.warn("Lucky battle: could not reach Cobblemon's Showdown: {}", t.toString());
        }
    }
}
