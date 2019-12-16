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
