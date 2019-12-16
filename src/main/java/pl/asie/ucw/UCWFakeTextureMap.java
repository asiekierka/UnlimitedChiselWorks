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

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Function;

class UCWFakeTextureMap extends TextureMap {
    private final TextureMap delegate;
    private final Function<ResourceLocation, TextureAtlasSprite> spriteFactory;

    UCWFakeTextureMap(TextureMap delegate, Function<ResourceLocation, TextureAtlasSprite> spriteFactory) {
        super(delegate.getBasePath());
        this.delegate = delegate;
        this.spriteFactory = spriteFactory;
    }

    @Override
    public int getMipmapLevels() {
        return delegate.getMipmapLevels();
    }

    @Override
    public TextureAtlasSprite getAtlasSprite(String iconName) {
        return delegate.getAtlasSprite(iconName);
    }

    @Override
    public TextureAtlasSprite getMissingSprite() {
        return delegate.getMissingSprite();
    }

    @Nullable
    @Override
    public TextureAtlasSprite getTextureExtry(String name) {
        return delegate.getTextureExtry(name);
    }

    @Override
    public TextureAtlasSprite registerSprite(ResourceLocation location) {
        TextureAtlasSprite sprite = spriteFactory.apply(location);
        this.setTextureEntry(sprite);
        return sprite;
    }

    @Override
    public boolean setTextureEntry(TextureAtlasSprite entry) {
        return delegate.setTextureEntry(entry);
    }
}
