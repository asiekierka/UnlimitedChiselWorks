/*
 * Copyright (c) 2017 Adrian Siekierka
 *
 * This file is part of Unlimited Chisel Works.
 *
 * Unlimited Chisel Works is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unlimited Chisel Works is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Unlimited Chisel Works.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.ucw;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

public class UCWColorProxy implements IBlockColor, IItemColor {
	protected static final UCWColorProxy INSTANCE = new UCWColorProxy();

	private final RayTraceResult dummyResult = new RayTraceResult(RayTraceResult.Type.MISS, Vec3d.ZERO, null, BlockPos.ORIGIN);
	private final Minecraft mc = Minecraft.getMinecraft();

	@Override
	public int colorMultiplier(IBlockState state, @Nullable IBlockAccess worldIn, @Nullable BlockPos pos, int tintIndex) {
		if (state.getBlock() instanceof IUCWBlock) {
			IBlockState state1 = ((IUCWBlock) state.getBlock()).getBaseState();
			return mc.getBlockColors().colorMultiplier(state1, worldIn, pos, tintIndex);
		} else {
			return -1;
		}
	}
	
	@Override
	public int colorMultiplier(ItemStack stack, int tintIndex) {
		Item item = stack.getItem();
		Block block = Block.getBlockFromItem(item);
		if (block instanceof IUCWBlock) {
			IBlockState state1 = ((IUCWBlock) block).getBaseState();
			try {
				ItemStack stack1 = state1.getBlock().getPickBlock(state1, dummyResult, mc.player.getEntityWorld(), mc.player.getPosition(), mc.player);
				return mc.getItemColors().colorMultiplier(stack1, tintIndex);
			} catch (Exception e) {
				e.printStackTrace();
				try {
					ItemStack stack1 = state1.getBlock().getItem(mc.player.getEntityWorld(), mc.player.getPosition(), state1);
					return mc.getItemColors().colorMultiplier(stack1, tintIndex);
				} catch (Exception ee) {
					ee.printStackTrace();
					return -1;
				}
			}
		} else {
			return -1;
		}
	}
}
