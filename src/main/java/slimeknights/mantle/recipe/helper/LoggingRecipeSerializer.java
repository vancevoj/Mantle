package slimeknights.mantle.recipe.helper;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.Mantle;

/**
 * Recipe serializer that logs network exceptions before throwing them as otherwise the exceptions may be invisible.
 * Implementors must provide {@link #codec()} along with {@link #fromNetworkSafe(RegistryFriendlyByteBuf)} and
 * {@link #toNetworkSafe(RegistryFriendlyByteBuf, Recipe)}; the stream codec is built automatically and wraps both
 * directions in logging.
 * @param <T>  Recipe class
 */
public interface LoggingRecipeSerializer<T extends Recipe<?>> extends RecipeSerializer<T> {
  /**
   * Read the recipe from the packet
   * @param buffer  Buffer instance
   * @return  Parsed recipe
   * @throws RuntimeException  If any errors happen, the exception will be logged automatically
   */
  T fromNetworkSafe(RegistryFriendlyByteBuf buffer);

  /**
   * Write the recipe to the buffer
   * @param buffer  Buffer instance
   * @param recipe  Recipe instance
   * @throws RuntimeException  If any errors happen, the exception will be logged automatically
   */
  void toNetworkSafe(RegistryFriendlyByteBuf buffer, T recipe);

  /** Reads the recipe from the network, logging any errors before rethrowing them */
  default T fromNetwork(RegistryFriendlyByteBuf buffer) {
    try {
      return fromNetworkSafe(buffer);
    } catch (RuntimeException e) {
      Mantle.logger.error("{}: Error reading recipe from packet", this.getClass().getSimpleName(), e);
      throw e;
    }
  }

  /** Writes the recipe to the network, logging any errors before rethrowing them */
  default void toNetwork(RegistryFriendlyByteBuf buffer, T recipe) {
    try {
      toNetworkSafe(buffer, recipe);
    } catch (RuntimeException e) {
      Mantle.logger.error("{}: Error writing recipe of class {} and type {} to packet", this.getClass().getSimpleName(), recipe.getClass().getSimpleName(), recipe.getType(), e);
      throw e;
    }
  }

  @Override
  default StreamCodec<RegistryFriendlyByteBuf,T> streamCodec() {
    return StreamCodec.of(this::toNetwork, this::fromNetwork);
  }
}
