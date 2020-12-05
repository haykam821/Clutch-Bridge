package io.github.haykam821.clutchbridge.game.phase;

import io.github.haykam821.clutchbridge.game.ClutchBridgeConfig;
import io.github.haykam821.clutchbridge.game.map.ClutchBridgeMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;

public class ClutchBridgeWaitingPhase {
	private final GameSpace gameSpace;
	private final ClutchBridgeMap map;
	private final ClutchBridgeConfig config;

	public ClutchBridgeWaitingPhase(GameSpace gameSpace, ClutchBridgeMap map, ClutchBridgeConfig config) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<ClutchBridgeConfig> context) {
		ClutchBridgeMap map = new ClutchBridgeMap(context.getConfig().getMapConfig());

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(map.createGenerator(context.getServer()))
			.setDefaultGameMode(GameMode.ADVENTURE);

		return context.createOpenProcedure(worldConfig, game -> {
			ClutchBridgeWaitingPhase phase = new ClutchBridgeWaitingPhase(game.getSpace(), map, context.getConfig());

			GameWaitingLobby.applyTo(game, context.getConfig().getPlayerConfig());
			ClutchBridgeActivePhase.setRules(game);

			// Listeners
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
			game.on(OfferPlayerListener.EVENT, phase::offerPlayer);
			game.on(RequestStartListener.EVENT, phase::requestStart);
		});
	}

	private boolean isFull() {
		return this.gameSpace.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	public JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	public StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameSpace.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.NOT_ENOUGH_PLAYERS;
		}

		ClutchBridgeActivePhase.open(this.gameSpace, this.map, this.config);
		return StartResult.OK;
	}

	public void addPlayer(ServerPlayerEntity player) {
		ClutchBridgeActivePhase.spawn(0, this.gameSpace.getWorld(), player);
	}

	public ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		ClutchBridgeActivePhase.spawn(0, this.gameSpace.getWorld(), player);
		return ActionResult.SUCCESS;
	}
}