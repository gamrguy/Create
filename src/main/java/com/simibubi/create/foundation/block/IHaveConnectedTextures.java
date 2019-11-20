package com.simibubi.create.foundation.block;

import java.util.function.BiPredicate;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface IHaveConnectedTextures {

	class CTContext {
		boolean up, down, left, right;
		boolean topLeft, topRight, bottomLeft, bottomRight;
	}

	@OnlyIn(Dist.CLIENT)
	public boolean appliesTo(BakedQuad quad);

	default boolean connectsTo(BlockState state, BlockState other, IEnviromentBlockReader reader, BlockPos pos,
			BlockPos otherPos, Direction face) {

		BlockPos blockingPos = otherPos.offset(face);
		if ((face.getAxis().getCoordinate(pos.getX(), pos.getY(), pos.getZ()) == face.getAxis()
				.getCoordinate(otherPos.getX(), otherPos.getY(), otherPos.getZ()))
				&& connectsTo(state, reader.getBlockState(blockingPos), reader, pos, blockingPos, face))
			return false;

		return state.getBlock() == other.getBlock();
	}

	default int getTextureIndex(IEnviromentBlockReader reader, BlockPos pos, BlockState state, Direction face) {
		return getTextureIndexForContext(reader, pos, state, face, buildContext(reader, pos, state, face));
	}

	default CTContext buildContext(IEnviromentBlockReader reader, BlockPos pos, BlockState state, Direction face) {
		Axis axis = face.getAxis();
		boolean positive = face.getAxisDirection() == AxisDirection.POSITIVE;
		Direction h = axis == Axis.X ? Direction.SOUTH : Direction.WEST;
		Direction v = axis.isHorizontal() ? Direction.UP : Direction.NORTH;
		h = positive ? h.getOpposite() : h;
		if (face == Direction.DOWN) {
			v = v.getOpposite();
			h = h.getOpposite();
		}

		final Direction horizontal = h;
		final Direction vertical = v;

		BiPredicate<Integer, Integer> connection = (x, y) -> {
			BlockPos p = pos.offset(horizontal, x).offset(vertical, y);
			return connectsTo(state, reader.getBlockState(p), reader, pos, p, face);
		};

		CTContext context = new CTContext();
		context.up = connection.test(0, 1);
		context.down = connection.test(0, -1);
		context.left = connection.test(-1, 0);
		context.right = connection.test(1, 0);
		context.topLeft = connection.test(-1, 1);
		context.topRight = connection.test(1, 1);
		context.bottomLeft = connection.test(-1, -1);
		context.bottomRight = connection.test(1, -1);
		return context;
	}

	default int getTextureIndexForContext(IEnviromentBlockReader reader, BlockPos pos, BlockState state, Direction face,
			CTContext c) {
		int tileX = 0, tileY = 0;
		int borders = (!c.up ? 1 : 0) + (!c.down ? 1 : 0) + (!c.left ? 1 : 0) + (!c.right ? 1 : 0);

		if (c.up)
			tileX++;
		if (c.down)
			tileX += 2;
		if (c.left)
			tileY++;
		if (c.right)
			tileY += 2;

		if (borders == 0) {
			if (c.topRight)
				tileX++;
			if (c.topLeft)
				tileX += 2;
			if (c.bottomRight)
				tileY += 2;
			if (c.bottomLeft)
				tileY++;
		}

		if (borders == 1) {
			if (!c.right) {
				if (c.topLeft || c.bottomLeft) {
					tileY = 4;
					tileX = -1 + (c.bottomLeft ? 1 : 0) + (c.topLeft ? 1 : 0) * 2;
				}
			}
			if (!c.left) {
				if (c.topRight || c.bottomRight) {
					tileY = 5;
					tileX = -1 + (c.bottomRight ? 1 : 0) + (c.topRight ? 1 : 0) * 2;
				}
			}
			if (!c.down) {
				if (c.topLeft || c.topRight) {
					tileY = 6;
					tileX = -1 + (c.topLeft ? 1 : 0) + (c.topRight ? 1 : 0) * 2;
				}
			}
			if (!c.up) {
				if (c.bottomLeft || c.bottomRight) {
					tileY = 7;
					tileX = -1 + (c.bottomLeft ? 1 : 0) + (c.bottomRight ? 1 : 0) * 2;
				}
			}
		}

		if (borders == 2) {
			if ((c.up && c.left && c.topLeft) || (c.down && c.left && c.bottomLeft) || (c.up && c.right && c.topRight)
					|| (c.down && c.right && c.bottomRight))
				tileX += 3;
		}

		return tileX + 8 * tileY;
	}

}