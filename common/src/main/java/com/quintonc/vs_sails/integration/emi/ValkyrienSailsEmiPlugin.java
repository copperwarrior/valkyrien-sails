package com.quintonc.vs_sails.integration.emi;

import com.quintonc.vs_sails.ValkyrienSails;
import com.quintonc.vs_sails.blocks.WindFlagBlock;
import com.quintonc.vs_sails.recipes.WindFlagDyeRecipe;
import com.quintonc.vs_sails.recipes.WindFlagPatternRecipe;
import com.quintonc.vs_sails.recipes.WindFlagRecipeUtil;
import com.quintonc.vs_sails.registration.SailsBlocks;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiPatternCraftingRecipe;
import dev.emi.emi.api.recipe.EmiWorldInteractionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.GeneratedSlotWidget;
import dev.emi.emi.api.widget.SlotWidget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@EmiEntrypoint
public class ValkyrienSailsEmiPlugin implements EmiPlugin {
    private static final String BLOCK_STATE_TAG_KEY = "BlockStateTag";
    private static final List<Item> PATTERN_ITEMS = List.of(
            Items.SKULL_BANNER_PATTERN,
            Items.CREEPER_BANNER_PATTERN,
            Items.FLOWER_BANNER_PATTERN,
            Items.MOJANG_BANNER_PATTERN,
            Items.GLOBE_BANNER_PATTERN,
            Items.PIGLIN_BANNER_PATTERN
    );

    @Override
    public void register(EmiRegistry registry) {
        registerWindFlagCraftingRecipes(registry);
        registerWindFlagWorldInteractions(registry);
        registerMagmaWorldInteractions(registry);
    }

    private static void registerWindFlagCraftingRecipes(EmiRegistry registry) {
        for (CraftingRecipe recipe : registry.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (recipe instanceof WindFlagPatternRecipe) {
                registry.addRecipe(new EmiWindFlagPatternCraftingRecipe(recipe.getId()));
            } else if (recipe instanceof WindFlagDyeRecipe) {
                registry.addRecipe(new EmiWindFlagDyeCraftingRecipe(recipe.getId()));
            }
        }
    }

    private static void registerWindFlagWorldInteractions(EmiRegistry registry) {
        EmiIngredient anyWindFlag = windFlagIngredient();
        EmiIngredient anyPatternedWindFlag = patternedWindFlagIngredient();

        for (Item patternItem : PATTERN_ITEMS) {
            int patternId = patternIdFromPatternItem(patternItem);
            String patternPath = BuiltInRegistries.ITEM.getKey(patternItem).getPath();
            ItemStack output = makeWindFlagStack(
                    SailsBlocks.WIND_FLAG.get().asItem(),
                    patternId,
                    DyeColor.WHITE.getId(),
                    false
            );
            registry.addRecipe(EmiWorldInteractionRecipe.builder()
                    .id(id("world/wind_flag_pattern/" + patternPath))
                    .leftInput(anyWindFlag)
                    .rightInput(EmiStack.of(patternItem), false)
                    .output(EmiStack.of(output))
                    .supportsRecipeTree(false)
                    .build());
        }

        int samplePattern = patternIdFromPatternItem(Items.SKULL_BANNER_PATTERN);
        for (DyeColor dyeColor : DyeColor.values()) {
            Item dyeItem = dyeItemForColor(dyeColor);
            ItemStack output = makeWindFlagStack(
                    SailsBlocks.WIND_FLAG.get().asItem(),
                    samplePattern,
                    dyeColor.getId(),
                    false
            );
            registry.addRecipe(EmiWorldInteractionRecipe.builder()
                    .id(id("world/wind_flag_overlay/" + BuiltInRegistries.ITEM.getKey(dyeItem).getPath()))
                    .leftInput(anyPatternedWindFlag)
                    .rightInput(EmiStack.of(dyeItem), false)
                    .output(EmiStack.of(output))
                    .supportsRecipeTree(false)
                    .build());
        }

        ItemStack emissiveOutput = makeWindFlagStack(
                SailsBlocks.WIND_FLAG.get().asItem(),
                samplePattern,
                DyeColor.WHITE.getId(),
                true
        );
        registry.addRecipe(EmiWorldInteractionRecipe.builder()
                .id(id("world/wind_flag_glow_ink"))
                .leftInput(anyPatternedWindFlag)
                .rightInput(EmiStack.of(Items.GLOW_INK_SAC), false)
                .output(EmiStack.of(emissiveOutput))
                .supportsRecipeTree(false)
                .build());

        ItemStack jebNameTag = new ItemStack(Items.NAME_TAG);
        jebNameTag.setHoverName(Component.literal("jeb_"));
        ItemStack rainbowOutput = makeWindFlagStack(
                SailsBlocks.WIND_FLAG.get().asItem(),
                samplePattern,
                WindFlagBlock.OVERLAY_COLOR_RAINBOW,
                false
        );
        registry.addRecipe(EmiWorldInteractionRecipe.builder()
                .id(id("world/wind_flag_rainbow"))
                .leftInput(anyPatternedWindFlag)
                .rightInput(EmiStack.of(jebNameTag), false)
                .output(EmiStack.of(rainbowOutput))
                .supportsRecipeTree(false)
                .build());
    }

