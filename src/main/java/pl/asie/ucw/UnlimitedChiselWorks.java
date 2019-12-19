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
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.VersionParser;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Mod(modid = UnlimitedChiselWorks.MODID, version = UnlimitedChiselWorks.VERSION,
        dependencies = "after:forge@[14.23.5.2838,);after:undergroundbiomes",
        updateJSON = "http://asie.pl/files/minecraft/update/" + UnlimitedChiselWorks.MODID + ".json")
public final class UnlimitedChiselWorks {
    public static final String MODID = "unlimitedchiselworks";
    public static final String VERSION = "${version}";
    public static final Set<UCWBlockRule> BLOCK_RULES = new LinkedHashSet<>();
    public static final Set<String> GROUP_RULE_NAMES = new HashSet<>();
    public static final Set<UCWGroupRule> GROUP_RULES = new LinkedHashSet<>();
    public static boolean useChiselGetSubItemsWorkaround = false;
    public static Logger LOGGER;
    public static Random RAND = new Random();
    static final Gson GSON = new Gson();
    private static Configuration CONFIG;
    private static ConfigCategory C_ENABLED;
    private static ConfigCategory C_ENABLED_GROUPS;
    private static File configDir;

    private boolean enableDebugFeatures;

    @SidedProxy(clientSide = "pl.asie.ucw.UCWProxyClient", serverSide = "pl.asie.ucw.UCWProxyCommon")
    public static UCWProxyCommon proxy;

    private boolean loadLate;
    private final List<JsonObject> objectsLoadLate = new ArrayList<>();
    private final Object proposeObjectSync = new Object();

