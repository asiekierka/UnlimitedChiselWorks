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

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.model.IModelState;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.*;
import java.util.function.Function;

public class TintApplyingModel implements IModel {
	public static class IOL extends ItemOverrideList {
		private final ItemOverrideList parent;

		public IOL(ItemOverrideList parent) {
			super(Collections.emptyList());
			this.parent = parent;
		}

		@Override
		public ResourceLocation applyOverride(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
			return parent.applyOverride(stack, worldIn, entityIn);
		}

		@Override
		public com.google.common.collect.ImmutableList<ItemOverride> getOverrides() {
			return parent.getOverrides();
		}

		@Override
		public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
			IBakedModel trueParent = originalModel;
			if (originalModel instanceof Baked) {
				trueParent = ((Baked) originalModel).parent;
			}
			IBakedModel model = parent.handleItemState(trueParent, stack, world, entity);
			if (model == trueParent) {
				return originalModel;
			} else {
				return new Baked(model);
			}
		}
	}

	public static class Baked implements IBakedModel {
		private final IBakedModel parent;

		public Baked(IBakedModel parent) {
			this.parent = parent;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
			List<BakedQuad> orig = parent.getQuads(state, side, rand);
			if (orig != null && !orig.isEmpty()) {
				List<BakedQuad> newList = new ArrayList<>(orig.size());
				for (BakedQuad q : orig) {
					if (!q.hasTintIndex()) {
						// TODO: Handle UBQs better
						newList.add(new BakedQuad(
								q.getVertexData(),
								0,
								q.getFace(),
								q.getSprite(),
								q.shouldApplyDiffuseLighting(),
								q.getFormat()
						));
					}
				}
				return newList;
			} else {
				return orig;
			}
		}

		@Override
		public boolean isAmbientOcclusion() {
			return parent.isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return parent.isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return parent.isBuiltInRenderer();
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return parent.getParticleTexture();
		}

		@Override
		public ItemOverrideList getOverrides() {
			return new IOL(parent.getOverrides());
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return parent.getItemCameraTransforms();
		}

		@Override
		public org.apache.commons.lang3.tuple.Pair<? extends IBakedModel, javax.vecmath.Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
			Pair<? extends IBakedModel, Matrix4f> pair = parent.handlePerspective(cameraTransformType);
			if (pair.getLeft() == parent) {
				return Pair.of(this, pair.getRight());
			} else {
				return Pair.of(new Baked(pair.getLeft()), pair.getRight());
			}
		}
	}

	private final IModel parent;

	public TintApplyingModel(IModel parent) {
		this.parent = parent;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return parent.getDependencies();
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return parent.getTextures();
	}

	@Override
	public IModelState getDefaultState() {
		return parent.getDefaultState();
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		return new Baked(parent.bake(state, format, bakedTextureGetter));
	}

	private IModel wrap(IModel other) {
		if (other == parent) {
			return this;
		} else {
			return new TintApplyingModel(other);
		}
	}

	@Override
	public IModel process(ImmutableMap<String, String> customData) {
		return wrap(parent.process(customData));
	}

	@Override
	public IModel smoothLighting(boolean value) {
		return wrap(parent.smoothLighting(value));
	}

	@Override
	public IModel gui3d(boolean value) {
		return wrap(parent.gui3d(value));
	}

	@Override
	public IModel uvlock(boolean value) {
		return wrap(parent.uvlock(value));
	}

	@Override
	public IModel retexture(ImmutableMap<String, String> textures) {
		return wrap(parent.retexture(textures));
	}
}
