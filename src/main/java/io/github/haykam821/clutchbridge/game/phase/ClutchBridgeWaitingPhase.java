package io.github.haykam821.clutchbridge.game.phase;

import io.github.haykam821.clutchbridge.game.ClutchBridgeConfig;
import io.github.haykam821.clutchbridge.game.map.ClutchBridgeMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ClutchBridgeWaitingPhase {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final ClutchBridgeMap map;
	private final ClutchBridgeConfig config;

	public ClutchBridgeWaitingPhase(GameSpace gameSpace, ServerWorld world, ClutchBridgeMap map, ClutchBridgeConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<ClutchBridgeConfig> context) {
		ClutchBridgeMap map = new ClutchBridgeMap(context.config().getMapConfig());

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithWorld(worldConfig, (activity, world) -> {
			ClutchBridgeWaitingPhase phase = new ClutchBridgeWaitingPhase(activity.getGameSpace(), world, map, context.config());

			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());
			ClutchBridgeActivePhase.setRules(activity);

			// Listeners
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
		});
	}

	public GameResult requestStart() {
		ClutchBridgeActivePhase.open(this.gameSpace, this.world, this.map, this.config);
		return GameResult.ok();
	}

	public PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, ClutchBridgeActivePhase.getSpawnPos(0)).and(() -> {
			offer.player().changeGameMode(GameMode.ADVENTURE);
		});
	}

	public ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		ClutchBridgeActivePhase.spawn(0, this.world, player);
		return ActionResult.SUCCESS;
	}
}