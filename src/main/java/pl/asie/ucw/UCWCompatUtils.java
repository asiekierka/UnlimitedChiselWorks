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

package pl.asie.ucw;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.event.FMLInterModComms;

public final class UCWCompatUtils {
	private UCWCompatUtils() {

	}

	public static void addChiselVariation(String group, ItemStack stack) {
		if (!stack.isEmpty()) {
			NBTTagCompound tag = new NBTTagCompound();
			NBTTagCompound itemTag = new NBTTagCompound();
			stack.writeToNBT(itemTag);

			tag.setString("group", group);
			tag.setTag("stack", itemTag);

			FMLInterModComms.sendMessage("chisel", "add_variation", tag);
		}
	}
}
