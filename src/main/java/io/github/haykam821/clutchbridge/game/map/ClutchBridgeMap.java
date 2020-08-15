package io.github.haykam821.clutchbridge.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class ClutchBridgeMap {
	private final ClutchBridgeMapConfig mapConfig;

	public ClutchBridgeMap(ClutchBridgeMapConfig mapConfig) {
		this.mapConfig = mapConfig;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new ClutchBridgeChunkGenerator(server, this.mapConfig);
	}
}