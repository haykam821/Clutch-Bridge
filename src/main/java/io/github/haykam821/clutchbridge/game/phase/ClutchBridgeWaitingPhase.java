package io.github.haykam821.clutchbridge.game.phase;

import java.util.concurrent.CompletableFuture;

import io.github.haykam821.clutchbridge.game.ClutchBridgeConfig;
import io.github.haykam821.clutchbridge.game.map.ClutchBridgeMap;
import io.github.haykam821.clutchbridge.game.map.ClutchBridgeMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.world.bubble.BubbleWorldConfig;

public class ClutchBridgeWaitingPhase {
	private final GameWorld gameWorld;
	private final ClutchBridgeMap map;
	private final ClutchBridgeConfig config;

	public ClutchBridgeWaitingPhase(GameWorld gameWorld, ClutchBridgeMap map, ClutchBridgeConfig config) {
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
	}

	public static CompletableFuture<Void> open(GameOpenContext<ClutchBridgeConfig> context) {
		ClutchBridgeMapBuilder mapBuilder = new ClutchBridgeMapBuilder(context.getConfig());

		return mapBuilder.create().thenAccept(map -> {
			BubbleWorldConfig worldConfig = new BubbleWorldConfig()
				.setGenerator(map.createGenerator(context.getServer()))
				.setDefaultGameMode(GameMode.ADVENTURE);
			GameWorld gameWorld = context.openWorld(worldConfig);

			ClutchBridgeWaitingPhase phase = new ClutchBridgeWaitingPhase(gameWorld, map, context.getConfig());

			gameWorld.openGame(game -> {
				ClutchBridgeActivePhase.setRules(game);

				// Listeners
				game.on(PlayerAddListener.EVENT, phase::addPlayer);
				game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
				game.on(OfferPlayerListener.EVENT, phase::offerPlayer);
				game.on(RequestStartListener.EVENT, phase::requestStart);
			});
		});
	}

	private boolean isFull() {
		return this.gameWorld.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	public JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	public StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameWorld.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.notEnoughPlayers();
		}

		ClutchBridgeActivePhase.open(this.gameWorld, this.map, this.config);
		return StartResult.ok();
	}

	public void addPlayer(ServerPlayerEntity player) {
		ClutchBridgeActivePhase.spawn(0, this.gameWorld.getWorld(), player);
	}

	public ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		ClutchBridgeActivePhase.spawn(0, this.gameWorld.getWorld(), player);
		return ActionResult.SUCCESS;
	}
}