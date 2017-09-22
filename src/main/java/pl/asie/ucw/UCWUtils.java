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
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class UCWUtils {
	private UCWUtils() {

	}

	public static IBlockState applyProperties(Block block, IBlockState state) {
		IBlockState toState = block
				.getDefaultState();
		for (IProperty property : state.getPropertyNames()) {
			toState = toState.withProperty(property, state.getValue(property));
		}
		return toState;
	}

	public static ItemStack copyChangeItem(ItemStack stack, Item item) {
		return copyChangeItem(stack, item, stack.getItemDamage());
	}

	public static ItemStack copyChangeItem(ItemStack stack, Item item, int damage) {
		ItemStack stackCopy = new ItemStack(item, stack.stackSize, damage);
		if (stack.hasTagCompound()) {
			stackCopy.setTagCompound(stack.getTagCompound().copy());
		}
		return stackCopy;
	}

}