    private boolean proposeObject(JsonObject json) {
        boolean result = false;
        if (json != null) {
            boolean jsonLoadLate = false;
            if (json.has("loadLate")) {
                jsonLoadLate = json.get("loadLate").getAsBoolean();
            }

            if (jsonLoadLate != loadLate) {
                if (!loadLate) {
                    synchronized (objectsLoadLate) {
                        objectsLoadLate.add(json);
                    }
                }
                return result;
            }

            if (json.has("modid")) {
                JsonElement element = json.get("modid");
                if (element.isJsonArray()) {
                    JsonArray array = element.getAsJsonArray();
                    for (int i = 0; i < array.size(); i++) {
                        if (!Loader.isModLoaded(array.get(i).getAsString())) {
                            return result;
                        }
                    }
                } else {
                    if (!Loader.isModLoaded(element.getAsString())) {
                        return result;
                    }
                }
            }

            if (json.has("blocks")) {
                for (JsonElement element : json.get("blocks").getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        try {
                            UCWBlockRule rule = new UCWBlockRule(element.getAsJsonObject());
                            if (rule.isValid()) {
                                String fbName = rule.fromBlock.getRegistryName().toString();

                                synchronized (proposeObjectSync) {
                                    if (!C_ENABLED.containsKey(fbName)) {
                                        Property prop = new Property(fbName, "true", Property.Type.BOOLEAN);
                                        C_ENABLED.put(fbName, prop);
                                    }

                                    if (C_ENABLED.get(fbName).getBoolean()) {
                                        if (BLOCK_RULES.contains(rule)) {
                                            LOGGER.warn("Duplicate rule found! " + rule);
                                        } else {
                                            BLOCK_RULES.add(rule);
                                            result = true;
                                        }
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
                            String fbName = rule.groupName;

                            synchronized (proposeObjectSync) {
                                if (GROUP_RULE_NAMES.contains(fbName)) {
                                    LOGGER.warn("Duplicate group name: " + fbName + "!");
                                } else {
                                    GROUP_RULE_NAMES.add(fbName);
                                    result = true;
                                }

                                if (!C_ENABLED_GROUPS.containsKey(fbName)) {
                                    Property prop = new Property(fbName, "true", Property.Type.BOOLEAN);
                                    C_ENABLED_GROUPS.put(fbName, prop);
                                }

                                if (C_ENABLED_GROUPS.get(fbName).getBoolean()) {
                                    GROUP_RULES.add(rule);
                                    result = true;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return result;
    }

    private boolean proposeRule(Path p) throws IOException {
        if (!Files.exists(p)) {
            return false;
        }

        if (Files.isDirectory(p)) {
            AtomicBoolean result = new AtomicBoolean();
            List<Path> paths;

            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(p)) {
                paths = StreamSupport.stream(dirStream.spliterator(), false).collect(Collectors.toList());
            }

            paths.parallelStream().forEach((pp) -> {
                try {
                    if (proposeRule(pp)) {
                        result.set(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            return result.get();
        } else {
            try (BufferedReader reader = Files.newBufferedReader(p, Charsets.UTF_8)) {
                JsonObject json = JsonUtils.fromJson(GSON, reader, JsonObject.class);
                return proposeObject(json);
            } catch (Exception e) {
                UnlimitedChiselWorks.LOGGER.error("Error parsing " + p.toString(), e);
                return false;
            }
        }
    }

    private boolean findRules() {
        final AtomicBoolean result = new AtomicBoolean();

        proxy.progressPush("UCW: scanning rules", 2);

        proxy.progressStep("config/ucwdefs");

        File dir = new File(configDir, "ucwdefs");
        if (dir.exists() && dir.isDirectory()) {
            try {
                if (proposeRule(dir.toPath())) {
                    result.set(true);
                }
            } catch (NoSuchFileException e) {
                // no problem with this one
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        proxy.progressStep("loaded mods");

        Loader.instance().getActiveModList().parallelStream().forEach((container) -> {
            File file = container.getSource();
            try {
                if (file.exists()) {
                    if (file.isDirectory()) {
                        File f = new File(file, "assets/" + container.getModId() + "/ucwdefs");
                        if (f.exists() && f.isDirectory()) {
                            if (proposeRule(f.toPath())) {
                                result.set(true);
                            }
                        }
                    } else {
                        FileSystem fs = FileSystems.newFileSystem(file.toPath(), null);
                        if (proposeRule(fs.getPath("assets/" + container.getModId() + "/ucwdefs"))) {
                            result.set(true);
                        }
                    }
                }
            } catch (NoSuchFileException e) {
                // no problem with this one
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        proxy.progressPop();
        if (result.get()) {
            LOGGER.info("So far, UCW found " + BLOCK_RULES.size() + " block rules and " + GROUP_RULES.size() + " group rules.");
            return true;
        } else {
            return false;
        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = LogManager.getLogger(MODID);
        CONFIG = new Configuration(event.getSuggestedConfigurationFile());
        configDir = event.getModConfigurationDirectory();

        if (FMLCommonHandler.instance().getSide() == Side.SERVER && Loader.isModLoaded("chisel")) {
            DefaultArtifactVersion requiredVersion = new DefaultArtifactVersion("chisel",
                    VersionParser.parseRange("[,MC1.12-0.0.14.18]"));

            if (requiredVersion.containsVersion(new DefaultArtifactVersion("chisel",
                    Loader.instance().getIndexedModList().get("chisel").getVersion()
            ))) {
                LOGGER.info("Buggy version of 1.12.x Chisel detected on dedicated server, enabling workaround.");
                useChiselGetSubItemsWorkaround = true;
            }
        }

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(proxy);
        proxy.preInit();

        C_ENABLED = CONFIG.getCategory("enabled");
        C_ENABLED_GROUPS = CONFIG.getCategory("enabled_groups");
        enableDebugFeatures = CONFIG.getBoolean("enableDebugFeatures", "general", false, "Whether or not to enable debug functionality.");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        loadLate = false;
        objectsLoadLate.clear();
        BLOCK_RULES.clear();

        findRules();

        if (CONFIG.hasChanged()) {
            CONFIG.save();
        }

        for (UCWBlockRule rule : BLOCK_RULES) {
            rule.registerBlocks(event.getRegistry());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void registerBlocksLate(RegistryEvent.Register<Block> event) {
        loadLate = true;

        boolean result = false;
        for (JsonObject o : objectsLoadLate) {
            result |= proposeObject(o);
        }

        if (result) {
            for (UCWBlockRule rule : BLOCK_RULES) {
                rule.registerBlocks(event.getRegistry());
            }
        }

        if (CONFIG.hasChanged()) {
            CONFIG.save();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void registerItems(RegistryEvent.Register<Item> event) {
        for (UCWBlockRule rule : BLOCK_RULES) {
            rule.registerItems(event.getRegistry());
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        for (UCWBlockRule rule : BLOCK_RULES) {

            for (int i = 0; i < rule.from.size(); i++) {
                IBlockState fromState = rule.from.get(i);
                if (fromState == null) continue;

                UCWObjectFactory factory = rule.objectFactories.get(i);
                String groupName = rule.group + "_" + fromState.getBlock().getMetaFromState(fromState);
                NonNullList<ItemStack> stacks = NonNullList.create();
                factory.getItem().getSubItems(CreativeTabs.SEARCH, stacks);

                if (factory.getBlock() instanceof IUCWCustomVariantHandler) {
                    ((IUCWCustomVariantHandler) factory.getBlock()).registerVariants(groupName, fromState, stacks);
                } else {
                    UCWCompatUtils.addChiselVariation(groupName, new ItemStack(fromState.getBlock(), 1, fromState.getBlock().damageDropped(fromState)));

                    for (ItemStack stack : stacks) {
                        UCWCompatUtils.addChiselVariation(groupName, stack);
                    }
                }
            }
        }

        for (UCWGroupRule rule : GROUP_RULES) {
            for (IBlockState state : rule.states) {
                if (state == null) continue;

                UCWCompatUtils.addChiselVariation(rule.groupName, new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state)));
            }
        }

        if (CONFIG.hasChanged()) {
            CONFIG.save();
        }
    }

    @EventHandler
    public void postInit(FMLInitializationEvent event) {
        Map<Block, int[]> oreIdMap = new HashMap<>();
        for (UCWBlockRule rule : BLOCK_RULES) {
            int[] oreIds = oreIdMap.computeIfAbsent(rule.fromBlock, b -> {
                ItemStack stack = new ItemStack(b, 1, OreDictionary.WILDCARD_VALUE);
                if (!stack.isEmpty()) {
                    return OreDictionary.getOreIDs(stack);
                } else {
                    return new int[0];
                }
            });
            if (oreIds.length > 0) {
                for (UCWObjectFactory factory : rule.objectFactories.valueCollection()) {
                    if (factory.isBlockRegistered()) {
                        for (int i : oreIds) {
                            OreDictionary.registerOre(OreDictionary.getOreName(i), new ItemStack(factory.getBlock(), 1, OreDictionary.WILDCARD_VALUE));
                        }
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
