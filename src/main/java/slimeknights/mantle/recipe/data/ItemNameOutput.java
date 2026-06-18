package slimeknights.mantle.recipe.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.common.NBTLoadable;
import slimeknights.mantle.recipe.helper.ItemOutput;

import javax.annotation.Nullable;

/**
 * Extension of {@link ItemOutput} for datagen of recipes for compat. Should never be used in an actual recipe
 */
public class ItemNameOutput extends ItemOutput {
  private final ResourceLocation name;
  private final int count;
  @Nullable
  private final CompoundTag nbt;

  protected ItemNameOutput(ResourceLocation name, int count, @Nullable CompoundTag nbt) {
    this.name = name;
    this.count = count;
    this.nbt = nbt;
  }

  /**
   * Creates an output for the given item, count, and NBT
   * @param name   Item name
   * @param count  Count
   * @param nbt    NBT, may be null
   * @return  Output
   */
  public static ItemNameOutput fromName(ResourceLocation name, int count, @Nullable CompoundTag nbt) {
    return new ItemNameOutput(name, count, nbt);
  }

  /**
   * Creates an output for the given item with no NBT
   * @param name   Item name
   * @param count  Count
   * @return  Output
   */
  public static ItemNameOutput fromName(ResourceLocation name, int count) {
    return fromName(name, count, null);
  }

  /**
   * Creates an output for the given item with a count of 1
   * @param name  Item name
   * @return  Output
   */
  public static ItemNameOutput fromName(ResourceLocation name) {
    return fromName(name, 1);
  }

  @Override
  public ItemStack get() {
    throw new UnsupportedOperationException("Cannot get the item stack from a item name output");
  }

  @Override
  public int getCount() {
    return count;
  }

  @Override
  public JsonElement serialize(boolean writeCount) {
    String itemName = name.toString();
    if (nbt == null && (count <= 1 || !writeCount)) {
      return new JsonPrimitive(itemName);
    } else {
      JsonObject jsonResult = new JsonObject();
      jsonResult.addProperty("item", itemName);
      if (writeCount) {
        jsonResult.addProperty("count", count);
      }
      if (nbt != null) {
        jsonResult.add("nbt", NBTLoadable.ALLOW_STRING.serialize(nbt));
      }
      return jsonResult;
    }
  }
}
