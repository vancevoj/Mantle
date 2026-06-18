package slimeknights.mantle.recipe.crafting;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Builder for a shaped recipe with fallbacks */
@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fallback")
public class ShapedFallbackRecipeBuilder {
  private final ShapedRecipeBuilder base;
  private final List<ResourceLocation> alternatives = new ArrayList<>();

  /**
   * Adds a single alternative to this recipe. Any matching alternative causes this recipe to fail
   * @param location  Alternative
   * @return  Builder instance
   */
  public ShapedFallbackRecipeBuilder addAlternative(ResourceLocation location) {
    this.alternatives.add(location);
    return this;
  }

  /**
   * Adds a list of alternatives to this recipe. Any matching alternative causes this recipe to fail
   * @param locations  Alternative list
   * @return  Builder instance
   */
  public ShapedFallbackRecipeBuilder addAlternatives(Collection<ResourceLocation> locations) {
    this.alternatives.addAll(locations);
    return this;
  }

  /**
   * Builds the recipe using the output as the name
   * @param output  Recipe output
   */
  public void build(RecipeOutput output) {
    base.save(new Output(output, alternatives));
  }

  /**
   * Builds the recipe using the given ID
   * @param output  Recipe output
   * @param id      Recipe ID
   */
  public void build(RecipeOutput output, ResourceLocation id) {
    base.save(new Output(output, alternatives), id);
  }

  /** Recipe output wrapper that re-emits the built shaped recipe wrapped in a {@link ShapedFallbackRecipe} */
  private record Output(RecipeOutput parent, List<ResourceLocation> alternatives) implements RecipeOutput {
    @Override
    public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
      ShapedRecipe shaped = (ShapedRecipe) recipe;
      // ShapedRecipe.getResultItem ignores the provider and returns the stored result, so null is safe here
      ShapedFallbackRecipe fallback = new ShapedFallbackRecipe(shaped.getGroup(), shaped.category(), shaped.pattern, shaped.getResultItem(null), shaped.showNotification(), alternatives);
      parent.accept(id, fallback, advancement, conditions);
    }

    @Override
    public Advancement.Builder advancement() {
      return parent.advancement();
    }
  }
}
