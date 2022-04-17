package io.github.haykam821.clutchbridge.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public class ClutchBridgeMapConfig {
	private static final BlockState DEFAULT_STATE = Blocks.MAGENTA_GLAZED_TERRACOTTA.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH);
	public static final Codec<ClutchBridgeMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("spacing").forGetter(map -> map.spacing),
			BlockStateProvider.TYPE_CODEC.optionalFieldOf("state_provider", BlockStateProvider.of(DEFAULT_STATE)).forGetter(map -> map.stateProvider)
		).apply(instance, ClutchBridgeMapConfig::new);
	});

	private final int spacing;
	private final BlockStateProvider stateProvider;

	public ClutchBridgeMapConfig(int spacing, BlockStateProvider stateProvider) {
		this.spacing = spacing;
		this.stateProvider = stateProvider;
	}

	public int getSpacing() {
		return this.spacing;
	}

	public BlockStateProvider getStateProvider() {
		return this.stateProvider;
	}
}