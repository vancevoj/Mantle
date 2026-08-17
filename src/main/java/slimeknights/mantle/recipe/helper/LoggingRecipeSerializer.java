package slimeknights.mantle.recipe.helper;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
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
      // wrap in a decoder exception so the client disconnect screen names the serializer instead of a bare error
      String error = this.getClass().getSimpleName() + ": Error reading recipe from packet";
      Mantle.logger.error("{}", error, e);
      throw new DecoderException(error + " - " + e.getMessage(), e);
    }
  }

  /** Writes the recipe to the network, logging any errors before rethrowing them */
  default void toNetwork(RegistryFriendlyByteBuf buffer, T recipe) {
    try {
      toNetworkSafe(buffer, recipe);
    } catch (RuntimeException e) {
      // wrap in an encoder exception so the disconnect screen names the serializer instead of a bare error
      String error = this.getClass().getSimpleName() + ": Error writing recipe of class " + recipe.getClass().getSimpleName() + " and type " + recipe.getType() + " to packet";
      Mantle.logger.error("{}", error, e);
      throw new EncoderException(error + " - " + e.getMessage(), e);
    }
  }

  @Override
  default StreamCodec<RegistryFriendlyByteBuf,T> streamCodec() {
    return StreamCodec.of(this::toNetwork, this::fromNetwork);
  }
}
