package io.github.haykam821.clutchbridge.game.phase;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import io.github.haykam821.clutchbridge.game.ClutchBridgeConfig;
import io.github.haykam821.clutchbridge.game.map.ClutchBridgeMap;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ClutchBridgeActivePhase {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final ClutchBridgeConfig config;
	private final Set<ServerPlayerEntity> players;
	private boolean singleplayer;
	private int ticksUntilKnockback;
	private ItemStack clutchBlock;

	public ClutchBridgeActivePhase(GameSpace gameSpace, ServerWorld world, ClutchBridgeConfig config, Set<ServerPlayerEntity> players) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.config = config;
		this.players = players;
		this.ticksUntilKnockback = this.config.getDelay();
		this.clutchBlock = ClutchBridgeActivePhase.buildClutchBlock(this.config);
	}
	
	private static ItemStack buildClutchBlock(ClutchBridgeConfig config) {
		ItemStackBuilder builder = ItemStackBuilder.of(config.getClutchBlock())
			.addCanPlaceOn(Blocks.MAGENTA_GLAZED_TERRACOTTA)
			.setCount(64);

		if (config.getClutchBlock().getItem() instanceof BlockItem) {
			BlockItem blockItem = (BlockItem) config.getClutchBlock().getItem();
			builder.addCanPlaceOn(blockItem.getBlock());
		}

		return builder.build();
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.THROW_ITEMS);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, ClutchBridgeMap map, ClutchBridgeConfig config) {
		ClutchBridgeActivePhase phase = new ClutchBridgeActivePhase(gameSpace, world, config, Sets.newHashSet(gameSpace.getPlayers()));

		gameSpace.setActivity(activity -> {
			ClutchBridgeActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(GamePlayerEvents.REMOVE, phase::removePlayer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(BlockUseEvent.EVENT, phase::useBlock);
		});
	}

	public void enable() {
		this.singleplayer = this.players.size() == 1;

		int x = 0;
 		for (ServerPlayerEntity player : this.players) {
			player.changeGameMode(GameMode.ADVENTURE);
			this.giveClutchBlocks(player);

			ClutchBridgeActivePhase.spawn(x, this.world, player);
			x += this.config.getMapConfig().getSpacing();
		}
	}

	private boolean isBelowBridges(PlayerEntity player) {
		return player.getY() < 64;
	}

	private void checkElimination() {
		Iterator<ServerPlayerEntity> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			ServerPlayerEntity player = iterator.next();

			if (this.isBelowBridges(player)) {
				this.eliminate(player, false);
				iterator.remove();
			}
		}
	}

	private void tick() {
		this.ticksUntilKnockback -= 1;
		if (this.ticksUntilKnockback <= 0) {
			double angle = this.world.getRandom().nextDouble() * 2 * Math.PI;

			Iterator<ServerPlayerEntity> iterator = this.players.iterator();
			while (iterator.hasNext()) {
				ServerPlayerEntity player = iterator.next();

				player.playSound(SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 1, 1);

				double x = Math.sin(angle) * 3;
				double z = Math.cos(angle) * -3;

				player.takeKnockback(0.4f, x, z);
				player.velocityModified = true;
			}

			this.ticksUntilKnockback = this.config.getDelay();
		}
		this.checkElimination();


		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;
			
			Text endingMessage = this.getEndingMessage();
			for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
				player.sendMessage(endingMessage, false);
			}

			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	private Text getEndingMessage() {
		if (this.players.size() == 1) {
			PlayerEntity winner = this.players.iterator().next();
			return new TranslatableText("text.clutchbridge.win", winner.getDisplayName()).formatted(Formatting.GOLD);
		}
		return new TranslatableText("text.clutchbridge.no_winners").formatted(Formatting.GOLD);
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, ClutchBridgeActivePhase.getSpawnPos(0)).and(() -> {
			this.setSpectator(offer.player());
		});
	}

	private void removePlayer(ServerPlayerEntity player) {
		this.eliminate(player, true);
	}

	private void giveClutchBlocks(ServerPlayerEntity player) {
		for (int slot = 0; slot < 9; slot++) {
			player.getInventory().setStack(slot, this.clutchBlock.copy());
		}
	}
	
	private ActionResult useBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
		this.giveClutchBlocks(player);
		return ActionResult.PASS;
	}

	private void eliminate(ServerPlayerEntity eliminatedPlayer, boolean remove) {
		Text message = new TranslatableText("text.clutchbridge.eliminated", eliminatedPlayer.getDisplayName()).formatted(Formatting.RED);
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			player.sendMessage(message, false);
		}

		if (remove) {
			this.players.remove(eliminatedPlayer);
		}
		this.setSpectator(eliminatedPlayer);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.eliminate(player, true);
		return ActionResult.SUCCESS;
	}

	public static Vec3d getSpawnPos(int x) {
		return new Vec3d(x + 0.5, 65, 0.5);
	}

	public static void spawn(int x, ServerWorld world, ServerPlayerEntity player) {
		Vec3d spawnPos = ClutchBridgeActivePhase.getSpawnPos(x);
		player.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
	}
}