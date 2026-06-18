package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Recipe serializer instance using loadables. Bridges a {@link RecordLoadable} to the vanilla
 * {@link MapCodec} + {@link net.minecraft.network.codec.StreamCodec} pair required by {@link RecipeSerializer}.
 * @param <T>  Recipe type
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class LoadableRecipeSerializer<T extends Recipe<?>> implements LoggingRecipeSerializer<T> {
  /** Context key to use if you want the recipe serializer passed into your recipe */
  public static final ContextKey<RecipeSerializer<?>> SERIALIZER = new ContextKey<>("serializer");
  /** Context key to use if you want a type aware serializer in the recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<TypeAwareRecipeSerializer<?>> TYPED_SERIALIZER = new ContextKey<>("typed_serializer");
  /** Context key to use if you want the recipe type passed into your recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<RecipeType<?>> TYPE = new ContextKey<>("type");
  /** Field for a group key in a recipe (common requirement) */
  public static final LoadableField<String,Recipe<?>> RECIPE_GROUP = StringLoadable.DEFAULT.defaultField("group", "", Recipe::getGroup);


  protected final RecordLoadable<T> loadable;
  private MapCodec<T> codec;

  /** Creates a standard serializer from a loadable */
  public static <T extends Recipe<?>> RecipeSerializer<T> of(RecordLoadable<T> loadable) {
    return new LoadableRecipeSerializer<>(loadable);
  }

  /** Creates a type aware serializer from a loadable */
  public static <T extends R, R extends Recipe<?>> TypeAwareRecipeSerializer<T> of(RecordLoadable<T> loadable, Supplier<? extends RecipeType<R>> type) {
    return new TypeAware<>(loadable, type);
  }

  @Override
  public MapCodec<T> codec() {
    if (codec == null) {
      codec = new LoadableMapCodec<>(loadable);
    }
    return codec;
  }

  @Override
  public T fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
    return loadable.decode(buffer);
  }

  @Override
  public void toNetworkSafe(RegistryFriendlyByteBuf buffer, T recipe) {
    loadable.encode(buffer, recipe);
  }

  /**
   * Map codec wrapping a {@link RecordLoadable}, bridging between arbitrary dynamic ops and the loadable's JSON form.
   * As Mantle loadables operate on {@link JsonObject}, we convert any map to JSON, deserialize, and on encode
   * serialize to JSON then copy the fields back into the target ops.
   */
  @RequiredArgsConstructor
  protected static class LoadableMapCodec<T> extends MapCodec<T> {
    private final RecordLoadable<T> loadable;

    @Override
    public <O> Stream<O> keys(DynamicOps<O> ops) {
      // we cannot know the keys ahead of time, so return none; vanilla only uses this for compression which we opt out of
      return Stream.empty();
    }

    @Override
    public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
      try {
        JsonObject json = new JsonObject();
        input.entries().forEach(pair -> {
          String key = ops.getStringValue(pair.getFirst()).result().orElse(null);
          if (key != null) {
            json.add(key, ops.convertTo(JsonOps.INSTANCE, pair.getSecond()));
          }
        });
        return DataResult.success(loadable.deserialize(json));
      } catch (RuntimeException e) {
        return DataResult.error(e::getMessage);
      }
    }

    @Override
    public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
      try {
        JsonObject json = new JsonObject();
        loadable.serialize(input, json);
        for (Entry<String,JsonElement> entry : json.entrySet()) {
          prefix.add(entry.getKey(), JsonOps.INSTANCE.convertTo(ops, entry.getValue()));
        }
      } catch (RuntimeException e) {
        return prefix.withErrorsFrom(DataResult.error(e::getMessage));
      }
      return prefix;
    }
  }

  public static class TypeAware<T extends Recipe<?>> extends LoadableRecipeSerializer<T> implements TypeAwareRecipeSerializer<T> {
    private final Supplier<? extends RecipeType<?>> type;
    protected TypeAware(RecordLoadable<T> loadable, Supplier<? extends RecipeType<?>> type) {
      super(loadable);
      this.type = type;
    }

    @Override
    public RecipeType<?> getType() {
      return type.get();
    }
  }
}
