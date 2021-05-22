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

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;

import java.util.List;

public class UCWGroupRule {
	protected final String groupName;
	protected final List<IBlockState> states;

	public UCWGroupRule(JsonObject object) throws Exception {
		this.groupName = object.get("name").getAsString();

		JsonElement element = object.get("entries");
		if (element.isJsonArray()) {
			states = Lists.newArrayList();
			for (JsonElement element1 : element.getAsJsonArray()) {
				states.addAll(UCWJsonUtils.parseStateList(element1.getAsJsonObject(), false));
			}
		} else {
			this.states = UCWJsonUtils.parseStateList(element.getAsJsonObject(), false);
		}
	}
}
