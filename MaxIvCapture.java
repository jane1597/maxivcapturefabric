package com.maxivcapture;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Every Pokemon the player catches gets 31 in every IV. */
public class MaxIvCapture implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("maxivcapture");

    private static final Stat[] ALL = {
            Stats.HP, Stats.ATTACK, Stats.DEFENCE,
            Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED
    };

    @Override
    public void onInitialize() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            try {
                Pokemon pokemon = event.getPokemon();
                for (Stat stat : ALL) {
                    pokemon.setIV(stat, 31);
                }
            } catch (Throwable t) {
                LOGGER.error("Could not set max IVs", t);
            }
            return Unit.INSTANCE;
        });
        LOGGER.info("Max IV Capture loaded: caught Pokemon get 31 IVs in every stat.");
    }
}
