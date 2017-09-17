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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class UCWMagic {
	public static final BufferedImage missingNo;

	static {
		missingNo = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < 256; i++) {
			missingNo.setRGB(i & 15, i >> 4, ((i ^ (i >> 4)) & 8) != 0 ? 0xFF000000 : 0xFFFF00FF);
		}
	}

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

		// System.out.println(state);
		return model.getTextures().iterator().next();
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
		int ir = ((rgb >> 16) & 0xFF);
		int ig = ((rgb >> 8) & 0xFF);
		int ib = (rgb & 0xFF);

		float r = (float) ir / 255.0f;
		float g = (float) ig / 255.0f;
		float b = (float) ib / 255.0f;

		int ciMin = Math.min(Math.min(ir, ig), ib);
		int ciMax = Math.max(Math.max(ir, ig), ib);
		float cMin = (float) ciMin / 255.0f;
		float cMax = (float) ciMax / 255.0f;
		float cDelta = cMax - cMin;

		float l = (cMax + cMin) / 2.0f;

		if (ciMin == ciMax) {
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
			float[] d = toHSL(i);
			if (contrast[0] > d[2]) contrast[0] = d[2];
			if (contrast[1] < d[2]) contrast[1] = d[2];
		}

		contrast[1] -= contrast[0];
		return contrast;
	}

	private static int[] getRGB(BufferedImage sprite) {
		return UCWProxyClient.getRGB(sprite);
	}

	public static int[] transform(BufferedImage sprite, BufferedImage from, BufferedImage overlay, BufferedImage basedUpon, UCWBlockRule.BlendMode mode) {
		int[] texture = getRGB(sprite);
		int width = sprite.getWidth();
		int height = sprite.getHeight();
		float[] contrastFrom = calculateContrast(getRGB(from));
		float[] contrastBasedUpon = calculateContrast(getRGB(basedUpon));
		double avgHueFromS = 0;
		double avgHueFromC = 0;
		double avgHueFrom;
		double avgSatFrom = 0;
		for (int i : getRGB(from)) {
			float[] hd = toHSL(i);
			avgHueFromS += Math.sin(hd[0] * 2 * Math.PI);
			avgHueFromC += Math.cos(hd[0] * 2 * Math.PI);
			avgSatFrom += hd[1];
		}
		avgHueFrom = Math.atan2(avgHueFromS, avgHueFromC) / 2.0f / Math.PI;
		avgSatFrom /= from.getWidth() * from.getHeight();

		double[] hueRange = new double[4];
		double[] satRange = new double[2];
		int[] srdiv = new int[2];

		if (mode == UCWBlockRule.BlendMode.PLANK) {
			for (int i : getRGB(from)) {
				float[] hd = toHSL(i);
				double normV = (double) (hd[2] - contrastFrom[0]) / contrastFrom[1];
				hueRange[0] += Math.sin(hd[0] * 2 * Math.PI) * (1 - normV);
				hueRange[1] += Math.sin(hd[0] * 2 * Math.PI) * normV;
				hueRange[2] += Math.cos(hd[0] * 2 * Math.PI) * (1 - normV);
				hueRange[3] += Math.cos(hd[0] * 2 * Math.PI) * normV;
				if (normV < 0.5) {
					satRange[0] += hd[1];
					srdiv[0]++;
				} else {
					satRange[1] += hd[1];
					srdiv[1]++;
				}
			}

			hueRange[0] = Math.atan2(hueRange[0], hueRange[2]) / 2.0f / Math.PI;
			hueRange[1] = Math.atan2(hueRange[1], hueRange[3]) / 2.0f / Math.PI;
			satRange[0] /= srdiv[0];
			satRange[1] /= srdiv[1];
		}

		int[] texData = new int[texture.length];
		for (int iy = 0; iy < height; iy++) {
			for (int ix = 0; ix < width; ix++) {
				int i = iy*width+ix;
				int it = texture[i];
				int ibu = getRGB(overlay)[(iy % overlay.getHeight())*overlay.getWidth() + (ix % overlay.getWidth())];

				float[] hsbTex = toHSL(it);
				float[] hsbBu = toHSL(ibu);
				double normV = (double) (hsbTex[2] - contrastBasedUpon[0]) / contrastBasedUpon[1];
				float v = (float) ((normV * contrastFrom[1]) + contrastFrom[0]);

				if (mode == UCWBlockRule.BlendMode.BLEND || (hsbBu[2] < 0.1 && hsbBu[1] < 0.1 && avgSatFrom >= 0.3)) {
					hsbBu[0] = (float) avgHueFrom;
					hsbBu[1] = (float) avgSatFrom;
				} else if (mode == UCWBlockRule.BlendMode.PLANK) {
					double nv2 = normV;
					if (nv2 < 0) nv2 = 0;
					if (nv2 > 1) nv2 = 1;
					hsbBu[0] = (float) (Math.atan2(
							(Math.sin(hueRange[1] * 2 * Math.PI) * nv2) + (Math.sin(hueRange[0] * 2 * Math.PI) * (1 - nv2)),
							(Math.cos(hueRange[1] * 2 * Math.PI) * nv2) + (Math.cos(hueRange[0] * 2 * Math.PI) * (1 - nv2))
					) / 2.0f / Math.PI);
					hsbBu[1] = (float) ((satRange[1] * nv2) + (satRange[0] * (1 - nv2)));
				}

				if (v < 0) v = 0;
				if (v > 1) v = 1;
				texData[i] = fromHSL(new float[]{hsbBu[0], hsbBu[1], v});
			}
		}
		return texData;
	}
}
