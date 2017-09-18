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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Mod(modid = UnlimitedChiselWorks.MODID, version = UnlimitedChiselWorks.VERSION, dependencies = "after:*", updateJSON = "http://asie.pl/files/minecraft/update/" + UnlimitedChiselWorks.MODID + ".json")
public class UnlimitedChiselWorks {
    public static final String MODID = "unlimitedchiselworks";
    public static final String VERSION = "${version}";
    public static final Set<UCWBlockRule> BLOCK_RULES = new LinkedHashSet<>();
    public static final TObjectIntMap<Block> RULE_COMBINATIONS = new TObjectIntHashMap<>();
    public static final Set<UCWGroupRule> GROUP_RULES = new LinkedHashSet<>();
    public static Logger LOGGER;
    public static Random RAND = new Random();
    protected static final Gson GSON = new Gson();
    private static Configuration CONFIG;
    private static ConfigCategory C_ENABLED;

    private boolean enableDebugFeatures;

    @SidedProxy(clientSide = "pl.asie.ucw.UCWProxyClient", serverSide = "pl.asie.ucw.UCWProxyCommon")
    public static UCWProxyCommon proxy;

    private void proposeRule(Path p) throws IOException {
        if (Files.isDirectory(p)) {
            for (Path pp : Files.newDirectoryStream(p)) {
                try {
                    proposeRule(pp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            BufferedReader reader = Files.newBufferedReader(p, Charsets.UTF_8);
            try {
                JsonObject json = UnlimitedChiselWorks.GSON.fromJson(reader, JsonElement.class).getAsJsonObject();
                if (json != null) {
                    if (json.has("blocks")) {
                        for (JsonElement element : json.get("blocks").getAsJsonArray()) {
                            if (element.isJsonObject()) {
                                try {
                                    UCWBlockRule rule = new UCWBlockRule(element.getAsJsonObject());
                                    if (rule.isValid()) {
                                        String fbName = rule.fromBlock.getRegistryName().toString();
                                        if (!C_ENABLED.containsKey(fbName)) {
                                            Property prop = new Property(fbName, "true", Property.Type.BOOLEAN);
                                            C_ENABLED.put(fbName, prop);
                                        }

                                        if (C_ENABLED.get(fbName).getBoolean()) {
                                            if (BLOCK_RULES.contains(rule)) {
                                                LOGGER.warn("Duplicate rule found! " + rule);
                                            } else {
                                                BLOCK_RULES.add(rule);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    if (json.has("groups")) {
                        for (JsonElement element : json.get("groups").getAsJsonArray()) {
                            if (element.isJsonObject()) {
                                try {
                                    UCWGroupRule rule = new UCWGroupRule(element.getAsJsonObject());
                                    GROUP_RULES.add(rule);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void findRules() {
        BLOCK_RULES.clear();

        proxy.progressPush("UCW: scanning rules", Loader.instance().getActiveModList().size());

        for (ModContainer container : Loader.instance().getActiveModList()) {
            proxy.progressStep(container.getName() == null ? container.getModId() : container.getName());

            File file = container.getSource();
            try {
                if (file.exists()) {
                    if (file.isDirectory()) {
                        File f = new File(file, "assets/" + container.getModId() + "/ucwdefs");
                        if (f.exists() && f.isDirectory()) {
                            proposeRule(f.toPath());
                        }
                    } else {
                        FileSystem fs = FileSystems.newFileSystem(file.toPath(), null);
                        proposeRule(fs.getPath("assets/" + container.getModId() + "/ucwdefs"));
                    }
                }
            } catch (NoSuchFileException e) {
                // no problem with this one
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        proxy.progressPop();
        LOGGER.info("Found " + BLOCK_RULES.size() + " block rules.");
        LOGGER.info("Found " + GROUP_RULES.size() + " group rules.");
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = LogManager.getLogger(MODID);
        CONFIG = new Configuration(event.getSuggestedConfigurationFile());

        MinecraftForge.EVENT_BUS.register(this);

        C_ENABLED = CONFIG.getCategory("enabled");
        enableDebugFeatures = CONFIG.getBoolean("enableDebugFeatures", "general", false, "Whether or not to enable debug functionality.");

        findRules();

        if (CONFIG.hasChanged()) {
            CONFIG.save();
        }

        for (UCWBlockRule rule : BLOCK_RULES) {
            rule.registerBlocks(ForgeRegistries.BLOCKS);
            rule.registerItems(ForgeRegistries.ITEMS);
        }

        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        for (UCWBlockRule rule : BLOCK_RULES) {
            RULE_COMBINATIONS.adjustOrPutValue(rule.fromBlock, rule.fromCount, rule.fromCount);
        }

        for (UCWBlockRule rule : BLOCK_RULES) {
            for (int i = 0; i < rule.from.size(); i++) {
                IBlockState fromState = rule.from.get(i);
                if (fromState == null) continue;

                String groupName = RULE_COMBINATIONS.get(rule.fromBlock) == 1 ? rule.group : rule.group + "_" + fromState.getBlock().getMetaFromState(fromState);
                UCWCompatUtils.addChiselVariation(groupName, new ItemStack(fromState.getBlock(), 1, fromState.getBlock().damageDropped(fromState)));

                UCWObjectFactory factory = rule.objectFactories.get(i);
                List<ItemStack> stacks = new ArrayList<>();
                proxy.getSubItemsUCW((IUCWItem) factory.item, stacks);
                for (ItemStack stack : stacks) {
                    UCWCompatUtils.addChiselVariation(groupName, stack);
                }
            }
        }

        for (UCWGroupRule rule : GROUP_RULES) {
            for (IBlockState state : rule.states) {
                if (state == null) continue;

                UCWCompatUtils.addChiselVariation(rule.groupName, new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state)));
            }
        }

        proxy.init();

        if (CONFIG.hasChanged()) {
            CONFIG.save();
        }
    }

    @EventHandler
    public void postInit(FMLInitializationEvent event) {
        for (UCWBlockRule rule : BLOCK_RULES) {
            ItemStack stack = new ItemStack(rule.fromBlock, 1, OreDictionary.WILDCARD_VALUE);
            int[] ids = OreDictionary.getOreIDs(stack);
            if (ids.length > 0) {
                for (UCWObjectFactory factory : rule.objectFactories.valueCollection()) {
                    for (int i : ids) {
                        OreDictionary.registerOre(OreDictionary.getOreName(i), factory.block);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        if (enableDebugFeatures) {
            event.registerServerCommand(new CommandUCWDebug());
        }
    }
}
