# Max IV Capture (Fabric 1.21.1, for Cobblemon / Cobbleverse)

Every Pokemon you catch with a Poke Ball gets 31 IVs in all six stats.
Wild Pokemon you did not catch, trades and starters are not changed.
Put the jar in `mods`. Needs Cobblemon and Fabric API (Cobbleverse has both).

## v1.1.0
Config file `config/maxivcapture.properties`: `cropGrowthMultiplier` (1-100) and `guaranteedPokemonDrops` (true/false).

## v1.1.1 - lucky battles
`alwaysCrit` and `maxMultiHit` in config/maxivcapture.properties (both true by default). Look for "Lucky battle:" in latest.log.

## v1.1.2
`alwaysHit` (player Pokemon moves never miss) and `maxRideStats` (all ride stats 100). Both true by default. Look for "Max ride stats:" and "Lucky battle:" in latest.log.

## v1.1.3
`guaranteedEffects` (true by default): extra effects of moves used by a player's Pokemon always happen (Bite flinch, burn chance, stat drops, etc.).

## v1.1.4
`doubleItemEffects` (true by default): buff effects of held items of a player's Pokemon are doubled (damage/speed/defense boosts and item healing).

## v1.1.5
`pastureDropsPerHour` (default 360 = a drop every 10 s per pastured Pokemon; 0 = off). Sets `drops.per_hour` in config/pastureloot.toml (Pasture Loot mod) and reloads it. Pasture drops are 100% because guaranteedPokemonDrops already makes every drop table 100%.

## v1.1.6
`luckyBattles` master switch (default false). When false the mod never touches the battle engine (alwaysCrit, maxMultiHit, alwaysHit, guaranteedEffects, doubleItemEffects do nothing). Added because battles stalled after picking a move on the updated Cobbleverse.

## v1.1.7
Everything is on again (`luckyBattles=true`). The battle luck is now split into two small independent scripts (held-item doubling, and crit/hit/effects/multi-hit). New in-game command `/maxiv status` and `/maxiv set <option> <true|false>` to switch any option live (battle options apply from the next battle). If a battle ever stalls, turn options off one at a time to find which one clashes with another mod.

## v1.1.8
Always-crit now also hooks Showdown's getDamage (in addition to useMoveInner), because another mod (Mega Showdown) can replace the first hook.

## v1.1.9
Fix: version 1.1.6 saved `luckyBattles=false` into config/maxivcapture.properties, and later versions kept reading that, so all battle luck stayed off. Old configs are now reset to on once (`configVersion=2`).

## v1.1.10
Battle luck froze battles at "Turn 0" on Cobblemon 1.8.4, so `luckyBattles` is OFF by default again (configVersion 3 resets it once). If you want to try it: `/maxiv set luckyBattles true`; it now sends one script `luckDelayTicks` (default 100 = 5 s) AFTER the battle starts instead of during the start. EXPERIMENTAL: if a battle freezes, turn it off again.

## v1.1.11
Battle luck rebuilt. It no longer sends `>eval` into a running battle (that froze battles at "Turn 0" on Cobblemon 1.8.4). It now gives Showdown a small script through `receiveScriptData("actions", ...)`, the same door Mega Showdown's own data-pack scripts use. It is pushed on server start, data-pack reload, player join and every `/maxiv set`, and applies to battles that start afterwards (player side p1). Needs Mega Showdown (Cobbleverse has it). `luckDelayTicks` is no longer used. Also fixes useMoveInner dropping the zMove/maxMove arguments.

## v1.1.12
Crop growth now works for Cobblemon plants. Berry trees do not random tick (they grow on a timer), so the old booster ignored them. Cobblemon plants are now tracked if they can be bone-mealed, and get extra bone-meal growth steps scaled by `cropGrowthMultiplier` (`cobblemonStageSeconds`, default 300 = about how long one stage takes unboosted; lower it to grow faster).

## v1.1.13
Even crop growth. Each crop now has its own counter and gets its boost on a fixed schedule instead of a dice roll, so no crop falls behind. Fixed the real cause of crops that stopped growing: a crop was dropped from the list when it was harvested and was only re-added if a player placed it, so crops replanted by right-click harvesting, villagers, dispensers or other mods were never boosted again. Chunks are now re-scanned every second (a few at a time around each player).

## v1.1.14
Chance-based held items are now guaranteed for your Pokemon (needs luckyBattles + doubleItemEffects): Quick Claw always activates, Focus Band always saves you, and King's Rock / Razor Fang flinch always happens (item-added flinch is now covered by guaranteedEffects).

## v1.1.15
Poke Ball buff: every ball's catch rate is multiplied by `ballMultiplier` (default 2.0; Poke Ball 1x -> 2x, Great 1.5x -> 3x, ...). Master Ball stays guaranteed. Change live with `/maxiv ball <1-20>`.

## v1.1.16
Exp. Share for the whole party: when one of your Pokemon earns battle exp, every other healthy Pokemon in your party gets the same exp (`expShare`, `expShareRatio` 1.0 = full, 0.5 = half). Toggle: `/maxiv set expShare false`.

## v1.1.17
Exp. Share fixed: now uses Cobblemon's EXPERIENCE_GAINED_EVENT_PRE (the event other mods use) instead of the POST one, which did not fire for the share. Default share is 50% (winner 500 exp -> every other healthy party member 250). `expShareRatio` in the config changes it (1.0 = full); old configs are reset to 0.5 once.

## v1.1.18
From a real latest.log: Exp. Share failed to hook (reflection 'argument type mismatch') -> now a normal typed subscription to EXPERIENCE_GAINED_EVENT_PRE. Ball buff found 0 balls -> balls are now looked up with PokeBalls.getPokeBall(id) for every *_ball item; if wrapping still fails the log prints the ball's fields.

## v1.1.19
Config file `config/maxivcapture.properties` is now a documented file: grouped, every option explained in a comment, created on first start. `/maxiv reload` re-reads it without restarting.
