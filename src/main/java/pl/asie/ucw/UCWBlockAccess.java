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

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;

public class UCWBlockAccess implements IBlockAccess {
	private final IBlockAccess parent;
	private final boolean isThrough;

	@Deprecated
	public UCWBlockAccess(IBlockAccess parent) {
		this(parent, false);
	}

	public UCWBlockAccess(IBlockAccess parent, boolean isThrough) {
		this.parent = parent;
		this.isThrough = isThrough;
	}

	@Nullable
	@Override
	public TileEntity getTileEntity(BlockPos pos) {
		return parent.getTileEntity(pos);
	}

	@Override
	public int getCombinedLight(BlockPos pos, int lightValue) {
		return parent.getCombinedLight(pos, lightValue);
	}

	@Override
	public IBlockState getBlockState(BlockPos pos) {
		IBlockState state = parent.getBlockState(pos);
		if (state.getBlock() instanceof IUCWBlock) {
			if (isThrough) {
				return ((IUCWBlock) state.getBlock()).getThroughState(state);
			} else {
				return ((IUCWBlock) state.getBlock()).getBaseState();
			}
		} else {
			return state;
		}
	}

	@Override
	public boolean isAirBlock(BlockPos pos) {
		return parent.isAirBlock(pos);
	}

	@Override
	public Biome getBiome(BlockPos pos) {
		return parent.getBiome(pos);
	}

	@Override
	public int getStrongPower(BlockPos pos, EnumFacing direction) {
		return parent.getStrongPower(pos, direction);
	}

	@Override
	public WorldType getWorldType() {
		return parent.getWorldType();
	}

	@Override
	public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
		return parent.isSideSolid(pos, side, _default);
	}
}
