package io.github.haykam821.clutchbridge.game.map;

import java.util.concurrent.CompletableFuture;

import io.github.haykam821.clutchbridge.game.ClutchBridgeConfig;
import net.minecraft.util.Util;

public class ClutchBridgeMapBuilder {
	private final ClutchBridgeConfig config;

	public ClutchBridgeMapBuilder(ClutchBridgeConfig config) {
		this.config = config;
	}

	public CompletableFuture<ClutchBridgeMap> create() {
		return CompletableFuture.supplyAsync(() -> {
			ClutchBridgeMapConfig mapConfig = this.config.getMapConfig();
			return new ClutchBridgeMap(mapConfig);
		}, Util.getMainWorkerExecutor());
	}
}