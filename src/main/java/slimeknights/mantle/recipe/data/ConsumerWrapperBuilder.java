package slimeknights.mantle.recipe.data;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link RecipeOutput} wrapper, which adds conditions to all recipes passed through it.
 * <p>
 * In 1.21 recipes carry their own serializer, so the legacy serializer-type override is no longer
 * meaningful and has been dropped. For condition-only wrapping you may also use
 * {@link RecipeOutput#withConditions(ICondition...)} directly.
 */
@SuppressWarnings("unused")  // API
public class ConsumerWrapperBuilder {
  private final List<ICondition> conditions = new ArrayList<>();

  private ConsumerWrapperBuilder() {}

  /**
   * Creates a wrapper builder
   * @return Wrapper builder
   */
  public static ConsumerWrapperBuilder wrap() {
    return new ConsumerWrapperBuilder();
  }

  /**
   * Adds a conditional to the consumer
   * @param condition Condition to add
   * @return Added condition
   */
  @CanIgnoreReturnValue
  public ConsumerWrapperBuilder addCondition(ICondition condition) {
    conditions.add(condition);
    return this;
  }

  /**
   * Builds the recipe output for the wrapper builder
   * @param base  Base recipe output
   * @return Built wrapper recipe output that injects the accumulated conditions
   */
  public RecipeOutput build(RecipeOutput base) {
    return new Wrapped(base, List.copyOf(conditions));
  }

  /** Recipe output that prepends a set of conditions to every recipe before delegating */
  private record Wrapped(RecipeOutput base, List<ICondition> conditions) implements RecipeOutput {
    @Override
    public Advancement.Builder advancement() {
      return base.advancement();
    }

    @Override
    public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
      ICondition[] combined;
      if (this.conditions.isEmpty()) {
        combined = conditions;
      } else {
        combined = new ICondition[this.conditions.size() + conditions.length];
        this.conditions.toArray(combined);
        System.arraycopy(conditions, 0, combined, this.conditions.size(), conditions.length);
      }
      base.accept(id, recipe, advancement, combined);
    }
  }
}
