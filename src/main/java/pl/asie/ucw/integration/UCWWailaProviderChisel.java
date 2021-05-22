/*
 * Copyright (c) 2017, 2018, 2019, 2021 Adrian Siekierka
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

package pl.asie.ucw.integration;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import pl.asie.ucw.IUCWBlock;
import team.chisel.api.block.ICarvable;
import team.chisel.common.block.ItemChiselBlock;

import javax.annotation.Nonnull;
import java.util.List;

public class UCWWailaProviderChisel implements IWailaDataProvider {
	@Nonnull
	@Override
	public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		if (accessor.getBlock() instanceof IUCWBlock) {
			try {
				IBlockState ucwState = accessor.getBlockState();
				IBlockState state = ((IUCWBlock) ucwState.getBlock()).getThroughState(ucwState);
				if (state != null && state.getBlock() instanceof ICarvable) {
					ICarvable block = (ICarvable) state.getBlock();
					int variation = block.getVariationIndex(state);
					ItemChiselBlock.addTooltips(block, variation, currenttip);
				}
			} catch (Exception e) {
				// pass
			}
		}
		return currenttip;
	}
}
