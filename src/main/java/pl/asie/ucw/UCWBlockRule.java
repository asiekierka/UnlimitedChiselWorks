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

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.*;

public class UCWBlockRule {
	private static final Method WITH_PROPERTY = ReflectionHelper.findMethod(IBlockState.class, "withProperty", "func_177226_a", IProperty.class, Comparable.class);
	protected final List<IBlockState> from;
	protected final List<IBlockState> through;
	protected final List<IBlockState> basedUpon;
	protected @Nonnull final Block fromBlock, throughBlock, basedUponBlock;
	protected final TIntObjectMap<UCWObjectFactory> objectFactories = new TIntObjectHashMap<>();
	protected final String prefix;
	protected final String group;
	protected final int fromCount;

	public UCWBlockRule(JsonObject object) throws Exception {
		from = parseStateList(object.get("from").getAsJsonObject(), true);
		through = parseStateList(object.get("through").getAsJsonObject(), true);
		basedUpon = parseStateList(object.get("based_upon").getAsJsonObject(), false);
		fromBlock = getBlock(from);
		throughBlock = getBlock(through);
		basedUponBlock = getBlock(basedUpon);

		int fc = 0;
		for (IBlockState state : from) {
			if (state != null) fc++;
		}
		fromCount = fc;

		group = object.has("group") ? object.get("group").getAsString() : fromBlock.getRegistryName().toString();

		String s1 = throughBlock.getRegistryName().toString().trim().replaceAll("[^A-Za-z0-9]", "_");
		String s2 = fromBlock.getRegistryName().toString().trim().replaceAll("[^A-Za-z0-9]", "_");
		prefix = s1 + "_" + s2 + "_";
		for (int i = 0; i < from.size(); i++) {
			if (from.get(i) != null) {
				IBlockState state = from.get(i);
				String s = prefix + i;

				objectFactories.put(i, new UCWObjectFactory(this, state, new ResourceLocation(UnlimitedChiselWorks.MODID, s)));
			}
		}
	}

	@Override
	public int hashCode() {
		return 7 * (17 * from.hashCode() + through.hashCode()) + basedUpon.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof UCWBlockRule) {
			UCWBlockRule other = (UCWBlockRule) o;
			return other.from.equals(from) && other.through.equals(through) && other.basedUpon.equals(basedUpon);
		}

		return false;
	}

	@Override
	public String toString() {
		return "[UCWBlockRule: " + fromBlock.getRegistryName() + " -> " + throughBlock.getRegistryName() + " (" + basedUponBlock.getRegistryName() + ")]";
	}

	public boolean isValid() {
		return from.size() > 0 && through.size() > 0 && (basedUpon.size() == 1 || basedUpon.size() == through.size());
	}

	public void registerBlocks(IForgeRegistry<Block> blocks) {
		for (UCWObjectFactory objectFactory : objectFactories.valueCollection()) {
			blocks.register(objectFactory.block);
		}
	}

	public void registerItems(IForgeRegistry<Item> blocks) {
		for (UCWObjectFactory objectFactory : objectFactories.valueCollection()) {
			blocks.register(objectFactory.item);
		}
	}

	private static Block getBlock(List<IBlockState> list) {
		for (IBlockState state : list) {
			if (state != null) {
				return state.getBlock();
			}
		}
		return Blocks.AIR;
	}

	private static List<IBlockState> parseStateList(JsonObject object, boolean orderMatters) throws Exception {
		if (object.has("block")) {
			ResourceLocation blockLoc = new ResourceLocation(object.get("block").getAsString());
			if (ForgeRegistries.BLOCKS.containsKey(blockLoc)) {
				Block block = ForgeRegistries.BLOCKS.getValue(blockLoc);
				if (orderMatters) {
					Set<IBlockState> states = Sets.newHashSet(block.getBlockState().getValidStates());
					List<IBlockState> stateList = new ArrayList<>(16);

					for (int i = 0; i < 16; i++) {
						stateList.add(null);
						try {
							IBlockState state = block.getStateFromMeta(i);
							if (states.remove(state)) {
								stateList.set(i, state);
							}
						} catch (Exception e) {
							e.printStackTrace();
							/* ... */
						}
					}

					return stateList;
				} else {
					return block.getBlockState().getValidStates();
				}
			} else {
				return Collections.emptyList();
			}
		} else if (object.has("state")) {
			String[] stateStr = object.get("state").getAsString().split("#", 2);
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
				return Collections.singletonList(state);
			} else {
				return Collections.emptyList();
			}
		} else {
			return Collections.emptyList();
		}
	}
}
