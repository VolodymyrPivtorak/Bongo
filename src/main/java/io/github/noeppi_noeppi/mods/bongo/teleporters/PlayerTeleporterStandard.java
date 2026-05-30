package io.github.noeppi_noeppi.mods.bongo.teleporters;

import io.github.noeppi_noeppi.mods.bongo.Bongo;
import io.github.noeppi_noeppi.mods.bongo.data.Team;
import io.github.noeppi_noeppi.mods.bongo.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Random;

public class PlayerTeleporterStandard implements PlayerTeleporter {

    public static final PlayerTeleporterStandard INSTANCE = new PlayerTeleporterStandard();
    private static final int RANDOM_ATTEMPTS = 4;
    private static final int LOCAL_SEARCH_RADIUS = 4;
    
    private PlayerTeleporterStandard() {
        
    }

    @Override
    public String id() {
        return "bongo.standard";
    }

    @Override
    public void teleportTeam(Bongo bongo, ServerLevel gameLevel, Team team, List<ServerPlayer> players, BlockPos center, int radius, Random random) {
        BlockPos pos = null;
        int attempts = Math.max(1, radius > 0 ? RANDOM_ATTEMPTS : 1);
        for (int i = 0; i < attempts; i++) {
            int x = radius > 0 ? center.getX() + random.nextInt(2 * radius) - radius : center.getX();
            int z = radius > 0 ? center.getZ() + random.nextInt(2 * radius) - radius : center.getZ();
            pos = findSpawnInColumn(gameLevel, x, z);
            if (Util.validSpawn(gameLevel, pos)) {
                break;
            }
            BlockPos nearby = findNearbySpawn(gameLevel, x, z);
            if (nearby != null) {
                pos = nearby;
                break;
            }
        }
        // After a few tries we give up and use the last position. This keeps game start responsive
        // instead of generating many far-away chunks while looking for a perfect spawn.
        BlockPos effectiveFinalPos = pos;
        gameLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(effectiveFinalPos), 1, team.hashCode());
        players.forEach(player -> {
            player.teleportTo(gameLevel, effectiveFinalPos.getX() + 0.5, effectiveFinalPos.getY(), effectiveFinalPos.getZ() + 0.5, player.getYHeadRot(), 0);
            player.setRespawnPosition(gameLevel.dimension(), effectiveFinalPos, 0, true, false);
        });
    }

    private static BlockPos findSpawnInColumn(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        y = Math.max(level.getMinBuildHeight() + 1, Math.min(y, level.getMaxBuildHeight() - 2));
        return new BlockPos(x, y, z);
    }

    private static BlockPos findNearbySpawn(ServerLevel level, int x, int z) {
        for (int offset = 1; offset <= LOCAL_SEARCH_RADIUS; offset++) {
            for (int dx = -offset; dx <= offset; dx++) {
                for (int dz = -offset; dz <= offset; dz++) {
                    if (Math.abs(dx) != offset && Math.abs(dz) != offset) {
                        continue;
                    }
                    BlockPos pos = findSpawnInColumn(level, x + dx, z + dz);
                    if (Util.validSpawn(level, pos)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}
