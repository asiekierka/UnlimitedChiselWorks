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
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

public class UCWObjectBroker {
	private static ThreadLocal<UCWObjectBroker> provider = ThreadLocal.withInitial(UCWObjectBroker::new);
	private UCWBlockRule rule;
	private IBlockState base;

	public static UCWObjectBroker get() {
		return provider.get();
	}

	public void begin(UCWBlockRule rule, IBlockState state) {
		this.rule = rule;
		this.base = state;
	}

	public void end() {
		this.rule = null;
		this.base = null;
	}

	public UCWBlockRule getRule() {
		return rule;
	}

	public IBlockState getBase() {
		return base;
	}

	public BlockStateContainer createBlockState(Block block) {
		Collection<IProperty<?>> propertyCollection = rule.throughBlock.getBlockState().getProperties();
		IProperty[] properties = propertyCollection.toArray(new IProperty[propertyCollection.size()]);
		if (rule.throughBlock.getBlockState() instanceof ExtendedBlockState) {
			Collection<IUnlistedProperty<?>> unlistedPropertyCollection = ((ExtendedBlockState) rule.throughBlock.getBlockState()).getUnlistedProperties();
			IUnlistedProperty[] unlistedProperties = unlistedPropertyCollection.toArray(new IUnlistedProperty[unlistedPropertyCollection.size()]);
			return new ExtendedBlockState(block, properties, unlistedProperties);
		} else {
			return new BlockStateContainer(block, properties);
		}
	}
}
