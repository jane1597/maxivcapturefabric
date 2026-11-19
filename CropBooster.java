package com.maxivcapture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crop growth multiplier with EVEN growth.
 *
 * Every crop block in a loaded chunk is tracked. Each crop has its own counter that fills at a fixed speed and
 * then gets one extra "random tick" (or one bone-meal step for Cobblemon berry trees, which grow on a timer and do
 * not random tick). No dice are rolled to decide WHICH crop gets a boost, so no crop is left behind.
 * Chunks are re-scanned regularly, so crops that were replanted by right-click harvesting, villagers, dispensers
 * or other mods are picked up too (the old version dropped a crop forever once it was harvested and replanted
 * by something other than a player placing a block).
 * Other random-tick things (grass spread, leaf decay, fire ...) are NOT sped up, only crops.
 */
public class CropBooster {
    public static void register() {
        CropBooster c = new CropBooster();
        ServerChunkEvents.CHUNK_LOAD.register(c::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(c::onChunkUnload);
        ServerTickEvents.END_SERVER_TICK.register(c::onServerTick);
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClientSide && player.getItemInHand(hand).getItem() instanceof BlockItem bi
                    && isCrop(bi.getBlock().defaultBlockState()) && world instanceof ServerLevel sl) {
                BlockPos p = hit.getBlockPos();
                c.track(sl.dimension(), p);
                c.track(sl.dimension(), p.relative(hit.getDirection()));
            }
            return InteractionResult.PASS;
        });
    }

    private static final int STEP = 2;            // boost runs every 2 ticks
    private static final int RESCAN_EVERY = 20;   // every second, re-scan a few chunks (round robin)
    private static final int RESCAN_CHUNKS = 6;

    /** crop position -> counter (random-tick units, or stage units for Cobblemon timer plants). */
    private static final class Crop {
        double acc;
    }

    private final Map<ResourceKey<Level>, Map<Long, Map<Long, Crop>>> tracked = new ConcurrentHashMap<>(); // dim -> chunk -> pos -> crop
    private int tick = 0;
    private int rescanCursor = 0;

    private static boolean isCrop(BlockState state) {
        Block b = state.getBlock();
        if (b instanceof CropBlock || b instanceof StemBlock || b instanceof SaplingBlock
                || b instanceof SugarCaneBlock || b instanceof BambooStalkBlock || b instanceof NetherWartBlock
                || b instanceof CactusBlock || b instanceof SweetBerryBushBlock || b instanceof CocoaBlock
                || b instanceof ChorusFlowerBlock || b instanceof GrowingPlantHeadBlock
                || b instanceof PitcherCropBlock) {
            return true;
        }
        return isCobblemonPlant(state);
    }

    /** Berry trees do not random tick (they grow on a timer in their block entity): anything bone-mealable counts. */
    private static boolean isCobblemonPlant(BlockState state) {
        Block b = state.getBlock();
        if (!"cobblemon".equals(BuiltInRegistries.BLOCK.getKey(b).getNamespace())) return false;
        return b instanceof BonemealableBlock || state.isRandomlyTicking();
    }

    private Map<Long, Map<Long, Crop>> chunksFor(ResourceKey<Level> dim) {
        return tracked.computeIfAbsent(dim, k -> new ConcurrentHashMap<>());
    }

    private void track(ResourceKey<Level> dim, BlockPos pos) {
        long ck = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        chunksFor(dim).computeIfAbsent(ck, k -> new ConcurrentHashMap<>()).putIfAbsent(pos.asLong(), new Crop());
    }

    private void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        chunksFor(level.dimension()).remove(chunk.getPos().toLong());
    }

    private void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        scan(level, chunk);
    }

    /** Rebuilds the crop list of one chunk. Counters of crops that are still there are kept. */
    private void scan(ServerLevel level, LevelChunk chunk) {
        try {
            Map<Long, Crop> old = chunksFor(level.dimension()).get(chunk.getPos().toLong());
            Map<Long, Crop> fresh = new ConcurrentHashMap<>();
            LevelChunkSection[] sections = chunk.getSections();
            int minSection = chunk.getMinSection();
            int baseX = chunk.getPos().getMinBlockX();
            int baseZ = chunk.getPos().getMinBlockZ();
            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection section = sections[i];
                if (section == null || section.hasOnlyAir()) continue;
                if (!section.maybeHas(CropBooster::isCrop)) continue;
                int baseY = (minSection + i) << 4;
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            if (isCrop(section.getBlockState(x, y, z))) {
                                long key = BlockPos.asLong(baseX + x, baseY + y, baseZ + z);
                                Crop c = old == null ? null : old.get(key);
                                fresh.put(key, c != null ? c : new Crop());
                            }
                        }
                    }
                }
            }
            if (fresh.isEmpty()) chunksFor(level.dimension()).remove(chunk.getPos().toLong());
            else chunksFor(level.dimension()).put(chunk.getPos().toLong(), fresh);
        } catch (Exception ex) {
            MaxIvCapture.LOGGER.warn("Crop scan failed for a chunk: {}", ex.toString());
        }
    }

    private void rescan(ServerLevel level) {
        // chunks that have crops are re-checked, plus a rolling window of every other loaded chunk near players
        // so brand new crops (right-click replant, villagers, dispensers, other mods) are found quickly.
        java.util.List<net.minecraft.server.level.ServerPlayer> players = level.players();
        if (players.isEmpty()) return;
        for (int n = 0; n < RESCAN_CHUNKS; n++) {
            net.minecraft.server.level.ServerPlayer p = players.get((rescanCursor + n) % players.size());
            int r = 6;
            int span = 2 * r + 1;
            int idx = (rescanCursor * RESCAN_CHUNKS + n) % (span * span);
            int cx = (p.getBlockX() >> 4) + (idx % span) - r;
            int cz = (p.getBlockZ() >> 4) + (idx / span) - r;
            if (level.hasChunk(cx, cz)) {
                scan(level, level.getChunk(cx, cz));
            }
        }
        rescanCursor++;
    }

    private void onServerTick(MinecraftServer server) {
        tick++;
        int mult = Config.cropGrowth();
        if (mult <= 1) return;
        if (tick % RESCAN_EVERY == 0) {
            for (ServerLevel level : server.getAllLevels()) rescan(level);
        }
        if (tick % STEP != 0) return;

        double stageUnits = (mult - 1) * (double) STEP / (Config.cobblemonStageSeconds() * 20.0);

        for (ServerLevel level : server.getAllLevels()) {
            Map<Long, Map<Long, Crop>> chunks = tracked.get(level.dimension());
            if (chunks == null || chunks.isEmpty()) continue;

            int rts = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            // natural rate: one random tick per block about every 4096/rts ticks, so this is the EXTRA ticks per step
            double tickUnits = (mult - 1) * (rts / 4096.0) * STEP;

            for (Map<Long, Crop> chunk : chunks.values()) {
                for (Map.Entry<Long, Crop> e : chunk.entrySet()) {
                    BlockPos pos = BlockPos.of(e.getKey());
                    Crop crop = e.getValue();
                    if (!level.isLoaded(pos) || !level.isPositionEntityTicking(pos)) continue;
                    BlockState st = level.getBlockState(pos);
                    if (!isCrop(st)) continue;           // harvested for now; the re-scan drops it or keeps it if replanted
                    try {
                        boolean cobble = isCobblemonPlant(st) && st.getBlock() instanceof BonemealableBlock;
                        crop.acc += cobble ? stageUnits : tickUnits;
                        int guard = 0;
                        while (crop.acc >= 1.0 && guard++ < 8) {
                            crop.acc -= 1.0;
                            if (cobble) {
                                BonemealableBlock bm = (BonemealableBlock) st.getBlock();
                                if (bm.isValidBonemealTarget(level, pos, st)) {
                                    bm.performBonemeal(level, level.random, pos, st);
                                } else {
                                    crop.acc = 0;        // fully grown: nothing to bank
                                }
                            } else {
                                st.randomTick(level, pos, level.random);
                            }
                            st = level.getBlockState(pos);
                            if (!isCrop(st)) break;
                        }
                        if (crop.acc > 8) crop.acc = 8;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}
