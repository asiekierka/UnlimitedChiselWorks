package pl.asie.ucw;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

public class UCWColorProxy implements IBlockColor, IItemColor {
	protected static final UCWColorProxy INSTANCE = new UCWColorProxy();

	@Override
	public int colorMultiplier(IBlockState state, @Nullable IBlockAccess worldIn, @Nullable BlockPos pos, int tintIndex) {
		if (state.getBlock() instanceof IUCWBlock) {
			IBlockState state1 = ((IUCWBlock) state.getBlock()).getBaseState();
			return Minecraft.getMinecraft().getBlockColors().colorMultiplier(state1, worldIn, pos, tintIndex);
		} else {
			return -1;
		}
	}

	@Override
	public int getColorFromItemstack(ItemStack stack, int tintIndex) {
		Item item = stack.getItem();

		if (item instanceof IUCWItem) {
			Block block = Block.getBlockFromItem(item);
			if (block instanceof IUCWBlock) {
				IBlockState state1 = ((IUCWBlock) block).getBaseState();
				try {
					ItemStack stack1 = state1.getBlock().getItem(Minecraft.getMinecraft().player.getEntityWorld(), Minecraft.getMinecraft().player.getPosition(), state1);
					return Minecraft.getMinecraft().getItemColors().getColorFromItemstack(stack1, tintIndex);
				} catch (Exception e) {
					e.printStackTrace();
					return -1;
				}
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}
}
