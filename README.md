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
