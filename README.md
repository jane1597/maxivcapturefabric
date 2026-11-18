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
