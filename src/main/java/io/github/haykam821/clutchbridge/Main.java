package io.github.haykam821.clutchbridge;

import io.github.haykam821.clutchbridge.game.ClutchBridgeConfig;
import io.github.haykam821.clutchbridge.game.phase.ClutchBridgeWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	private static final String MOD_ID = "clutchbridge";

	private static final Identifier CLUTCH_BRIDGE_ID = Main.identifier("clutch_bridge");
	public static final GameType<ClutchBridgeConfig> CLUTCH_BRIDGE_TYPE = GameType.register(CLUTCH_BRIDGE_ID, ClutchBridgeConfig.CODEC, ClutchBridgeWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}

	public static Identifier identifier(String path) {
		return new Identifier(MOD_ID, path);
	}
}