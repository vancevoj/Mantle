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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import javax.annotation.Nullable;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Recipe serializer instance using loadables. Bridges a {@link RecordLoadable} to the vanilla
 * {@link MapCodec} + {@link net.minecraft.network.codec.StreamCodec} pair required by {@link RecipeSerializer}.
 * <p>
 * In 1.21 the recipe's {@link ResourceLocation} is no longer available during decode (it is the registry key,
 * assigned to the {@link net.minecraft.world.item.crafting.RecipeHolder} only after decoding). Recipes that need a
 * non-null {@link ContextKey#ID} (for JEI display and logging) therefore receive a stable synthetic id derived from
 * the serializer's registry name plus the recipe contents. Nothing in game logic keys off this id; matching, caches,
 * and authoritative id lookups all use the real {@link net.minecraft.world.item.crafting.RecipeHolder} id.
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

  /** Fallback namespace used when the serializer is not yet registered (should not happen in practice) */
  private static final String FALLBACK_NAMESPACE = "mantle";


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

  /**
   * Builds the loadable context for a recipe parse. Adds the recipe serializer (and, for type aware serializers, the
   * recipe type and typed serializer) plus the id and debug info.
   * @param id  Recipe id. In 1.21 this is a synthetic stable id, as the real id is not available during decode.
   */
  protected TypedMapBuilder buildContext(ResourceLocation id) {
    return TypedMapBuilder.builder().put(ContextKey.ID, id).put(ContextKey.DEBUG, "Recipe " + id).put(SERIALIZER, this);
  }

  /** Namespace for synthetic recipe ids, the registry name of this serializer if available */
  private String syntheticNamespace() {
    ResourceLocation key = BuiltInRegistries.RECIPE_SERIALIZER.getKey(this);
    return key != null ? key.getNamespace() : FALLBACK_NAMESPACE;
  }

  /**
   * Builds a stable synthetic id from a hash. The real recipe id is unavailable during 1.21 decode (it is the registry
   * key assigned to the {@link net.minecraft.world.item.crafting.RecipeHolder} after decoding), so we derive a
   * deterministic placeholder. Uniqueness keeps JEI recipe UIDs from colliding; nothing in game logic depends on it.
   */
  private ResourceLocation syntheticId(int hash) {
    // mask off the sign so the path is always a valid lowercase hex string
    return ResourceLocation.fromNamespaceAndPath(syntheticNamespace(), "synthetic/" + Integer.toHexString(hash & 0x7fffffff));
  }

  @Override
  public MapCodec<T> codec() {
    if (codec == null) {
      // pass a context builder so decode can supply the ID/SERIALIZER/TYPE/TYPED_SERIALIZER context fields
      codec = new LoadableMapCodec<>(loadable, json -> buildContext(syntheticId(json.toString().hashCode())).build());
    }
    return codec;
  }

  @Override
  public T fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
    // network decode also pulls ID/SERIALIZER from context (see ContextField.decode), so build context here too.
    // the buffer is opaque (no recipe id is sent), so derive a synthetic id from the read position, which is stable
    // for a given recipe given recipes sync in a deterministic order. nothing in game logic keys off this id.
    return loadable.decode(buffer, buildContext(syntheticId(buffer.readerIndex())).build());
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
    /**
     * Builds the loadable context from the parsed JSON. Recipe serializers pass a builder supplying the
     * ID/SERIALIZER/TYPE/TYPED_SERIALIZER context; other users (e.g. custom ingredients) pass null for no context.
     */
    @Nullable
    private final Function<JsonObject,TypedMap> contextBuilder;

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
        // build the loadable context: the real recipe id is not available during 1.21 decode, so the recipe serializer
        // derives a stable synthetic id from the json contents. this gives required context fields (ID, SERIALIZER,
        // TYPE, TYPED_SERIALIZER) a non-null value, keeping recipes that require them loading 1:1 with the original.
        TypedMap context = contextBuilder != null ? contextBuilder.apply(json) : TypedMap.EMPTY;
        return DataResult.success(loadable.deserialize(json, context));
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
    protected TypedMapBuilder buildContext(ResourceLocation id) {
      return super.buildContext(id).put(TYPE, getType()).put(TYPED_SERIALIZER, this);
    }

    @Override
    public RecipeType<?> getType() {
      return type.get();
    }
  }
}