    private static void registerMagmaWorldInteractions(EmiRegistry registry) {
        EmiStack fireResistancePotion = EmiStack.of(
                PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.FIRE_RESISTANCE)
        );
        EmiStack splashWaterPotion = EmiStack.of(
                PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.WATER)
        );
        EmiStack waterPotion = EmiStack.of(
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)
        );
        EmiIngredient washingPotions = EmiIngredient.of(List.of(splashWaterPotion, waterPotion));

        for (BlockPair pair : magmaConvertiblePairs()) {
            ResourceLocation regularId = BuiltInRegistries.BLOCK.getKey(pair.regular());
            ResourceLocation magmaId = BuiltInRegistries.BLOCK.getKey(pair.magma());

            registry.addRecipe(EmiWorldInteractionRecipe.builder()
                    .id(id("world/magma_coating/" + regularId.getPath()))
                    .leftInput(EmiStack.of(pair.regular()))
                    .rightInput(fireResistancePotion, false)
                    .output(EmiStack.of(pair.magma()))
                    .build());

            registry.addRecipe(EmiWorldInteractionRecipe.builder()
                    .id(id("world/magma_washing/" + magmaId.getPath()))
                    .leftInput(EmiStack.of(pair.magma()))
                    .rightInput(washingPotions, false)
                    .output(EmiStack.of(pair.regular()))
                    .build());
        }
    }

    private static ResourceLocation id(String path) {
        if (path.startsWith("/")) {
            return new ResourceLocation(ValkyrienSails.MOD_ID, path);
        }
        return new ResourceLocation(ValkyrienSails.MOD_ID, "/" + path);
    }

    private static EmiIngredient windFlagIngredient() {
        List<EmiIngredient> flags = new ArrayList<>();
        for (Item flag : windFlagItems()) {
            flags.add(EmiStack.of(flag));
        }
        return EmiIngredient.of(flags);
    }

    private static EmiIngredient patternItemIngredient() {
        List<EmiIngredient> patterns = new ArrayList<>();
        for (Item patternItem : PATTERN_ITEMS) {
            patterns.add(EmiStack.of(patternItem));
        }
        return EmiIngredient.of(patterns);
    }

    private static EmiIngredient dyeIngredient() {
        List<EmiIngredient> dyes = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            dyes.add(EmiStack.of(dyeItemForColor(color)));
        }
        return EmiIngredient.of(dyes);
    }

    private static EmiIngredient patternedWindFlagIngredient() {
        int samplePattern = patternIdFromPatternItem(Items.SKULL_BANNER_PATTERN);
        List<EmiIngredient> flags = new ArrayList<>();
        for (Item flag : windFlagItems()) {
            flags.add(EmiStack.of(makeWindFlagStack(flag, samplePattern, DyeColor.WHITE.getId(), false)));
        }
        return EmiIngredient.of(flags);
    }

    private static List<Item> windFlagItems() {
        return List.of(
                SailsBlocks.WIND_FLAG.get().asItem(),
                SailsBlocks.BLACK_WIND_FLAG.get().asItem(),
                SailsBlocks.BROWN_WIND_FLAG.get().asItem(),
                SailsBlocks.CYAN_WIND_FLAG.get().asItem(),
                SailsBlocks.GRAY_WIND_FLAG.get().asItem(),
                SailsBlocks.GREEN_WIND_FLAG.get().asItem(),
                SailsBlocks.LIGHT_BLUE_WIND_FLAG.get().asItem(),
                SailsBlocks.BLUE_WIND_FLAG.get().asItem(),
                SailsBlocks.LIGHT_GRAY_WIND_FLAG.get().asItem(),
                SailsBlocks.LIME_WIND_FLAG.get().asItem(),
                SailsBlocks.MAGENTA_WIND_FLAG.get().asItem(),
                SailsBlocks.ORANGE_WIND_FLAG.get().asItem(),
                SailsBlocks.PINK_WIND_FLAG.get().asItem(),
                SailsBlocks.PURPLE_WIND_FLAG.get().asItem(),
                SailsBlocks.RED_WIND_FLAG.get().asItem(),
                SailsBlocks.WHITE_WIND_FLAG.get().asItem(),
                SailsBlocks.YELLOW_WIND_FLAG.get().asItem()
        );
    }

    private static List<BlockPair> magmaConvertiblePairs() {
        List<BlockPair> pairs = new ArrayList<>();
        List<Block> regularSails = List.of(
                SailsBlocks.SAIL_BLOCK.get(),
                SailsBlocks.WHITE_SAIL.get(),
                SailsBlocks.LIGHT_GRAY_SAIL.get(),
                SailsBlocks.GRAY_SAIL.get(),
                SailsBlocks.BLACK_SAIL.get(),
                SailsBlocks.BROWN_SAIL.get(),
                SailsBlocks.RED_SAIL.get(),
                SailsBlocks.ORANGE_SAIL.get(),
                SailsBlocks.YELLOW_SAIL.get(),
                SailsBlocks.LIME_SAIL.get(),
                SailsBlocks.GREEN_SAIL.get(),
                SailsBlocks.CYAN_SAIL.get(),
                SailsBlocks.LIGHT_BLUE_SAIL.get(),
                SailsBlocks.BLUE_SAIL.get(),
                SailsBlocks.PURPLE_SAIL.get(),
                SailsBlocks.MAGENTA_SAIL.get(),
                SailsBlocks.PINK_SAIL.get()
        );

        for (Block regularSail : regularSails) {
            Block magmaSail = SailsBlocks.getMagmaSailBlock(regularSail);
            if (magmaSail != null) {
                pairs.add(new BlockPair(regularSail, magmaSail));
            }
        }

        pairs.add(new BlockPair(SailsBlocks.ROPE_BLOCK.get(), SailsBlocks.MAGMA_ROPE_BLOCK.get()));
        pairs.add(new BlockPair(SailsBlocks.RIGGING_BLOCK.get(), SailsBlocks.MAGMA_RIGGING_BLOCK.get()));
        return pairs;
    }

    private static int patternIdFromPatternItem(Item patternItem) {
        return WindFlagRecipeUtil.getPatternFromBannerPatternItem(new ItemStack(patternItem));
    }

    private static ItemStack makeWindFlagStack(Item flagItem, int pattern, int overlayColor, boolean emissive) {
        ItemStack stack = new ItemStack(flagItem);
        WindFlagRecipeUtil.writePattern(stack, pattern);
        WindFlagRecipeUtil.writeOverlayColor(stack, overlayColor);
        if (emissive) {
            stack.getOrCreateTagElement(BLOCK_STATE_TAG_KEY)
                    .putString(WindFlagBlock.EMISSIVE.getName(), Boolean.toString(true));
        }
        return stack;
    }

    private static Item randomWindFlag(Random random) {
        List<Item> flags = windFlagItems();
        return flags.get(random.nextInt(flags.size()));
    }

    private static Item randomWindFlagDifferent(Random random, Item notThisOne) {
        List<Item> flags = windFlagItems();
        Item selected = randomWindFlag(random);
        for (int i = 0; i < 24 && selected == notThisOne; i++) {
            selected = randomWindFlag(random);
        }
        if (selected != notThisOne) {
            return selected;
        }
        for (Item flag : flags) {
            if (flag != notThisOne) {
                return flag;
            }
        }
        return selected;
    }

    private static PatternSample samplePattern(Random random) {
        Item flagItem = randomWindFlag(random);
        Item patternItem = PATTERN_ITEMS.get(random.nextInt(PATTERN_ITEMS.size()));
        int overlayColor = random.nextInt(DyeColor.values().length);
        return new PatternSample(flagItem, patternItem, overlayColor);
    }

    private static DyeSample sampleDye(Random random) {
        DyeColor dyeColor = DyeColor.byId(random.nextInt(DyeColor.values().length));
        Item dyeItem = dyeItemForColor(dyeColor);
        if (random.nextBoolean()) {
            Item flagItem = randomWindFlag(random);
            Item patternItem = PATTERN_ITEMS.get(random.nextInt(PATTERN_ITEMS.size()));
            int patternId = patternIdFromPatternItem(patternItem);
            int outputOverlayColor = dyeColor.getId();
            int inputOverlayColor = (outputOverlayColor + 1 + random.nextInt(15)) % DyeColor.values().length;

            ItemStack input = makeWindFlagStack(flagItem, patternId, inputOverlayColor, false);
            ItemStack output = makeWindFlagStack(flagItem, patternId, outputOverlayColor, false);
            return new DyeSample(input, dyeItem, output);
        }

        Item targetFlag = WindFlagRecipeUtil.getFlagItemForDye(dyeColor);
        Item inputFlag = randomWindFlagDifferent(random, targetFlag);
        int overlayColor = random.nextInt(DyeColor.values().length);

        ItemStack input = makeWindFlagStack(inputFlag, WindFlagRecipeUtil.PATTERN_NONE, overlayColor, false);
        ItemStack output = makeWindFlagStack(targetFlag, WindFlagRecipeUtil.PATTERN_NONE, overlayColor, false);
        return new DyeSample(input, dyeItem, output);
    }

    private static Item dyeItemForColor(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
    }

    private record BlockPair(Block regular, Block magma) {
    }

    private record PatternSample(Item flagItem, Item patternItem, int overlayColorId) {
    }

    private record DyeSample(ItemStack inputFlag, Item dyeItem, ItemStack outputFlag) {
    }

    private static final class EmiWindFlagPatternCraftingRecipe extends EmiPatternCraftingRecipe {
        private EmiWindFlagPatternCraftingRecipe(ResourceLocation id) {
            super(
                    List.of(windFlagIngredient(), patternItemIngredient()),
                    EmiStack.of(makeWindFlagStack(
                            SailsBlocks.WIND_FLAG.get().asItem(),
                            patternIdFromPatternItem(Items.SKULL_BANNER_PATTERN),
                            DyeColor.WHITE.getId(),
                            false
                    )),
                    id
            );
        }

        @Override
        public SlotWidget getInputWidget(int slot, int x, int y) {
            if (slot == 0) {
                return new GeneratedSlotWidget(random -> {
                    PatternSample sample = samplePattern(random);
                    return EmiStack.of(makeWindFlagStack(
                            sample.flagItem(),
                            WindFlagRecipeUtil.PATTERN_NONE,
                            sample.overlayColorId(),
                            false
                    ));
                }, unique, x, y);
            }
            if (slot == 1) {
                return new GeneratedSlotWidget(random -> EmiStack.of(samplePattern(random).patternItem()), unique, x, y);
            }
            return new SlotWidget(EmiStack.EMPTY, x, y);
        }

        @Override
        public SlotWidget getOutputWidget(int x, int y) {
            return new GeneratedSlotWidget(random -> {
                PatternSample sample = samplePattern(random);
                return EmiStack.of(makeWindFlagStack(
                        sample.flagItem(),
                        patternIdFromPatternItem(sample.patternItem()),
                        sample.overlayColorId(),
                        false
                ));
            }, unique, x, y);
        }
    }

    private static final class EmiWindFlagDyeCraftingRecipe extends EmiPatternCraftingRecipe {
        private EmiWindFlagDyeCraftingRecipe(ResourceLocation id) {
            super(
                    List.of(windFlagIngredient(), dyeIngredient()),
                    EmiStack.of(makeWindFlagStack(
                            SailsBlocks.RED_WIND_FLAG.get().asItem(),
                            WindFlagRecipeUtil.PATTERN_NONE,
                            DyeColor.WHITE.getId(),
                            false
                    )),
                    id
            );
        }

        @Override
        public SlotWidget getInputWidget(int slot, int x, int y) {
            if (slot == 0) {
                return new GeneratedSlotWidget(random -> EmiStack.of(sampleDye(random).inputFlag()), unique, x, y);
            }
            if (slot == 1) {
                return new GeneratedSlotWidget(random -> EmiStack.of(sampleDye(random).dyeItem()), unique, x, y);
            }
            return new SlotWidget(EmiStack.EMPTY, x, y);
        }

        @Override
        public SlotWidget getOutputWidget(int x, int y) {
            return new GeneratedSlotWidget(random -> EmiStack.of(sampleDye(random).outputFlag()), unique, x, y);
        }
    }
}
