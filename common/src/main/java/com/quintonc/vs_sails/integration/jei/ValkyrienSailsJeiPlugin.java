package com.quintonc.vs_sails.integration.jei;

import com.quintonc.vs_sails.ValkyrienSails;
import com.quintonc.vs_sails.blocks.WindFlagBlock;
import com.quintonc.vs_sails.recipes.WindFlagRecipeUtil;
import com.quintonc.vs_sails.registration.SailsBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class ValkyrienSailsJeiPlugin implements IModPlugin {
    private static final String BLOCK_STATE_TAG_KEY = "BlockStateTag";
    private static final ResourceLocation PLUGIN_UID = id("jei_plugin");
    private static final RecipeType<WindFlagCraftingJeiRecipe> WIND_FLAG_CRAFTING =
            RecipeType.create(ValkyrienSails.MOD_ID, "wind_flag_crafting", WindFlagCraftingJeiRecipe.class);
    private static final RecipeType<WorldInteractionJeiRecipe> WORLD_INTERACTION =
            RecipeType.create(ValkyrienSails.MOD_ID, "world_interaction", WorldInteractionJeiRecipe.class);
    private static final List<Item> PATTERN_ITEMS = List.of(
            Items.SKULL_BANNER_PATTERN,
            Items.CREEPER_BANNER_PATTERN,
            Items.FLOWER_BANNER_PATTERN,
            Items.MOJANG_BANNER_PATTERN,
            Items.GLOBE_BANNER_PATTERN,
            Items.PIGLIN_BANNER_PATTERN
    );

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new WindFlagCraftingCategory(guiHelper),
                new WorldInteractionCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(WIND_FLAG_CRAFTING, buildWindFlagCraftingRecipes());
        registration.addRecipes(WORLD_INTERACTION, buildWorldInteractionRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Blocks.CRAFTING_TABLE, WIND_FLAG_CRAFTING);
        registration.addRecipeCatalyst(SailsBlocks.WIND_FLAG.get(), WORLD_INTERACTION);
        registration.addRecipeCatalyst(Items.SPLASH_POTION, WORLD_INTERACTION);
        registration.addRecipeCatalyst(Items.POTION, WORLD_INTERACTION);
    }

    private static List<WindFlagCraftingJeiRecipe> buildWindFlagCraftingRecipes() {
        List<WindFlagCraftingJeiRecipe> recipes = new ArrayList<>();
        List<Item> flagItems = windFlagItems();

        for (Item patternItem : PATTERN_ITEMS) {
            int pattern = patternIdFromPatternItem(patternItem);
            List<ItemStack> inputs = new ArrayList<>();
            List<ItemStack> patternInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            for (Item flag : flagItems) {
                inputs.add(makeWindFlagStack(flag, WindFlagRecipeUtil.PATTERN_NONE, DyeColor.WHITE.getId(), false));
                patternInputs.add(new ItemStack(patternItem));
                outputs.add(makeWindFlagStack(flag, pattern, DyeColor.WHITE.getId(), false));
            }
            recipes.add(new WindFlagCraftingJeiRecipe(
                    id("jei/crafting/wind_flag_pattern/" + BuiltInRegistries.ITEM.getKey(patternItem).getPath()),
                    inputs,
                    patternInputs,
                    outputs
            ));
        }

        for (DyeColor dyeColor : DyeColor.values()) {
            Item targetFlag = WindFlagRecipeUtil.getFlagItemForDye(dyeColor);
            Item dyeItem = dyeItemForColor(dyeColor);
            List<ItemStack> inputs = new ArrayList<>();
            List<ItemStack> dyeInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            for (Item flag : flagItems) {
                if (flag == targetFlag) {
                    continue;
                }
                inputs.add(makeWindFlagStack(flag, WindFlagRecipeUtil.PATTERN_NONE, DyeColor.WHITE.getId(), false));
                dyeInputs.add(new ItemStack(dyeItem));
                outputs.add(makeWindFlagStack(targetFlag, WindFlagRecipeUtil.PATTERN_NONE, DyeColor.WHITE.getId(), false));
            }
            recipes.add(new WindFlagCraftingJeiRecipe(
                    id("jei/crafting/wind_flag_dye_base/" + BuiltInRegistries.ITEM.getKey(dyeItem).getPath()),
                    inputs,
                    dyeInputs,
                    outputs
            ));
        }

        int samplePattern = patternIdFromPatternItem(Items.SKULL_BANNER_PATTERN);
        for (DyeColor dyeColor : DyeColor.values()) {
            Item dyeItem = dyeItemForColor(dyeColor);
            List<ItemStack> inputs = new ArrayList<>();
            List<ItemStack> dyeInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            for (Item flag : flagItems) {
                int inputOverlay = (dyeColor.getId() + 1) % DyeColor.values().length;
                inputs.add(makeWindFlagStack(flag, samplePattern, inputOverlay, false));
                dyeInputs.add(new ItemStack(dyeItem));
                outputs.add(makeWindFlagStack(flag, samplePattern, dyeColor.getId(), false));
            }
            recipes.add(new WindFlagCraftingJeiRecipe(
                    id("jei/crafting/wind_flag_dye_overlay/" + BuiltInRegistries.ITEM.getKey(dyeItem).getPath()),
                    inputs,
                    dyeInputs,
                    outputs
            ));
        }

        return recipes;
    }

    private static List<WorldInteractionJeiRecipe> buildWorldInteractionRecipes() {
        List<WorldInteractionJeiRecipe> recipes = new ArrayList<>();
        List<Item> flagItems = windFlagItems();

        for (Item patternItem : PATTERN_ITEMS) {
            int pattern = patternIdFromPatternItem(patternItem);
            List<ItemStack> leftInputs = new ArrayList<>();
            List<ItemStack> rightInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            for (Item flag : flagItems) {
                leftInputs.add(makeWindFlagStack(flag, WindFlagRecipeUtil.PATTERN_NONE, DyeColor.WHITE.getId(), false));
                rightInputs.add(new ItemStack(patternItem));
                outputs.add(makeWindFlagStack(flag, pattern, DyeColor.WHITE.getId(), false));
            }
            recipes.add(new WorldInteractionJeiRecipe(
                    id("jei/world/wind_flag_pattern/" + BuiltInRegistries.ITEM.getKey(patternItem).getPath()),
                    leftInputs,
                    rightInputs,
                    outputs
            ));
        }

        int samplePattern = patternIdFromPatternItem(Items.SKULL_BANNER_PATTERN);
        for (DyeColor dyeColor : DyeColor.values()) {
            Item dyeItem = dyeItemForColor(dyeColor);
            List<ItemStack> leftInputs = new ArrayList<>();
            List<ItemStack> rightInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            for (Item flag : flagItems) {
                int inputOverlay = (dyeColor.getId() + 1) % DyeColor.values().length;
                leftInputs.add(makeWindFlagStack(flag, samplePattern, inputOverlay, false));
                rightInputs.add(new ItemStack(dyeItem));
                outputs.add(makeWindFlagStack(flag, samplePattern, dyeColor.getId(), false));
            }
            recipes.add(new WorldInteractionJeiRecipe(
                    id("jei/world/wind_flag_overlay/" + BuiltInRegistries.ITEM.getKey(dyeItem).getPath()),
                    leftInputs,
                    rightInputs,
                    outputs
            ));
        }

        {
            List<ItemStack> leftInputs = new ArrayList<>();
            List<ItemStack> rightInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            for (Item flag : flagItems) {
                leftInputs.add(makeWindFlagStack(flag, samplePattern, DyeColor.WHITE.getId(), false));
                rightInputs.add(new ItemStack(Items.GLOW_INK_SAC));
                outputs.add(makeWindFlagStack(flag, samplePattern, DyeColor.WHITE.getId(), true));
            }
            recipes.add(new WorldInteractionJeiRecipe(
                    id("jei/world/wind_flag_glow_ink"),
                    leftInputs,
                    rightInputs,
                    outputs
            ));
        }

        {
            ItemStack jebTag = new ItemStack(Items.NAME_TAG);
            jebTag.setHoverName(Component.literal("jeb_"));
            List<ItemStack> leftInputs = new ArrayList<>();
            List<ItemStack> rightInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            for (Item flag : flagItems) {
                leftInputs.add(makeWindFlagStack(flag, samplePattern, DyeColor.WHITE.getId(), false));
                rightInputs.add(jebTag.copy());
                outputs.add(makeWindFlagStack(flag, samplePattern, WindFlagBlock.OVERLAY_COLOR_RAINBOW, false));
            }
            recipes.add(new WorldInteractionJeiRecipe(
                    id("jei/world/wind_flag_rainbow"),
                    leftInputs,
                    rightInputs,
                    outputs
            ));
        }

        ItemStack fireResistancePotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.FIRE_RESISTANCE);
        ItemStack splashWaterPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.WATER);
        ItemStack waterPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
        for (BlockPair pair : magmaConvertiblePairs()) {
            ResourceLocation regularId = BuiltInRegistries.BLOCK.getKey(pair.regular());
            ResourceLocation magmaId = BuiltInRegistries.BLOCK.getKey(pair.magma());

            recipes.add(new WorldInteractionJeiRecipe(
                    id("jei/world/magma_coating/" + regularId.getPath()),
                    List.of(new ItemStack(pair.regular())),
                    List.of(fireResistancePotion.copy()),
                    List.of(new ItemStack(pair.magma()))
            ));

            recipes.add(new WorldInteractionJeiRecipe(
                    id("jei/world/magma_washing/" + magmaId.getPath()),
                    List.of(new ItemStack(pair.magma())),
                    List.of(splashWaterPotion.copy(), waterPotion.copy()),
                    List.of(new ItemStack(pair.regular()))
            ));
        }

        return recipes;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ValkyrienSails.MOD_ID, path);
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

    private record WindFlagCraftingJeiRecipe(
            ResourceLocation id,
            List<ItemStack> firstInputs,
            List<ItemStack> secondInputs,
            List<ItemStack> outputs
    ) {
    }

    private record WorldInteractionJeiRecipe(
            ResourceLocation id,
            List<ItemStack> leftInputs,
            List<ItemStack> rightInputs,
            List<ItemStack> outputs
    ) {
    }

    private static final class WindFlagCraftingCategory extends AbstractRecipeCategory<WindFlagCraftingJeiRecipe> {
        private final IDrawable arrow;

        private WindFlagCraftingCategory(IGuiHelper guiHelper) {
            super(
                    WIND_FLAG_CRAFTING,
                    Component.translatable("jei.vs_sails.category.wind_flag_crafting"),
                    guiHelper.createDrawableItemLike(SailsBlocks.WIND_FLAG.get()),
                    118,
                    54
            );
            this.arrow = guiHelper.getRecipeArrow();
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, WindFlagCraftingJeiRecipe recipe, IFocusGroup focuses) {
            IRecipeSlotBuilder first = builder.addInputSlot(1, 1)
                    .setStandardSlotBackground()
                    .addItemStacks(recipe.firstInputs());
            IRecipeSlotBuilder second = builder.addInputSlot(19, 1)
                    .setStandardSlotBackground()
                    .addItemStacks(recipe.secondInputs());
            IRecipeSlotBuilder output = builder.addOutputSlot(92, 14)
                    .setOutputSlotBackground()
                    .addItemStacks(recipe.outputs());
            if (recipe.firstInputs().size() == recipe.secondInputs().size()
                    && recipe.secondInputs().size() == recipe.outputs().size()
                    && recipe.outputs().size() > 1) {
                builder.createFocusLink(first, second, output);
            }
            builder.setShapeless(97, 0);
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, WindFlagCraftingJeiRecipe recipe, IFocusGroup focuses) {
            builder.addDrawable(arrow, 60, 18);
        }

        @Override
        public ResourceLocation getRegistryName(WindFlagCraftingJeiRecipe recipe) {
            return recipe.id();
        }
    }

    private static final class WorldInteractionCategory extends AbstractRecipeCategory<WorldInteractionJeiRecipe> {
        private final IDrawable plus;
        private final IDrawable arrow;

        private WorldInteractionCategory(IGuiHelper guiHelper) {
            super(
                    WORLD_INTERACTION,
                    Component.translatable("jei.vs_sails.category.world_interactions"),
                    guiHelper.createDrawableItemLike(Items.SPLASH_POTION),
                    125,
                    38
            );
            this.plus = guiHelper.getRecipePlusSign();
            this.arrow = guiHelper.getRecipeArrow();
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, WorldInteractionJeiRecipe recipe, IFocusGroup focuses) {
            IRecipeSlotBuilder left = builder.addInputSlot(1, 1)
                    .setStandardSlotBackground()
                    .addItemStacks(recipe.leftInputs());
            IRecipeSlotBuilder right = builder.addInputSlot(50, 1)
                    .setStandardSlotBackground()
                    .addItemStacks(recipe.rightInputs());
            IRecipeSlotBuilder output = builder.addOutputSlot(108, 1)
                    .setOutputSlotBackground()
                    .addItemStacks(recipe.outputs());
            if (recipe.leftInputs().size() == recipe.rightInputs().size()
                    && recipe.rightInputs().size() == recipe.outputs().size()
                    && recipe.outputs().size() > 1) {
                builder.createFocusLink(left, right, output);
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, WorldInteractionJeiRecipe recipe, IFocusGroup focuses) {
            builder.addDrawable(plus, 27, 3);
            builder.addDrawable(arrow, 76, 1);
        }

        @Override
        public ResourceLocation getRegistryName(WorldInteractionJeiRecipe recipe) {
            return recipe.id();
        }
    }
}
