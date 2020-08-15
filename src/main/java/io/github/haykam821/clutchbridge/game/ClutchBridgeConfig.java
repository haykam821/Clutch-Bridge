package io.github.haykam821.clutchbridge.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.clutchbridge.game.map.ClutchBridgeMapConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;

public class ClutchBridgeConfig {
	public static final Codec<ClutchBridgeConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			ClutchBridgeMapConfig.CODEC.fieldOf("map").forGetter(ClutchBridgeConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(ClutchBridgeConfig::getPlayerConfig),
			Codec.INT.optionalFieldOf("delay", 20 * 4).forGetter(ClutchBridgeConfig::getDelay),
			ItemStack.CODEC.optionalFieldOf("clutch_block", new ItemStack(Items.LIGHT_BLUE_WOOL)).forGetter(ClutchBridgeConfig::getClutchBlock)
		).apply(instance, ClutchBridgeConfig::new);
	});

	private final ClutchBridgeMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final int delay;
	private final ItemStack clutchBlock;

	public ClutchBridgeConfig(ClutchBridgeMapConfig mapConfig, PlayerConfig playerConfig, int delay, ItemStack clutchBlock) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.delay = delay;
		this.clutchBlock = clutchBlock;
	}

	public ClutchBridgeMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public int getDelay() {
		return this.delay;
	}

	public ItemStack getClutchBlock() {
		return this.clutchBlock;
	}
}