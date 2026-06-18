package slimeknights.mantle.recipe.crafting;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fromShaped")
public class ShapedRetexturedRecipeBuilder {
  private final ShapedRecipeBuilder parent;
  private Ingredient texture = null;
  private boolean matchAll = false;

  /**
   * Sets the texture source to the given ingredient
   * @param texture Ingredient to use for texture
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setSource(Ingredient texture) {
    this.texture = texture;
    return this;
  }

  /**
   * Sets the texture source to the given tag
   * @param tag Tag to use for texture
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setSource(TagKey<Item> tag) {
    return setSource(Ingredient.of(tag));
  }

  /**
   * Sets the match first property on the recipe.
   * If set, the recipe uses the first ingredient match for the texture. If unset, all items that match the ingredient must be the same or no texture is applied
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setMatchAll() {
    this.matchAll = true;
    return this;
  }

  /**
   * Builds the recipe with the default name using the given output
   * @param output Recipe output
   */
  public void build(RecipeOutput output) {
    this.validate();
    parent.save(new Output(output));
  }

  /**
   * Builds the recipe using the given output
   * @param output   Recipe output
   * @param location Recipe location
   */
  public void build(RecipeOutput output, ResourceLocation location) {
    this.validate();
    parent.save(new Output(output), location);
  }

  /**
   * Ensures this recipe can be built
   * @throws IllegalStateException If the recipe cannot be built
   */
  private void validate() {
    if (texture == null) {
      throw new IllegalStateException("No texture defined for texture recipe");
    }
  }

  /** Recipe output wrapper that re-emits the built shaped recipe wrapped in a {@link ShapedRetexturedRecipe} */
  private class Output implements RecipeOutput {
    private final RecipeOutput wrapped;

    private Output(RecipeOutput wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
      ShapedRecipe shaped = (ShapedRecipe) recipe;
      // ShapedRecipe.getResultItem ignores the provider and returns the stored result, so null is safe here
      ShapedRetexturedRecipe retextured = new ShapedRetexturedRecipe(shaped.getGroup(), shaped.category(), shaped.pattern, shaped.getResultItem(null), shaped.showNotification(), texture, matchAll);
      wrapped.accept(id, retextured, advancement, conditions);
    }

    @Override
    public Advancement.Builder advancement() {
      return wrapped.advancement();
    }
  }
}
