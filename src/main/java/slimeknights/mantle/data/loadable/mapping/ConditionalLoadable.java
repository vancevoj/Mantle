package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Loadable allowing a load time condition check to change which object is used.
 * Automatically used in {@link GenericLoaderRegistry} for all types, though you will have to manually provide datagen using {@link ConditionalObject}.
 * @param registry  Loader registry to fetch nested objects.
 * @param defaultIfFalse  Value to use if the condition fails and no false object is provided, typically should be an empty object. If null, a false object must be specified.
 */
public record ConditionalLoadable<T extends IHaveLoader>(GenericLoaderRegistry<T> registry, @Nullable T defaultIfFalse) implements RecordLoadable<T> {
  @Override
  public T deserialize(JsonObject json, TypedMap context) {
    // allow passing in the condition context via the loadable context
    // if missing, assume tags are invalid
    IContext conditionContext = context.getOrDefault(ContextKey.CONDITION_CONTEXT, IContext.TAGS_INVALID);
    // if the condition matches, use the true value
    if (conditionsMatch(json, conditionContext)) {
      return registry.getIfPresent(json, "if_true");
    }
    // loader can define a default instance for false if they have one. Otherwise false is required.
    if (defaultIfFalse != null) {
      return registry.getOrDefault(json, "if_false", defaultIfFalse, context);
    }
    return registry.getIfPresent(json, "if_false", context);
  }

  @SuppressWarnings("unchecked") // loader is invalid if not
  @Override
  public void serialize(T object, JsonObject json) {
    ConditionalObject<T> conditional = (ConditionalObject<T>) object;
    json.add("conditions", ICondition.LIST_CODEC.encodeStart(JsonOps.INSTANCE, List.of(conditional.conditions()))
                                                .getOrThrow(msg -> new IllegalStateException("Failed to serialize conditions: " + msg)));
    json.add("if_true", registry.serialize(conditional.ifTrue()));
    T ifFalse = conditional.ifFalse();
    if (ifFalse != defaultIfFalse) {
      json.add("if_false", registry.serialize(ifFalse));
    }
  }

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    throw new UnsupportedOperationException("Conditional loadable should always resolve to a specific instance. This should never happen.");
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T value) {
    throw new UnsupportedOperationException("Conditional loadable should always resolve to a specific instance. This should never happen.");
  }

  /** Checks if all conditions under the {@code conditions} key match the given context. Returns true if the key is absent. */
  private static boolean conditionsMatch(JsonObject json, IContext conditionContext) {
    JsonElement element = json.get("conditions");
    if (element == null) {
      return true;
    }
    List<ICondition> conditions = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, element)
                                                       .getOrThrow(msg -> new com.google.gson.JsonSyntaxException("Failed to parse conditions: " + msg));
    for (ICondition condition : conditions) {
      if (!condition.test(conditionContext)) {
        return false;
      }
    }
    return true;
  }

  /** Interface for the serializable version of {@link ConditionalLoadable} */
  public interface ConditionalObject<T> extends IHaveLoader {
    /** Conditions on the object */
    ICondition[] conditions();

    /** Object to use when conditions are true */
    T ifTrue();

    /** Object to use when conditions are false */
    T ifFalse();
  }
}
