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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;

import java.lang.reflect.Field;
import java.util.*;

public final class UCWVanillaModelRemapper {
    private static final Class vanillaModelWrapperClass;
    private static final Class weightedRandomModelClass;
    private static final Field vmwModelBlockField;
    private static final Field wrmModelsField;
    private static final Field wrmTexturesField;

    static {
        try {
            vanillaModelWrapperClass = Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper");
            weightedRandomModelClass = Class.forName("net.minecraftforge.client.model.ModelLoader$WeightedRandomModel");
            vmwModelBlockField = vanillaModelWrapperClass.getDeclaredField("model");
            vmwModelBlockField.setAccessible(true);
            wrmModelsField = weightedRandomModelClass.getDeclaredField("models");
            wrmModelsField.setAccessible(true);
            wrmTexturesField = weightedRandomModelClass.getDeclaredField("textures");
            wrmTexturesField.setAccessible(true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private UCWVanillaModelRemapper() {

    }

    private static ModelBlock clone(ModelBlock orig) {
        return new ModelBlock(orig.getParentLocation(), orig.getElements(),
                new HashMap<>(orig.textures), orig.isAmbientOcclusion(), orig.isGui3d(),
                orig.getAllTransforms(), orig.getOverrides());
    }

    private static IModel hackyClone(IModel orig) {
        return orig.retexture(ImmutableMap.of("\ufd00\ufd01\ufd02\ufd03", "this does not exist"));
    }

    public static IModel retexture(ImmutableMap<String, String> textures, IModel model) {
        try {
            if (model.getClass() == vanillaModelWrapperClass) {
                IModel newModel = hackyClone(model);
                ModelBlock modelBlock = (ModelBlock) vmwModelBlockField.get(newModel);

                while (modelBlock != null) {
                    Map<String, String> replacements = new HashMap<>();
                    for (Map.Entry<String, String> entry : modelBlock.textures.entrySet()) {
                        if (textures.containsKey(entry.getValue())) {
                            replacements.put(entry.getKey(), textures.get(entry.getValue()));
                        }
                    }
                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                        modelBlock.textures.put(entry.getKey(), entry.getValue());
                    }

                    // clone parent first - Forge's retexture() does not
                    // TODO: optimize memory usage by not cloning everything
                    if (modelBlock.parent != null) {
                        modelBlock.parent = clone(modelBlock.parent);
                    }

                    modelBlock = modelBlock.parent;
                }

                return newModel;
            } else if (model.getClass() == weightedRandomModelClass) {
                IModel newModel = hackyClone(model);
                List<IModel> models = (List<IModel>) wrmModelsField.get(newModel);
                for (int i = 0; i < models.size(); i++) {
                    IModel submodel = models.get(i);
                    if (submodel != null) {
                        IModel newSubmodel = retexture(textures, submodel);
                        if (newSubmodel == null) {
                            newSubmodel = submodel.retexture(textures);
                        }
                        models.set(i, newSubmodel);
                    }
                }

                Collection<ResourceLocation> newModelTextures = (Collection<ResourceLocation>) wrmTexturesField.get(newModel);
                for (String s : textures.values()) {
                    if (s.indexOf(':') > 0) {
                        newModelTextures.add(new ResourceLocation(s));
                    }
                }

                return newModel;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
