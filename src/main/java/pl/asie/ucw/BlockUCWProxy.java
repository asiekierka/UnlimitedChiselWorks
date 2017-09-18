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
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class BlockUCWProxy extends Block implements IUCWBlock {
	private UCWBlockRule rule;
	private IBlockState base;

	public BlockUCWProxy() {
		super(UCWObjectBroker.get().getBase().getMaterial());

		rule = UCWObjectBroker.get().getRule();
		base = UCWObjectBroker.get().getBase();

		UnlimitedChiselWorks.proxy.initBlock(base, this);
		setUnlocalizedName(base.getBlock().getUnlocalizedName());
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return UCWUtils.applyProperties(rule.throughBlock, state).isOpaqueCube();
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return UCWUtils.applyProperties(rule.throughBlock, state).isFullCube();
	}

	@Override
	public boolean isFullBlock(IBlockState state) {
		return UCWUtils.applyProperties(rule.throughBlock, state).isFullBlock();
	}

	@Override
	public boolean isNormalCube(IBlockState state) {
		return UCWUtils.applyProperties(rule.throughBlock, state).isNormalCube();
	}

	@Override
	public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
		return base.getLightOpacity(new UCWBlockAccess(world), pos);
	}

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
		return base.getLightValue(new UCWBlockAccess(world), pos);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
		IBlockState newState = UCWUtils.applyProperties(rule.throughBlock, state);
		return newState.getBlock().canRenderInLayer(newState, layer);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getBlockLayer() {
		return rule.throughBlock.getBlockLayer();
	}

	@Override
	public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
		return base.getBlock().isReplaceable(new UCWBlockAccess(worldIn), pos);
	}

	@Override
	public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
		return rule.fromBlock.canHarvestBlock(new UCWBlockAccess(world), pos, player);
	}

	@Override
	public float getExplosionResistance(Entity exploder) {
		return base.getBlock().getExplosionResistance(exploder);
	}

	@Override
	public SoundType getSoundType() {
		return base.getBlock().getSoundType();
	}

	@Override
	public boolean isLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
		return base.getBlock().isLeaves(base, new UCWBlockAccess(world), pos);
	}

	@Override
	public boolean isWood(IBlockAccess world, BlockPos pos) {
		return base.getBlock().isWood(new UCWBlockAccess(world), pos);
	}

	@Override
	public boolean isFoliage(IBlockAccess world, BlockPos pos) {
		return base.getBlock().isFoliage(new UCWBlockAccess(world), pos);
	}

	@Override
	public float getBlockHardness(IBlockState blockState, World worldIn, BlockPos pos) {
		try {
			return rule.fromBlock.getBlockHardness(base, worldIn, pos);
		} catch (Exception e) {
			try {
				return rule.throughBlock.getBlockHardness(UCWUtils.applyProperties(rule.throughBlock, blockState), worldIn, pos);
			} catch (Exception ee) {
				return blockHardness;
			}
		}
	}

	@Override
	public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World worldIn, BlockPos pos) {
		try {
			return rule.fromBlock.getPlayerRelativeBlockHardness(base, player, worldIn, pos);
		} catch (Exception e) {
			try {
				return rule.throughBlock.getPlayerRelativeBlockHardness(UCWUtils.applyProperties(rule.throughBlock, state), player, worldIn, pos);
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
		return UCWUtils.applyProperties(this, rule.throughBlock.getStateFromMeta(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return rule.throughBlock.getMetaFromState(UCWUtils.applyProperties(rule.throughBlock, state));
	}

	@Override
	public void getSubBlocks(Item itemIn, CreativeTabs tab, NonNullList<ItemStack> items) {
		Item.getItemFromBlock(this).getSubItems(Item.getItemFromBlock(this), tab, items);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		rule = UCWObjectBroker.get().getRule();
		base = UCWObjectBroker.get().getBase();

		return UCWObjectBroker.get().createBlockState(this);
	}

	@Override
	public IBlockState getBaseState() {
		return base;
	}
}
