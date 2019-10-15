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

import com.google.common.collect.ImmutableList;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.TRSRTransformation;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;

public final class UCWMagic {
	private static class FakeSprite extends TextureAtlasSprite {
		protected FakeSprite(ResourceLocation spriteName) {
			super(spriteName.toString());
			setIconWidth(16);
			setIconHeight(16);
			initSprite(16, 16, 0, 0, false);
			setFramesTextureData(fakeTextureFrameList);
		}
	}

	private static final List<int[][]> fakeTextureFrameList;
	public static final BufferedImage missingNo;

	static {
		fakeTextureFrameList = ImmutableList.of(new int[1][256]);

		missingNo = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < 256; i++) {
			missingNo.setRGB(i & 15, i >> 4, ((i ^ (i >> 4)) & 8) != 0 ? 0xFF000000 : 0xFFFF00FF);
		}
	}

	private UCWMagic() {

	}

	public static ResourceLocation getLocation(IBlockState state, ModelResourceLocation location, IModel model) {
		String domain = location.getNamespace();

		if ("forestry".equals(domain)) {
			String[] name = state.getBlock().getRegistryName().getPath().split("\\.", 2);
			IProperty variantProp = state.getBlock().getBlockState().getProperty("variant");
			if (variantProp != null) {
				String variant = variantProp.getName(state.getValue(variantProp));
				if (name.length == 2 && ("planks".equals(name[0]))) {
					return new ResourceLocation("forestry", "blocks/wood/" + name[0] + "." + variant);
				}
			}
		} else if ("extratrees".equals(domain)) {
			String[] name = state.getBlock().getRegistryName().getPath().split("\\.", 2);
			IProperty variantProp = state.getBlock().getBlockState().getProperty("variant");
			if (variantProp != null) {
				String variant = variantProp.getName(state.getValue(variantProp));
				if (name.length == 2 && ("planks".equals(name[0]))) {
					return new ResourceLocation("extratrees", "blocks/planks/" + variant);
				}
			}
		}

		if (model == null) {
			UnlimitedChiselWorks.LOGGER.error("Could not find model for " + location + " (" + state.getBlock() + ")");
			return TextureMap.LOCATION_MISSING_TEXTURE;
		}

		//noinspection ConstantConditions
		boolean hasTextureList = model.getTextures() != null;

		if (hasTextureList && model.getTextures().size() == 1) {
			return model.getTextures().iterator().next();
		} else {
			try {
				// some mods have a skewed particle texture
				IBakedModel bakedModel = model.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, FakeSprite::new);
				Collection<BakedQuad> quadList = bakedModel.getQuads(state, EnumFacing.NORTH, 0);
				if (quadList.size() > 0) {
					return new ResourceLocation(quadList.iterator().next().getSprite().getIconName());
				}
			} catch (Exception e) {
				// pass
			}

			// fallback
			if (hasTextureList) {
				return model.getTextures().iterator().next();
			} else {
				UnlimitedChiselWorks.LOGGER.error("Bug? Model for " + location + " (" + state.getBlock() + ") provides null texture list!");
				return TextureMap.LOCATION_MISSING_TEXTURE;
			}
		}
	}

	private static float toLuma(int rgb) {
		return UCWColorspaceUtils.sRGBtoLuma(UCWColorspaceUtils.fromInt(rgb));
	}

	private static float[] toLAB(int rgb) {
		return UCWColorspaceUtils.XYZtoLAB(UCWColorspaceUtils.sRGBtoXYZ(UCWColorspaceUtils.fromInt(rgb)));
	}

	private static int fromLAB(float[] lab) {
		return UCWColorspaceUtils.asInt(UCWColorspaceUtils.XYZtosRGB(UCWColorspaceUtils.LABtoXYZ(lab)));
	}

	private static float[] calculateContrast(int[] data) {
		float[] contrast = new float[] { Float.MAX_VALUE, Float.MIN_VALUE };

		for (int i : data) {
			float d = toLuma(i);
			if (contrast[0] > d) contrast[0] = d;
			if (contrast[1] < d) contrast[1] = d;
		}

		contrast[1] -= contrast[0];
		return contrast;
	}

	public static int[][] createBaseForColorMultiplier(TextureAtlasSprite texture, boolean keepTinting) {
		int[][] frames = new int[texture.getFrameCount()][texture.getIconWidth() * texture.getIconHeight()];

		int divider = texture.getIconWidth() * texture.getIconHeight() * texture.getFrameCount();
		double avgLuma = 0.0;

		for (int i = 0; i < texture.getFrameCount(); i++) {
			for (int v : texture.getFrameTextureData(i)[0]) {
				avgLuma += toLuma(v);
			}
		}
		avgLuma /= divider;

		for (int i = 0; i < texture.getFrameCount(); i++) {
			int[] data = texture.getFrameTextureData(i)[0];
			for (int j = 0; j < texture.getIconWidth() * texture.getIconHeight(); j++) {
				float[] hd = toLAB(data[j]);

				frames[i][j] = (data[j] & 0xFF000000) | fromLAB(new float[]{(float) (hd[0] / avgLuma * 100.0), keepTinting ? hd[1] : 0, keepTinting ? hd[2] : 0});
			}
		}

		return frames;
	}

	public static int[] transform(TextureAtlasSprite sprite, int frame, TextureAtlasSprite from, TextureAtlasSprite overlay, TextureAtlasSprite basedUpon, UCWBlockRule.BlendMode mode) {
		int[] texture = sprite.getFrameTextureData(frame)[0];
		int width = sprite.getIconWidth();
		int height = sprite.getIconHeight();
		float[] contrastFrom = calculateContrast(from.getFrameTextureData(0)[0]);
		float[] contrastBasedUpon = calculateContrast(basedUpon.getFrameTextureData(0)[0]);
		double avgA = 0.0, avgB = 0.0;
		double[] rangeA = new double[2], rangeB = new double[2];
		int[] rangeDiv = new int[2];

		if (mode == UCWBlockRule.BlendMode.PLANK) {
			for (int i : from.getFrameTextureData(0)[0]) {
				float[] hd = toLAB(i);
				double normV = (double) (hd[0] - contrastFrom[0]) / contrastFrom[1];
				if (normV < 0.5) {
					rangeA[0] += hd[1];
					rangeB[0] += hd[2];
					rangeDiv[0]++;
				} else {
					rangeA[1] += hd[1];
					rangeB[1] += hd[2];
					rangeDiv[1]++;
				}
			}

			if (rangeDiv[0] > 0) {
				rangeA[0] /= rangeDiv[0];
				rangeB[0] /= rangeDiv[0];
			}

			if (rangeDiv[1] > 0) {
				rangeA[1] /= rangeDiv[1];
				rangeB[1] /= rangeDiv[1];
			}
		}

		if (mode == UCWBlockRule.BlendMode.BLEND) {
			for (int i : from.getFrameTextureData(0)[0]) {
				float[] data = toLAB(i);
				avgA += data[1];
				avgB += data[2];
			}

			avgA /= from.getIconWidth() * from.getIconHeight();
			avgB /= from.getIconWidth() * from.getIconHeight();
		}

		int[] texData = new int[texture.length];
		for (int iy = 0; iy < height; iy++) {
			for (int ix = 0; ix < width; ix++) {
				int i = iy*width+ix;
				int it = texture[i];
				int ibu = overlay.getFrameTextureData(0)[0][(iy % from.getIconHeight())*from.getIconWidth() + (ix % from.getIconWidth())];

				float[] hsbTex = toLAB(it);
				float[] hsbBu = toLAB(ibu);
				double normV;
				float v;

				if (contrastBasedUpon[1] != 0.0) {
					normV = (double) (hsbTex[0] - contrastBasedUpon[0]) / contrastBasedUpon[1];
				} else {
					normV = 0.5f;
				}
				v = (float) ((normV * contrastFrom[1]) + contrastFrom[0]);

				if (mode == UCWBlockRule.BlendMode.BLEND) {
					hsbBu[1] = (float) avgA;
					hsbBu[2] = (float) avgB;
				} else if (mode == UCWBlockRule.BlendMode.PLANK) {
					double nv2 = normV;
					if (nv2 < 0) nv2 = 0;
					else if (nv2 > 1) nv2 = 1;
					hsbBu[1] = (float) ((rangeA[1] * nv2) + (rangeA[0] * (1 - nv2)));
					hsbBu[2] = (float) ((rangeB[1] * nv2) + (rangeB[0] * (1 - nv2)));
				}

				if (v < 0) v = 0;
				else if (v > 100) v = 100;
				texData[i] = (it & 0xFF000000) | fromLAB(new float[]{v, hsbBu[1], hsbBu[2]});
			}
		}
		return texData;
	}
}
