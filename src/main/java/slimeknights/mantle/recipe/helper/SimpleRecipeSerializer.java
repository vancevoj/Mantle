package slimeknights.mantle.recipe.helper;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Supplier;

/** Simple implementation of a recipe serializer with no properties. */
public record SimpleRecipeSerializer<T extends Recipe<?>>(Supplier<T> constructor) implements RecipeSerializer<T> {
  @Override
  public MapCodec<T> codec() {
    return MapCodec.unit(constructor);
  }

  @Override
  public StreamCodec<RegistryFriendlyByteBuf,T> streamCodec() {
    // no data to sync; build a fresh instance on decode. Must NOT use StreamCodec.unit(constructor.get()),
    // as that captures a single instance and asserts identity equality on encode, which fails because the
    // map codec (MapCodec.unit(constructor)) decodes a distinct instance per datapack load.
    return StreamCodec.of((buf, value) -> {}, buf -> constructor.get());
  }
}
