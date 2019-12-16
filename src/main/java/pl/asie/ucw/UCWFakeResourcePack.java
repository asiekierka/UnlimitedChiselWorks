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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: Rewrite this mess
public class UCWFakeResourcePack implements IResourcePack, IResourceManagerReloadListener {
	public static final UCWFakeResourcePack INSTANCE = new UCWFakeResourcePack();

	private final Map<ResourceLocation, byte[]> data = new HashMap<>();
	private final Map<ResourceLocation, JsonElement> jsonCache = new HashMap<>();
	private final Set<String> domains = ImmutableSet.of("ucw_generated");
	private final Minecraft mc = Minecraft.getMinecraft();

	private UCWFakeResourcePack() {

	}

	public JsonElement parseJsonElement(String[] str, JsonElement element) {
		if (element.isJsonObject()) {
			JsonObject parent = element.getAsJsonObject();
			JsonObject object = new JsonObject();
			for (Map.Entry<String, JsonElement> entry : parent.entrySet()) {
				object.add(entry.getKey(), parseJsonElement(str, entry.getValue()));
			}
			return object;
		} else if (element.isJsonArray()) {
			JsonArray parent = element.getAsJsonArray();
			JsonArray array = new JsonArray();
			for (JsonElement element1 : parent) {
				array.add(parseJsonElement(str, element1));
			}
			return array;
		} else if (!element.isJsonNull()) {
			String s = element.getAsString();
			if (s != null && s.startsWith(str[1] + ":")) {
				String ns = UCWUtils.toUcwGenerated(new ResourceLocation(s), str[0]).toString();
				// System.out.println(s + " -> " + ns);
				return new JsonPrimitive(ns);
			} else {
				return element;
			}
		} else {
			return element;
		}
	}

	@Override
	public InputStream getInputStream(ResourceLocation location) throws IOException {
		if (location.getPath().endsWith(".png")) {
			return new ByteArrayInputStream(new byte[0]);
		}

		if (!data.containsKey(location)) {
			String[] str = UCWUtils.getUcwLocationData(location);
			JsonElement element;
			ResourceLocation nonProxiedLoc = new ResourceLocation(str[1], str[2]);

			if (jsonCache.containsKey(nonProxiedLoc)) {
				element = jsonCache.get(nonProxiedLoc);
			} else {
				try (
					IResource nonProxiedResource = mc.getResourceManager().getResource(nonProxiedLoc);
					InputStream nonProxied = nonProxiedResource.getInputStream();
					Reader reader = new InputStreamReader(nonProxied)
				) {
					element = JsonUtils.fromJson(UnlimitedChiselWorks.GSON, reader, JsonElement.class);
				} catch (Exception e) {
					element = null;
				}
			}

			byte[] out;

			if (element != null) {
				JsonElement newElement = parseJsonElement(str, element);
				out = UnlimitedChiselWorks.GSON.toJson(newElement).getBytes(Charsets.UTF_8);
			} else {
				if (data.containsKey(nonProxiedLoc)) {
					out = data.get(nonProxiedLoc);
				} else {
					try (
							IResource nonProxiedResource = mc.getResourceManager().getResource(nonProxiedLoc);
							InputStream nonProxied = nonProxiedResource.getInputStream()
					) {
						out = ByteStreams.toByteArray(nonProxied);
						data.put(nonProxiedLoc, out);
					} catch (Exception e) {
						out = null;
					}
				}
			}

			data.put(location, out);
			return new ByteArrayInputStream(out);
		} else {
			return new ByteArrayInputStream(data.get(location));
		}
	}

	@Override
	public boolean resourceExists(ResourceLocation location) {
		if (location.getPath().endsWith(".png")) {
			return true;
		}
		
		String[] str = UCWUtils.getUcwLocationData(location);
		if (str == null || str[1].isEmpty()) return false;

		try (IResource resource = mc.getResourceManager().getResource(
				new ResourceLocation(str[1], str[2])
		)) {
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public Set<String> getResourceDomains() {
		return domains;
	}

	@Nullable
	@Override
	public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
		return null;
	}

	@Override
	public BufferedImage getPackImage() throws IOException {
		return null;
	}

	@Override
	public String getPackName() {
		return "UCWFakePack";
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		invalidate();
	}

	public void invalidate() {
		data.clear();
		jsonCache.clear();
	}
}
