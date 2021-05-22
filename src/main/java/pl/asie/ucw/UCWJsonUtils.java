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
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class UCWJsonUtils {
	private static final Method WITH_PROPERTY = ReflectionHelper.findMethod(IBlockState.class, "withProperty", "func_177226_a", IProperty.class, Comparable.class);
	private static final Class CARVABLE_CLASS = UCWUtils.findClass("team.chisel.common.block.BlockCarvable");

	private UCWJsonUtils() {

	}

	private static IBlockState parseState(String s) throws Exception {
		String[] stateStr = s.split("#", 2);
		ResourceLocation blockLoc = new ResourceLocation(stateStr[0]);
		if (ForgeRegistries.BLOCKS.containsKey(blockLoc)) {
			Block block = ForgeRegistries.BLOCKS.getValue(blockLoc);
			IBlockState state = block.getDefaultState();
			if (stateStr.length > 1) {
				for (String stateParam : stateStr[1].split(",")) {
					String[] stateKv = stateParam.split("=", 2);
					if (stateKv.length > 1) {
						IProperty property = block.getBlockState().getProperty(stateKv[0]);
						if (property != null) {
							com.google.common.base.Optional optValue = property.parseValue(stateKv[1]);
							if (optValue.isPresent()) {
								// TODO: !?!?
								state = (IBlockState) WITH_PROPERTY.invoke(state, property, optValue.get());
							}
						}
					}
				}
			}
			return state;
		} else {
			return null;
		}
	}

	public static List<IBlockState> parseStateList(JsonObject object, boolean orderMatters) throws Exception {
		if (object.has("block")) {
			ResourceLocation blockLoc = new ResourceLocation(object.get("block").getAsString());
			if (ForgeRegistries.BLOCKS.containsKey(blockLoc)) {
				Block block = ForgeRegistries.BLOCKS.getValue(blockLoc);
				List<IBlockState> stateList = new ArrayList<>(16);

				// TODO: Kludge, but seems to fix #72. Also speeds up loading
				if (CARVABLE_CLASS != null && CARVABLE_CLASS.isAssignableFrom(block.getClass())) {
					try {
						IBlockState lastState = null;
						int i = 0;
						while (i < 16) {
							IBlockState nextState = block.getStateFromMeta(i++);
							if (nextState == lastState) {
								break;
							}
							stateList.add(nextState);
							lastState = nextState;
						}
					} catch (IllegalArgumentException e) {
						// pass
					}
					return stateList;
				}

				Set<IBlockState> states = Sets.newIdentityHashSet();
				states.addAll(block.getBlockState().getValidStates());
				TIntSet validMetas = new TIntHashSet();

				for (int i = 0; i < 16; i++) {
					stateList.add(null);
				}

				for (IBlockState state : block.getBlockState().getValidStates()) {
					validMetas.add(block.getMetaFromState(state));
				}

				TIntIterator iterator = validMetas.iterator();
				while (iterator.hasNext()) {
					int i = iterator.next();
					try {
						IBlockState state = block.getStateFromMeta(i);
						if (states.remove(state)) {
							stateList.set(i, state);
						}
					} catch (Exception e) {
						/* ... */
					}
				}

				while (stateList.size() >= 1 && stateList.get(stateList.size() - 1) == null) {
					stateList.remove(stateList.size() - 1);
				}

				return stateList;
			} else {
				return Collections.emptyList();
			}
		} else if (object.has("state")) {
			JsonElement element = object.get("state");
			if (element.isJsonArray()) {
				List<IBlockState> stateList = Lists.newArrayList();
				for (int i = 0; i < 16; i++)
					stateList.add(i, null);

				for (JsonElement el : element.getAsJsonArray()) {
					IBlockState state = parseState(el.getAsString());
					if (state != null) {
						stateList.set(state.getBlock().getMetaFromState(state), state);
					}
				}

				return stateList;
			} else {
				IBlockState state = parseState(element.getAsString());
				if (state != null) {
					return Collections.singletonList(state);
				} else {
					return Collections.emptyList();
				}
			}
		} else {
			return Collections.emptyList();
		}
	}
}
