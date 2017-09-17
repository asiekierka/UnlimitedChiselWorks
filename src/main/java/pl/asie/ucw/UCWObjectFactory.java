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
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class UCWObjectFactory {
	private static final Random proxyRand = new Random();

	protected final Block block;
	protected final Item item;
	protected final IBlockState base;
	private final UCWBlockRule rule;

	public UCWObjectFactory(UCWBlockRule rule, IBlockState base, ResourceLocation location) {
		this.rule = rule;
		this.base = base;

		this.block = new BlockUCW();
		this.item = new ItemUCW(block);

		this.block.setRegistryName(location);
		this.item.setRegistryName(location);
	}

	public static class UCWBlockAccess implements IBlockAccess {
		private final IBlockAccess parent;

		public UCWBlockAccess(IBlockAccess parent) {
			this.parent = parent;
		}

		@Nullable
		@Override
		public TileEntity getTileEntity(BlockPos pos) {
			return parent.getTileEntity(pos);
		}

		@Override
		public int getCombinedLight(BlockPos pos, int lightValue) {
			return parent.getCombinedLight(pos, lightValue);
		}

		@Override
		public IBlockState getBlockState(BlockPos pos) {
			IBlockState state = parent.getBlockState(pos);
			if (state.getBlock() instanceof BlockUCW) {
				return ((BlockUCW) state.getBlock()).getBaseState();
			} else {
				return state;
			}
		}

		@Override
		public boolean isAirBlock(BlockPos pos) {
			return parent.isAirBlock(pos);
		}

		@Override
		public Biome getBiome(BlockPos pos) {
			return parent.getBiome(pos);
		}

		@Override
		public int getStrongPower(BlockPos pos, EnumFacing direction) {
			return parent.getStrongPower(pos, direction);
		}

		@Override
		public WorldType getWorldType() {
			return parent.getWorldType();
		}

		@Override
		public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
			return parent.isSideSolid(pos, side, _default);
		}
	}

	public class ItemUCW extends ItemBlock {
		public ItemUCW(Block block) {
			super(block);
			setHasSubtypes(true);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
			getItemThrough().addInformation(
					UCWMagic.copyChangeItem(stack, getItemThrough()),
					worldIn, tooltip, flagIn
			);
		}

		@Override
		public String getUnlocalizedName() {
			return getItemFrom().getUnlocalizedName();
		}

		@Override
		public String getUnlocalizedName(ItemStack stack) {
			try {
				return getItemFrom().getUnlocalizedName(new ItemStack(base.getBlock().getItemDropped(base, proxyRand, 0), 1, base.getBlock().damageDropped(base)));
			} catch (Exception e) {
				e.printStackTrace();
				return getUnlocalizedName();
			}
		}
		@Override
		public String getItemStackDisplayName(ItemStack stack) {
			try {
				return getItemFrom().getItemStackDisplayName(new ItemStack(base.getBlock().getItemDropped(base, proxyRand, 0), 1, base.getBlock().damageDropped(base)));
			} catch (Exception e) {
				e.printStackTrace();
				return getUnlocalizedName();
			}
		}

		private Item getItemFrom() {
			return Item.getItemFromBlock(rule.fromBlock);
		}

		private Item getItemThrough() {
			return Item.getItemFromBlock(rule.throughBlock);
		}

		@Override
		public int getMetadata(int damage) {
			if (rule.objectFactories.get(damage) != null) {
				return damage;
			} else {
				return 0;
			}
		}

		@Override
		public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
			Item origItem = getItemThrough();
			NonNullList<ItemStack> proxyList = NonNullList.create();
			origItem.getSubItems(tab, proxyList);
			for (ItemStack stack : proxyList) {
				if (stack.getItem() == origItem) {
					// FIXME: Dirt#9 doesn't really work well :-(
					if (rule.throughBlock.getRegistryName().toString().equals("chisel:dirt") && stack.getItemDamage() == 9) {
						continue;
					}

					items.add(UCWMagic.copyChangeItem(stack, this));
				}
			}
		}
	}

	public class BlockUCW extends Block {
		public BlockUCW() {
			super(base.getMaterial());
			setCreativeTab(base.getBlock().getCreativeTabToDisplayOn());
			setUnlocalizedName(base.getBlock().getUnlocalizedName());
		}

		private IBlockState applyProperties(Block block, IBlockState state) {
			IBlockState toState = block.getDefaultState();
			for (IProperty property : state.getPropertyKeys()) {
				toState = toState.withProperty(property, state.getValue(property));
			}
			return toState;
		}

		@Override
		public boolean isOpaqueCube(IBlockState state) {
			return applyProperties(rule.throughBlock, state).isOpaqueCube();
		}

		@Override
		public boolean isFullCube(IBlockState state) {
			return applyProperties(rule.throughBlock, state).isFullCube();
		}

		@Override
		public boolean isFullBlock(IBlockState state) {
			return applyProperties(rule.throughBlock, state).isFullBlock();
		}

		@Override
		public boolean isNormalCube(IBlockState state) {
			return applyProperties(rule.throughBlock, state).isNormalCube();
		}

		@Override
		public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
			return applyProperties(rule.throughBlock, state).getLightOpacity(world, pos);
		}

		@Override
		public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
			return applyProperties(rule.throughBlock, state).getLightValue(world, pos);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
			IBlockState newState = applyProperties(rule.throughBlock, state);
			return newState.getBlock().canRenderInLayer(newState, layer);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public BlockRenderLayer getBlockLayer() {
			return rule.throughBlock.getBlockLayer();
		}

		@Override
		public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
			return rule.fromBlock.canHarvestBlock(new UCWBlockAccess(world), pos, player);
		}

		@Override
		public float getBlockHardness(IBlockState blockState, World worldIn, BlockPos pos) {
			try {
				return rule.fromBlock.getBlockHardness(base, worldIn, pos);
			} catch (Exception e) {
				try {
					return rule.throughBlock.getBlockHardness(applyProperties(rule.throughBlock, blockState), worldIn, pos);
				} catch (Exception ee) {
					return blockHardness;
				}
			}
		}

		@Override
		public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
			return rule.fromBlock.getFlammability(new UCWBlockAccess(world), pos, face);
		}

		@Override
		public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
			return rule.fromBlock.isFlammable(new UCWBlockAccess(world), pos, face);
		}

		@Override
		public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
			return rule.fromBlock.getFireSpreadSpeed(new UCWBlockAccess(world), pos, face);
		}

		@Override
		public boolean isBeaconBase(IBlockAccess worldObj, BlockPos pos, BlockPos beacon) {
			return rule.fromBlock.isBeaconBase(new UCWBlockAccess(worldObj), pos, beacon);
		}

		@Override
		public int getHarvestLevel(IBlockState state) {
			return base.getBlock().getHarvestLevel(base);
		}

		@Override
		public boolean isToolEffective(String type, IBlockState state) {
			return base.getBlock().isToolEffective(type, base);
		}

		@Override
		public int damageDropped(IBlockState state) {
			return getMetaFromState(state);
		}

		@Override
		public IBlockState getStateFromMeta(int meta) {
			return applyProperties(this, rule.throughBlock.getStateFromMeta(meta));
		}

		@Override
		public int getMetaFromState(IBlockState state) {
			return rule.throughBlock.getMetaFromState(applyProperties(rule.throughBlock, state));
		}

		@Override
		public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
			item.getSubItems(tab, items);
		}

		@Override
		protected BlockStateContainer createBlockState() {
			Collection<IProperty<?>> propertyCollection = rule.throughBlock.getBlockState().getProperties();
			IProperty[] properties = propertyCollection.toArray(new IProperty[propertyCollection.size()]);
			if (rule.throughBlock.getBlockState() instanceof ExtendedBlockState) {
				Collection<IUnlistedProperty<?>> unlistedPropertyCollection = ((ExtendedBlockState) rule.throughBlock.getBlockState()).getUnlistedProperties();
				IUnlistedProperty[] unlistedProperties = unlistedPropertyCollection.toArray(new IUnlistedProperty[unlistedPropertyCollection.size()]);
				return new ExtendedBlockState(this, properties, unlistedProperties);
			} else {
				return new BlockStateContainer(this, properties);
			}
		}

		public IBlockState getBaseState() {
			return base;
		}
	}
}
