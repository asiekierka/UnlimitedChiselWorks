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
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class UCWObjectFactory {
	protected final Block block;
	protected final Item item;
	protected final IBlockState base;

	public UCWObjectFactory(UCWBlockRule rule, IBlockState base, ResourceLocation location) {
		this.base = base;

		UCWObjectBroker broker = UCWObjectBroker.get();

		broker.begin(rule, base);

		if (rule.customBlockClass != null) {
			try {
				Class c = Class.forName(rule.customBlockClass);
				this.block = (Block) c.getConstructor().newInstance();

				if (!(this.block instanceof IUCWBlock)) {
					throw new RuntimeException("Not an UCW block!");
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			this.block = new BlockUCWProxy();
		}

		if (rule.customItemClass != null) {
			try {
				Class c = Class.forName(rule.customItemClass);
				this.item = (Item) c.getConstructor(Block.class).newInstance(this.block);

				if (!(this.item instanceof IUCWItem)) {
					throw new RuntimeException("Not an UCW item!");
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			this.item = new ItemUCWProxy(block);
		}

		this.block.setRegistryName(location);
		this.item.setRegistryName(location);

		broker.end();
	}
}
