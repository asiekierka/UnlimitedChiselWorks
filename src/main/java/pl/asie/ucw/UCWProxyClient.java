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
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

public class UCWProxyClient extends UCWProxyCommon {
	private Map<String, JsonObject> chiselCache = new HashMap<>();

	private JsonObject getChiselCache(ResourceLocation throughLoc) throws IOException {
		JsonObject obj = chiselCache.get("default");
		if (obj != null) return obj;

		obj = chiselCache.get(throughLoc.getPath());
		if (obj != null) return obj;

		IResourceManager manager = Minecraft.getMinecraft().getResourceManager();

		String resourceKey;
		IResource resource;
		try {
			resourceKey = "default";
			resource = manager.getResource(
					new ResourceLocation("chisel", "blockstates/default.json")
			);
		} catch (FileNotFoundException e) {
			resourceKey = throughLoc.getPath();
			resource = manager.getResource(
					new ResourceLocation("chisel", "blockstates/" + throughLoc.getPath() + ".json")
			);
		}

		try (InputStream stream = resource.getInputStream(); InputStreamReader reader = new InputStreamReader(stream)) {
			obj = JsonUtils.fromJson(UnlimitedChiselWorks.GSON, reader, JsonObject.class);
			chiselCache.put(resourceKey, obj);
			resource.close();
			return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void asLateAsPossible(ModelBakeEvent event) {
		chiselCache.clear();
		UCWFakeResourcePack.INSTANCE.invalidate();
	}

	@SubscribeEvent
	public void onModelRegistry(ModelRegistryEvent event) {
		chiselCache.clear();
	}

	@SubscribeEvent
	@SuppressWarnings("unchecked")
	public void onTextureStitchPre(TextureStitchEvent.Pre event) {
		if (event.getMap() != Minecraft.getMinecraft().getTextureMapBlocks()) {
			UnlimitedChiselWorks.LOGGER.warn("Mod called TextureStitchEvent.Pre for non-default texture atlas - this is not handled well by 1.12 mods; please hold off to 1.14+...");
			return;
		}

		// don't tell lex
		ModelLoader loader;
		Map<ModelResourceLocation, IModel> secretSauce = null;
		BlockModelShapes blockModelShapes = null;
		try {
			Class c = Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaLoader");
			Field f = c.getDeclaredField("INSTANCE");
			f.setAccessible(true);
			Object o = f.get(null);
			f = c.getDeclaredField("loader");
			f.setAccessible(true);
			loader = (ModelLoader) f.get(o);
			f = ModelLoader.class.getDeclaredField("stateModels");
			f.setAccessible(true);
			secretSauce = (Map<ModelResourceLocation, IModel>) f.get(loader);
			f = ObfuscationReflectionHelper.findField(ModelBakery.class, "field_177610_k");
			f.setAccessible(true);
			blockModelShapes = (BlockModelShapes) f.get(loader);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		UnlimitedChiselWorks.proxy.progressPush("UCW: generating models", UnlimitedChiselWorks.BLOCK_RULES.size());
		int cc = 0;

		for (UCWBlockRule rule : UnlimitedChiselWorks.BLOCK_RULES) {
			UnlimitedChiselWorks.proxy.progressStep(String.format("%d%%", (++cc) * 100 / UnlimitedChiselWorks.BLOCK_RULES.size()));

			Map<IBlockState, ModelResourceLocation> fromVariants = blockModelShapes.getBlockStateMapper().getVariants(rule.fromBlock);
			Map<IBlockState, ModelResourceLocation> overlayVariants = blockModelShapes.getBlockStateMapper().getVariants(rule.overlayBlock);
			Map<IBlockState, ModelResourceLocation> throughVariants = blockModelShapes.getBlockStateMapper().getVariants(rule.throughBlock);
			Map<IBlockState, ModelResourceLocation> basedUponVariants = blockModelShapes.getBlockStateMapper().getVariants(rule.basedUponBlock);

			for (int i = 0; i < rule.from.size(); i++) {
				if (rule.from.get(i) != null) {
					IBlockState state = rule.from.get(i);
					String s2 = rule.fromBlock.getRegistryName().toString().trim().replaceAll("[^A-Za-z0-9]", "_") + "_" + state.getBlock().getMetaFromState(state);

					IBlockState stateOverlay = rule.overlay.get(i);
					IModel modelFrom = secretSauce.get(fromVariants.get(state));
					IModel modelOverlay = secretSauce.get(overlayVariants.get(stateOverlay));
					IBlockState stateBasedUpon = rule.basedUpon.size() == 1 ? rule.basedUpon.get(0) : rule.basedUpon.get(i);
					IModel modelBasedUpon = secretSauce.get(basedUponVariants.get(stateBasedUpon));
					ResourceLocation textureFrom = UCWMagic.getLocation(state, fromVariants.get(state), modelFrom);
					ResourceLocation textureOverlay = UCWMagic.getLocation(stateOverlay, overlayVariants.get(stateOverlay), modelOverlay);
					ResourceLocation textureBasedUpon = UCWMagic.getLocation(stateBasedUpon, basedUponVariants.get(stateBasedUpon), modelBasedUpon);

					for (int j = 0; j < 16; j++) {
						IBlockState throughState = rule.through.get(j);
						if (throughState == null) continue;

						ModelResourceLocation throughLoc = throughVariants.get(throughState);
						IModel modelThrough = secretSauce.get(throughLoc);
						ImmutableMap.Builder<String, String> textureRemapMap = ImmutableMap.builder();
						for (ResourceLocation oldLocation : modelThrough.getTextures()) {
							ResourceLocation newLocation = new ResourceLocation("ucw_generated",
									"blocks/ucw_ucw_" + s2 + "/" + oldLocation.getNamespace() + "/" + oldLocation.getPath().substring(7));

							textureRemapMap.put(oldLocation.toString(), newLocation.toString());
							event.getMap().setTextureEntry(new TextureAtlasSprite(newLocation.toString()) {
								@Override
								public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
									return true;
								}

								@Override
								public boolean load(IResourceManager manager, ResourceLocation location, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
									TextureAtlasSprite fromTex = textureGetter.apply(textureFrom);
									TextureAtlasSprite overlayTex = textureGetter.apply(textureOverlay);
									TextureAtlasSprite basedUponTex = textureGetter.apply(textureBasedUpon);
									TextureAtlasSprite locationTex = textureGetter.apply(oldLocation);

									setIconWidth(locationTex.getIconWidth());
									setIconHeight(locationTex.getIconHeight());

									clearFramesTextureData();
									for (int i = 0; i < locationTex.getFrameCount(); i++) {
										int[][] pixels = new int[Minecraft.getMinecraft().gameSettings.mipmapLevels + 1][];
										pixels[0] = UCWMagic.transform(locationTex, i, fromTex, overlayTex, basedUponTex, rule.mode);
										framesTextureData.add(pixels);
									}

									return false;
								}

								@Override
								public java.util.Collection<ResourceLocation> getDependencies() {
									return ImmutableList.of(textureFrom, textureBasedUpon, oldLocation, textureOverlay);
								}
							});
						}

						UCWObjectFactory factory = rule.objectFactories.get(i);
						List<String> propertyNames = new ArrayList<>();
						for (IProperty property : factory.block.getBlockState().getProperties()) {
							propertyNames.add(property.getName());
						}
						IBlockState targetState = factory.block.getStateFromMeta(j);
						Collections.sort(propertyNames);
						StringBuilder variant = new StringBuilder();
						for (String s : propertyNames) {
							if (variant.length() > 0) variant.append(",");
							IProperty property = factory.block.getBlockState().getProperty(s);
							variant.append(s).append("=").append(property.getName(targetState.getValue(property)));
						}

						ModelResourceLocation targetLoc = new ModelResourceLocation(factory.block.getRegistryName(), variant.toString());
						ModelLoader.setCustomModelResourceLocation(factory.item, j, targetLoc);

						IModel target = null;

						if (throughLoc.getNamespace().equals("chisel")) {
							// fun!
							try {
								JsonObject variants = getChiselCache(throughLoc).get("variants").getAsJsonObject();
								if (variants.has(throughLoc.getVariant())) {
									String modelPath = variants
											.get(throughLoc.getVariant()).getAsJsonObject().get("model").getAsString();
									modelPath = modelPath.replaceFirst("chisel:", "ucw_generated:ucw_ucw_" + s2 + "/chisel/");
									target = ModelLoaderRegistry.getModel(new ModelResourceLocation(modelPath));
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							target = modelThrough.retexture(textureRemapMap.build());
						}

						if (target != null) {
							secretSauce.put(targetLoc, rule.hasColor() ? new TintApplyingModel(target) : target);
						}
					}
				}
			}
		}

		UnlimitedChiselWorks.proxy.progressPop();
	}

	@Override
	public void preInit() {
		try {
			Field field = ObfuscationReflectionHelper.findField(Minecraft.class, "field_110449_ao");
			((List) field.get(Minecraft.getMinecraft())).add(UCWFakeResourcePack.INSTANCE);

			((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(UCWFakeResourcePack.INSTANCE);

			// TODO: Can we get rid of this to save a bit of loading time?
			// (We can, but it involves loading Minecraft.<init> a bit early.
			// Hmm.)
			FMLClientHandler.instance().refreshResources(a -> false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	public void onColorHandlerRegister(ColorHandlerEvent.Item event) {
		for (UCWBlockRule rule : UnlimitedChiselWorks.BLOCK_RULES) {
			if (!rule.hasColor()) {
				continue;
			}

			for (int i = 0; i < rule.from.size(); i++) {
				IBlockState fromState = rule.from.get(i);
				if (fromState == null) continue;

				UCWObjectFactory factory = rule.objectFactories.get(i);

				Object o = UCWColorProxy.INSTANCE;
				if (rule.customColorClass != null) {
					try {
						o = Class.forName(rule.customColorClass).getConstructor().newInstance();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				if (!(o instanceof IBlockColor) || !(o instanceof IItemColor)) {
					throw new RuntimeException(o.getClass().getName() + " is not IBlockColor and IItemColor!");
				}

				event.getBlockColors().registerBlockColorHandler((IBlockColor) o, factory.block);
				event.getItemColors().registerItemColorHandler((IItemColor) o, factory.item);
			}
		}

	}

	private final Deque<ProgressManager.ProgressBar> progressBarDeque = new ArrayDeque<>();

	@Override
	public void progressPush(String name, int count) {
		progressBarDeque.addFirst(ProgressManager.push(name, count));
	}

	@Override
	public void progressStep(String text) {
		progressBarDeque.peekFirst().step(text);
	}

	@Override
	public void progressPop() {
		ProgressManager.pop(progressBarDeque.removeFirst());
	}
}
