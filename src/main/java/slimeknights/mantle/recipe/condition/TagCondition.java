package slimeknights.mantle.recipe.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.mantle.Mantle;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

/** Common logic for {@link TagEmptyCondition} and {@link TagFilledCondition} */
@RequiredArgsConstructor
public abstract class TagCondition<T> implements ICondition {
  @Getter
  protected final TagKey<T> tag;
  @Nullable
  private Optional<Registry<T>> registry;

  /** Gets the registry */
  @Nullable
  protected Registry<T> registry(LootContext context) {
    // registry is not going to disappear within the lifetime of this object
    if (registry == null) {
      registry = context.getLevel().registryAccess().registry(tag.registry());
      if (registry.isEmpty()) {
        Mantle.logger.error("Failed to find registry for tag " + tag + " in " + getClass().getSimpleName() + ", this indicates a broken resource or datapack.");
      }
    }
    return registry.orElse(null);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(\"" + tag + "\")";
  }

  /**
   * Builds a {@link MapCodec} for a tag condition. Reads an optional "registry" (defaulting to the item registry) and a
   * required "tag", combining them into a {@link TagKey} passed to the given constructor.
   * @param constructor  Constructor taking the parsed tag key
   * @param <C>  Condition type
   * @return  Map codec for the condition
   */
  protected static <C extends TagCondition<?>> MapCodec<C> makeCodec(Function<TagKey<?>,C> constructor) {
    return RecordCodecBuilder.mapCodec(builder -> builder
      .group(
        // save some space in JSON by not setting registry if item (most common)
        ResourceLocation.CODEC.optionalFieldOf("registry", Registries.ITEM.location()).forGetter(c -> c.getTag().registry().location()),
        ResourceLocation.CODEC.fieldOf("tag").forGetter(c -> c.getTag().location()))
      .apply(builder, (registry, tag) -> constructor.apply(TagKey.create(ResourceKey.createRegistryKey(registry), tag))));
  }
}
