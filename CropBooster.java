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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crop growth multiplier. It keeps a list of every crop block in loaded chunks and gives each one extra
 * "random ticks" (the same thing the game uses to grow crops). Other random-tick things (grass spread,
 * leaf decay, fire ...) are NOT sped up, only crops.
 */
public class CropBooster {
    public static void register() {
        CropBooster c = new CropBooster();
        ServerChunkEvents.CHUNK_LOAD.register(c::onChunkLoad);
        ServerTickEvents.END_SERVER_TICK.register(c::onServerTick);
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClientSide && player.getItemInHand(hand).getItem() instanceof BlockItem bi
                    && isCrop(bi.getBlock().defaultBlockState()) && world instanceof ServerLevel sl) {
                BlockPos p = hit.getBlockPos();
                c.setFor(sl.dimension()).add(p.asLong());
                c.setFor(sl.dimension()).add(p.relative(hit.getDirection()).asLong());
            }
            return InteractionResult.PASS;
        });
    }

    private static final int STEP = 2; // run every 2 ticks

    private final Map<ResourceKey<Level>, Set<Long>> tracked = new ConcurrentHashMap<>();
    private int tick = 0;

    private static boolean isCrop(BlockState state) {
        Block b = state.getBlock();
        if (b instanceof CropBlock || b instanceof StemBlock || b instanceof SaplingBlock
                || b instanceof SugarCaneBlock || b instanceof BambooStalkBlock || b instanceof NetherWartBlock
                || b instanceof CactusBlock || b instanceof SweetBerryBushBlock || b instanceof CocoaBlock
                || b instanceof ChorusFlowerBlock || b instanceof GrowingPlantHeadBlock
                || b instanceof PitcherCropBlock) {
            return true;
        }
        // Cobblemon crops (berries, apricorns, mints ...) and other Cobblemon random-tick blocks
        return state.isRandomlyTicking()
                && "cobblemon".equals(BuiltInRegistries.BLOCK.getKey(b).getNamespace());
    }

    private Set<Long> setFor(ResourceKey<Level> dim) {
        return tracked.computeIfAbsent(dim, k -> ConcurrentHashMap.newKeySet());
    }

    private void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        try {
            Set<Long> set = setFor(level.dimension());
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
                                set.add(BlockPos.asLong(baseX + x, baseY + y, baseZ + z));
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            MaxIvCapture.LOGGER.warn("Crop scan failed for a chunk: {}", ex.toString());
        }
    }

    private void onServerTick(MinecraftServer server) {
        if (++tick % STEP != 0) return;
        int mult = Config.cropGrowth();
        if (mult <= 1) return;

        for (ServerLevel level : server.getAllLevels()) {
            Set<Long> set = tracked.get(level.dimension());
            if (set == null || set.isEmpty()) continue;

            int rts = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            // chance that a block gets one natural random tick in STEP ticks is about rts/4096 per tick
            double p = Math.min(1.0, (mult - 1) * (rts / 4096.0) * STEP);
            if (p <= 0) continue;

            Iterator<Long> it = set.iterator();
            while (it.hasNext()) {
                long l = it.next();
                if (level.random.nextDouble() >= p) continue;
                BlockPos pos = BlockPos.of(l);
                if (!level.isLoaded(pos)) {
                    it.remove();
                    continue;
                }
                if (!level.isPositionEntityTicking(pos)) continue;
                BlockState st = level.getBlockState(pos);
                if (!isCrop(st)) {
                    it.remove();
                    continue;
                }
                try {
                    st.randomTick(level, pos, level.random);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
