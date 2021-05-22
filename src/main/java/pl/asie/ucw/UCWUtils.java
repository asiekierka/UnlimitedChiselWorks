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

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UCWUtils {
	private UCWUtils() {

	}

	public static String[] getUcwLocationData(ResourceLocation location) {
		String path = location.getPath();
		Matcher m = Pattern.compile("ucw_ucw_([A-Za-z0-9_]+)/([a-z]+)").matcher(path);
		String[] str = new String[] {
				"",
				"",
				""
		};

		if (m.find()) {
			str[0] = m.group(1);
			str[1] = m.group(2);
		}

		String nonProxyPath = path.replaceAll("ucw_ucw_[A-Za-z0-9_]+/[a-z]+/", "");
		str[2] = nonProxyPath;
		return str;
	}

	public static ResourceLocation fromUcwGenerated(ResourceLocation location) {
		if (!"ucw_generated".equals(location.getNamespace())) {
			return location;
		}

		String[] str = getUcwLocationData(location);
		return new ResourceLocation(str[1], str[2]);
	}

	public static ResourceLocation toUcwGenerated(ResourceLocation oldLocation, String s2) {
		return new ResourceLocation("ucw_generated", "ucw_ucw_" + s2 + "/" + oldLocation.getNamespace() + "/" + oldLocation.getPath());
	}

	public static String toUcwGenerated(String[] oldLocation, String s2) {
		return "ucw_generated:ucw_ucw_" + s2 + "/" + oldLocation[0] + "/" + oldLocation[1];
	}

	public static ItemStack copyChangeItem(ItemStack stack, Item item) {
		return copyChangeItem(stack, item, stack.getItemDamage());
	}

	public static ItemStack copyChangeItem(ItemStack stack, Item item, int damage) {
		ItemStack stackCopy = new ItemStack(item, stack.getCount(), damage);
		if (stack.hasTagCompound()) {
			stackCopy.setTagCompound(stack.getTagCompound().copy());
		}
		return stackCopy;
	}

}
