package io.github.haykam821.clutchbridge.game.phase;

import java.util.Iterator;
import java.util.Set;

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
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.Game;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.UseBlockListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public class ClutchBridgeActivePhase {
	private final ServerWorld world;
	private final GameWorld gameWorld;
	private final ClutchBridgeConfig config;
	private final Set<ServerPlayerEntity> players;
	private boolean singleplayer;
	private int ticksUntilKnockback;
	private boolean opened;
	private ItemStack clutchBlock;

	public ClutchBridgeActivePhase(GameWorld gameWorld, ClutchBridgeConfig config, Set<ServerPlayerEntity> players) {
		this.world = gameWorld.getWorld();
		this.gameWorld = gameWorld;
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

	public static void setRules(Game game) {
		game.setRule(GameRule.CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.HUNGER, RuleResult.DENY);
		game.setRule(GameRule.PORTALS, RuleResult.DENY);
		game.setRule(GameRule.PVP, RuleResult.DENY);
		game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
	}

	public static void open(GameWorld gameWorld, ClutchBridgeMap map, ClutchBridgeConfig config) {
		ClutchBridgeActivePhase phase = new ClutchBridgeActivePhase(gameWorld, config, gameWorld.getPlayers());

		gameWorld.openGame(game -> {
			ClutchBridgeActivePhase.setRules(game);

			// Listeners
			game.on(GameOpenListener.EVENT, phase::open);
			game.on(GameTickListener.EVENT, phase::tick);
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
			game.on(UseBlockListener.EVENT, phase::useBlock);
		});
	}

	public void open() {
		this.opened = true;
		this.singleplayer = this.players.size() == 1;

		int x = 0;
 		for (ServerPlayerEntity player : this.players) {
			player.setGameMode(GameMode.ADVENTURE);
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
			for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
				player.sendMessage(endingMessage, false);
			}

			this.gameWorld.close();
		}
	}

	private Text getEndingMessage() {
		if (this.players.size() == 1) {
			PlayerEntity winner = this.players.iterator().next();
			return winner.getDisplayName().shallowCopy().append(" has won the game!").formatted(Formatting.GOLD);
		}
		return new LiteralText("Nobody won the game!").formatted(Formatting.GOLD);
	}

	private void setSpectator(PlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}

	private void addPlayer(PlayerEntity player) {
		if (!this.players.contains(player)) {
			this.setSpectator(player);
		} else if (this.opened) {
			this.eliminate(player, true);
		}
	}

	private void giveClutchBlocks(ServerPlayerEntity player) {
		for (int slot = 0; slot < 9; slot++) {
			player.inventory.setStack(slot, this.clutchBlock.copy());
		}
	}
	
	private ActionResult useBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
		this.giveClutchBlocks(player);
		return ActionResult.PASS;
	}

	private void eliminate(PlayerEntity eliminatedPlayer, boolean remove) {
		Text message = eliminatedPlayer.getDisplayName().shallowCopy().append(" has been eliminated!").formatted(Formatting.RED);
		for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
			player.sendMessage(message, false);
		}

		if (remove) {
			this.players.remove(eliminatedPlayer);
		}
		this.setSpectator(eliminatedPlayer);
	}

	private ActionResult onPlayerDeath(PlayerEntity player, DamageSource source) {
		this.eliminate(player, true);
		return ActionResult.SUCCESS;
	}

	public static void spawn(int x, ServerWorld world, ServerPlayerEntity player) {
		player.teleport(world, x + 0.5, 65, 0.5, 0, 0);
	}
}