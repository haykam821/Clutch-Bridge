package io.github.haykam821.clutchbridge.game.map;

import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import xyz.nucleoid.plasmid.game.world.generator.GameChunkGenerator;

public class ClutchBridgeChunkGenerator extends GameChunkGenerator {
	private final ClutchBridgeMapConfig mapConfig;

	public ClutchBridgeChunkGenerator(MinecraftServer server, ClutchBridgeMapConfig mapConfig) {
		super(server);
		this.mapConfig = mapConfig;
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, Chunk chunk) {
		BlockStateProvider stateProvider = this.mapConfig.getStateProvider();
		Random random = region.getRandom();
		
		int startX = chunk.getPos().getStartX();
		int startZ = chunk.getPos().getStartZ();
		if (startZ < 0) return;

		BlockPos.Mutable pos = new BlockPos.Mutable(startX, 64, startZ);
		for (int x = 0; x < 16; x++) {
			if ((startX + x) % this.mapConfig.getSpacing() != 0) continue;
			pos.setX(startX + x);

			for (int z = 0; z < 16; z++) {
				pos.setZ(startZ + z);

				BlockState state = stateProvider.getBlockState(random, pos);
				region.setBlockState(pos, state, 3);
			}
		}
	}
}