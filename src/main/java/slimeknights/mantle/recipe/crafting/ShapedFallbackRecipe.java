package slimeknights.mantle.recipe.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.MantleRecipes;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ShapedFallbackRecipe extends ShapedRecipe {

  /** Recipes to skip if they match */
  private final List<ResourceLocation> alternatives;
  private List<CraftingRecipe> alternativeCache;

  /**
   * Main constructor, creates a recipe from all parameters
   * @param group             Recipe group
   * @param category          Recipe book category
   * @param pattern           Recipe pattern (width, height, ingredients)
   * @param result            Recipe output
   * @param showNotification  If true, shows the toast notification on first craft
   * @param alternatives      List of recipe names to fail this match if they match
   */
  public ShapedFallbackRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification, List<ResourceLocation> alternatives) {
    super(group, category, pattern, result, showNotification);
    this.alternatives = alternatives;
  }

  @Override
  public boolean matches(CraftingInput inv, Level world) {
    // if this recipe does not match, fail it
    if (!super.matches(inv, world)) {
      return false;
    }

    // fetch all alternatives, fail if any match
    // cache to save effort down the line
    if (alternativeCache == null) {
      var manager = world.getRecipeManager();
      alternativeCache = alternatives.stream()
                                     .map(manager::byKey)
                                     .filter(java.util.Optional::isPresent)
                                     .map(java.util.Optional::get)
                                     .map(holder -> holder.value())
                                     .filter(recipe -> {
                                       // only allow exact shaped or shapeless match, prevent infinite recursion due to complex recipes
                                       Class<?> clazz = recipe.getClass();
                                       return clazz == ShapedRecipe.class || clazz == ShapelessRecipe.class;
                                     })
                                     .map(recipe -> (CraftingRecipe) recipe)
                                     .toList();
    }
    // fail if any alternative matches
    return this.alternativeCache.stream().noneMatch(recipe -> recipe.matches(inv, world));
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return MantleRecipes.CRAFTING_SHAPED_FALLBACK.get();
  }

  public static class Serializer implements RecipeSerializer<ShapedFallbackRecipe> {
    private static final MapCodec<ShapedFallbackRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
      CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
      ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
      ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.getResultItem(null)),
      Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification),
      ResourceLocation.CODEC.listOf().optionalFieldOf("alternatives", List.of()).forGetter(r -> r.alternatives)
    ).apply(inst, ShapedFallbackRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, ShapedFallbackRecipe> STREAM_CODEC = StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

    @Override
    public MapCodec<ShapedFallbackRecipe> codec() {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ShapedFallbackRecipe> streamCodec() {
      return STREAM_CODEC;
    }

    private static ShapedFallbackRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
      String group = buffer.readUtf();
      CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
      ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
      ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
      boolean showNotification = buffer.readBoolean();
      int size = buffer.readVarInt();
      List<ResourceLocation> alternatives = new java.util.ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        alternatives.add(buffer.readResourceLocation());
      }
      return new ShapedFallbackRecipe(group, category, pattern, result, showNotification, List.copyOf(alternatives));
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, ShapedFallbackRecipe recipe) {
      buffer.writeUtf(recipe.getGroup());
      buffer.writeEnum(recipe.category());
      ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
      ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(null));
      buffer.writeBoolean(recipe.showNotification());
      buffer.writeVarInt(recipe.alternatives.size());
      for (ResourceLocation alternative : recipe.alternatives) {
        buffer.writeResourceLocation(alternative);
      }
    }
  }
}
