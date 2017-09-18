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

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

public class CommandUCWDebug extends CommandBase {
	@Override
	public String getName() {
		return "ucw_debug";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "no, don't";
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
		if (args.length == 1) {
			Set<String> domains = new HashSet<>();
			for (UCWBlockRule rule : UnlimitedChiselWorks.BLOCK_RULES) {
				domains.add(rule.fromBlock.getRegistryName().getResourceDomain());
			}

			return getListOfStringsMatchingLastWord(args, domains.toArray(new String[domains.size()]));
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (sender instanceof EntityPlayerMP) {
			World world = sender.getEntityWorld();
			BlockPos pos = sender.getPosition();
			for (UCWBlockRule rule : UnlimitedChiselWorks.BLOCK_RULES) {
				if (args.length >= 1 && !rule.fromBlock.getRegistryName().toString().toLowerCase().startsWith(args[0].toLowerCase())) {
					continue;
				}

				for (UCWObjectFactory factory : rule.objectFactories.valueCollection()) {
					NonNullList<ItemStack> stackList = NonNullList.create();
					UnlimitedChiselWorks.proxy.getSubItemsUCW(factory.item, stackList);

					world.setBlockState(pos, factory.base);

					BlockPos.MutableBlockPos posCopy = new BlockPos.MutableBlockPos(pos);
					posCopy.move(EnumFacing.EAST);

					for (ItemStack stack : stackList) {
						try {
							world.setBlockState(posCopy, factory.block.getStateFromMeta(stack.getMetadata()));
							posCopy.move(EnumFacing.EAST);
						} catch (Exception e) {

						}
					}

					pos = pos.south();
				}
			}
		}
	}
}
