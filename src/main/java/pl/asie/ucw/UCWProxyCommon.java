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
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import java.util.List;

public class UCWProxyCommon {
	public void preInit() {

	}

	public void init() {

	}

	public void progressPush(String name, int count) {
		UnlimitedChiselWorks.LOGGER.info(name);
	}

	public void progressStep(String text) {

	}

	public void progressPop() {

	}

	public void getSubItemsUCW(UCWObjectFactory.ItemUCW item, List<ItemStack> list) {
		item.getSubItemsServer(CreativeTabs.SEARCH, list);
	}

	public void initBlock(IBlockState state, UCWObjectFactory.BlockUCW block) {

	}
}
