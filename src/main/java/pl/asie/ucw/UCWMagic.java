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

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;

import java.awt.*;

public final class UCWMagic {
	private UCWMagic() {

	}

	public static ResourceLocation getLocation(IBlockState state, ModelResourceLocation location, IModel model) {
		String domain = location.getResourceDomain();

		if ("forestry".equals(domain)) {
			String[] name = state.getBlock().getRegistryName().getResourcePath().split("\\.", 2);
			IProperty variantProp = state.getBlock().getBlockState().getProperty("variant");
			if (variantProp != null) {
				String variant = variantProp.getName(state.getValue(variantProp));
				if (name.length == 2 && ("planks".equals(name[0]))) {
					return new ResourceLocation("forestry", "blocks/wood/" + name[0] + "." + variant);
				}
			}
		} else if ("extratrees".equals(domain)) {
			String[] name = state.getBlock().getRegistryName().getResourcePath().split("\\.", 2);
			IProperty variantProp = state.getBlock().getBlockState().getProperty("variant");
			if (variantProp != null) {
				String variant = variantProp.getName(state.getValue(variantProp));
				if (name.length == 2 && ("planks".equals(name[0]))) {
					return new ResourceLocation("extratrees", "blocks/planks/" + variant);
				}
			}
		}

		System.out.println(state);
		return model.getTextures().iterator().next();
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

	private static float hsl_hue2rgb(float v1, float v2, float hue) {
		if (hue < 0.0f) hue += 1.0f;
		else if (hue > 1.0f) hue -= 1.0f;

		if ((6 * hue) < 1) return (v1 + (v2 - v1) * 6.0f * hue);
		else if ((2 * hue) < 1) return v2;
		else if ((3 * hue) < 2) return (v1 + (v2 - v1) * ((2.0f/3.0f)-hue)*6.0f);
		else return v1;
	}

	private static int asFF(float f) {
		return (Math.round(f * 255.0f) & 0xFF);
	}

	private static int fromHSL(float[] hsl) {
		if (hsl[1] == 0) {
			return 0xFF000000 | (asFF(hsl[2]) * 0x10101);
		} else {
			float v2 = hsl[2] < 0.5 ? hsl[2] * (1 + hsl[1]) : (hsl[2] + hsl[1]) - (hsl[1] * hsl[2]);
			float v1 = 2 * hsl[2] - v2;

			int r = asFF(hsl_hue2rgb(v1, v2, hsl[0] + 1.0f/3.0f));
			int g = asFF(hsl_hue2rgb(v1, v2, hsl[0]));
			int b = asFF(hsl_hue2rgb(v1, v2, hsl[0] - 1.0f/3.0f));
			return 0xFF000000 | (r << 16) | (g << 8) | b;
		}
	}

	private static int fromHSB(float[] hsb) {
		return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
	}

	private static float[] toHSL(int rgb) {
		float r = (float) ((rgb >> 16) & 0xFF) / 255.0f;
		float g = (float) ((rgb >> 8) & 0xFF) / 255.0f;
		float b = (float) (rgb & 0xFF) / 255.0f;

		float cMin = Math.min(Math.min(r, g), b);
		float cMax = Math.max(Math.max(r, g), b);
		float cDelta = cMax - cMin;

		float l = (cMax + cMin) / 2.0f;

		if (cMin == cMax) {
			return new float[] {0.0f, 0.0f, l};
		} else {
			float[] hsb = new float[] {
					0,
					l < 0.5f ? (cDelta / (cMin + cMax)) : (cDelta / (2 - cMax - cMin)),
					l
			};

			float dr = (((cMax - r) / 6.0f) + (cMax / 2.0f)) / cDelta;
			float dg = (((cMax - g) / 6.0f) + (cMax / 2.0f)) / cDelta;
			float db = (((cMax - b) / 6.0f) + (cMax / 2.0f)) / cDelta;

			if (cMax == r) hsb[0] = db - dg;
			else if (cMax == g) hsb[0] = (1.0f/3.0f) + dr - db;
			else if (cMax == b) hsb[0] = (2.0f/3.0f) + dg - dr;

			if (hsb[0] < 0.0f) hsb[0] += 1.0f;
			else if (hsb[0] > 1.0f) hsb[0] -= 1.0f;

			return hsb;
		}
	}

	private static float[] toHSB(int rgb) {
		float[] hsb = new float[3];
		Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
		return hsb;
	}

	private static float[] calculateContrast(int[] data) {
		float[] contrast = new float[] { Float.MAX_VALUE, Float.MIN_VALUE };

		for (int i : data) {
			float[] d = toHSB(i);
			if (contrast[0] > d[2]) contrast[0] = d[2];
			if (contrast[1] < d[2]) contrast[1] = d[2];
		}

		contrast[1] -= contrast[0];
		return contrast;
	}

	public static int[] transform(TextureAtlasSprite sprite, int frame, TextureAtlasSprite from, TextureAtlasSprite basedUpon) {
		int[] texture = sprite.getFrameTextureData(frame)[0];
		int width = sprite.getIconWidth();
		int height = sprite.getIconHeight();
		float[] contrastFrom = calculateContrast(from.getFrameTextureData(0)[0]);
		float[] contrastBasedUpon = calculateContrast(basedUpon.getFrameTextureData(0)[0]);
		double avgHueFromS = 0;
		double avgHueFromC = 0;
		double avgHueFrom;
		double avgSatFrom = 0;
		for (int i : from.getFrameTextureData(0)[0]) {
			float[] hd = toHSB(i);
			avgHueFromS += Math.sin(hd[0] * 2 * Math.PI);
			avgHueFromC += Math.cos(hd[0] * 2 * Math.PI);
			avgSatFrom += hd[1];
		}
		avgHueFrom = Math.atan2(avgHueFromS, avgHueFromC) / 2.0f / Math.PI;
		avgSatFrom /= from.getIconWidth() * from.getIconHeight();

		int[] texData = new int[texture.length];
		for (int iy = 0; iy < height; iy++) {
			for (int ix = 0; ix < width; ix++) {
				int i = iy*width+ix;
				int it = texture[i];
				int ibu = from.getFrameTextureData(0)[0][(iy % from.getIconHeight())*from.getIconWidth() + (ix % from.getIconWidth())];

				float[] hsbTex = toHSB(it);
				float[] hsbBu = toHSB(ibu);
				if (hsbBu[2] < 0.1 && hsbBu[1] < 0.1 && avgSatFrom >= 0.3) {
					hsbBu[0] = (float) avgHueFrom;
					hsbBu[1] = (float) avgSatFrom;
				}
				double normV = (double) (hsbTex[2] - contrastBasedUpon[0]) / contrastBasedUpon[1];
				float v = (float) ((normV * contrastFrom[1]) + contrastFrom[0]);

				if (v < 0) v = 0;
				if (v > 1) v = 1;
				texData[i] = fromHSB(new float[]{hsbBu[0], hsbBu[1], v});
			}
		}
		return texData;
	}
}
