package slimeknights.mantle.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import slimeknights.mantle.util.ItemStackList;

// Updated version of InventoryLogic in Mantle. Also contains a few bugfixes DOES NOT OVERRIDE createMenu
public abstract class InventoryBlockEntity extends NameableBlockEntity implements Container, MenuProvider, Nameable {
  private static final String TAG_INVENTORY_SIZE = "InventorySize";
  private static final String TAG_ITEMS = "Items";
  private static final String TAG_SLOT = "Slot";
  /**
   * Default stack size limit. Acts as a high ceiling so the real cap is each item's own max stack size
   * ({@link ItemStack#getMaxStackSize()}), matching the rest of the game. Previously this was a flat 64,
   * which capped tinker station / workbench slots at 64 even when a mod (e.g. Stack Size Tweaks) raised
   * item stacks to 256, producing a stack-size mismatch and an item duplication bug. Inventories that
   * truly need a smaller cap (e.g. the casting table at 1) still pass an explicit limit.
   */
  public static final int DEFAULT_STACK_SIZE_LIMIT = 1_000_000_000;

  private NonNullList<ItemStack> inventory;
  /** If true, the inventory size is saved to NBT, false means you are responsible for serializing it if it changes */
  private final boolean saveSizeToNBT;
  protected int stackSizeLimit;
  /**
   * Item handler exposing this inventory. In NeoForge 1.21.1 the capability is no longer provided via {@code getCapability};
   * the owning mod registers this handler in {@code RegisterCapabilitiesEvent} via
   * {@code event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.getItemHandler())}.
   */
  @Getter
  protected IItemHandlerModifiable itemHandler;

  /**
   * @param name Localization String for the inventory title. Can be overridden through setCustomName
   */
  public InventoryBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state, Component name, boolean saveSizeToNBT, int inventorySize) {
    this(tileEntityTypeIn, pos, state, name, saveSizeToNBT, inventorySize, DEFAULT_STACK_SIZE_LIMIT);
  }

  /**
   * @param name Localization String for the inventory title. Can be overridden through setCustomName
   */
  public InventoryBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state, Component name, boolean saveSizeToNBT, int inventorySize, int maxStackSize) {
    super(tileEntityTypeIn, pos, state, name);
    this.saveSizeToNBT = saveSizeToNBT;
    this.inventory = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
    this.stackSizeLimit = maxStackSize;
    this.itemHandler = new InvWrapper(this);
  }

  /* Inventory management */

  @Override
  public ItemStack getItem(int slot) {
    if (slot < 0 || slot >= this.inventory.size()) {
      return ItemStack.EMPTY;
    }

    return this.inventory.get(slot);
  }

  public boolean isStackInSlot(int slot) {
    return !this.getItem(slot).isEmpty();
  }

  /**
   * Same as resize, but does not call markDirty. Used on loading from NBT
   */
  private void resizeInternal(int size) {
    // save effort if the size did not change
    if (size == this.inventory.size()) {
      return;
    }
    ItemStackList newInventory = ItemStackList.withSize(size);

    for (int i = 0; i < size && i < this.inventory.size(); i++) {
      newInventory.set(i, this.inventory.get(i));
    }
    this.inventory = newInventory;
  }

  public void resize(int size) {
    this.resizeInternal(size);
    this.setChangedFast();
  }

  @Override
  public int getContainerSize() {
    return this.inventory.size();
  }

  @Override
  public int getMaxStackSize() {
    return this.stackSizeLimit;
  }

  /**
   * Per-item stack limit: the smaller of this inventory's configured limit and the item's own max stack size.
   * This keeps slots and storage in sync with the rest of the game (e.g. a 256-stack item stacks to 256 here,
   * a 16-stack item to 16, a tool to 1), instead of a flat 64 that mismatched modded stack sizes and duped items.
   */
  @Override
  public int getMaxStackSize(ItemStack stack) {
    return Math.min(this.stackSizeLimit, stack.getMaxStackSize());
  }

  @Override
  public void setItem(int slot, ItemStack itemstack) {
    if (slot < 0 || slot >= this.inventory.size()) {
      return;
    }

    ItemStack current = this.inventory.get(slot);
    this.inventory.set(slot, itemstack);

    int limit = this.getMaxStackSize(itemstack);
    if (!itemstack.isEmpty() && itemstack.getCount() > limit) {
      itemstack.setCount(limit);
    }
    if (!ItemStack.matches(current, itemstack)) {
      this.setChangedFast();
    }
  }

  @Override
  public ItemStack removeItem(int slot, int quantity) {
    if (quantity <= 0) {
      return ItemStack.EMPTY;
    }
    ItemStack itemStack = this.getItem(slot);

    if (itemStack.isEmpty()) {
      return ItemStack.EMPTY;
    }

    // whole itemstack taken out
    if (itemStack.getCount() <= quantity) {
      this.setItem(slot, ItemStack.EMPTY);
      this.setChangedFast();
      return itemStack;
    }

    // split itemstack
    itemStack = itemStack.split(quantity);
    // slot is empty, set to ItemStack.EMPTY
    // isn't this redundant to the above check?
    if (this.getItem(slot).getCount() == 0) {
      this.setItem(slot, ItemStack.EMPTY);
    }

    this.setChangedFast();
    // return remainder
    return itemStack;
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    ItemStack itemStack = this.getItem(slot);
    this.setItem(slot, ItemStack.EMPTY);
    return itemStack;
  }

  @Override
  public boolean canPlaceItem(int slot, ItemStack itemstack) {
    if (slot < this.getContainerSize()) {
      return this.inventory.get(slot).isEmpty() || itemstack.getCount() + this.inventory.get(slot).getCount() <= this.getMaxStackSize(itemstack);
    }
    return false;
  }

  @Override
  public void clearContent() {
    for (int i = 0; i < this.inventory.size(); i++) {
      this.inventory.set(i, ItemStack.EMPTY);
    }
  }

  /* Supporting methods */
  @Override
  public boolean stillValid(Player entityplayer) {
    // block changed/got broken?
    if (level == null || this.level.getBlockEntity(this.worldPosition) != this || this.getBlockState().getBlock() == Blocks.AIR) {
      return false;
    }

    return entityplayer.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= 64D;
  }

  @Override
  public void startOpen(Player player) {}

  @Override
  public void stopOpen(Player player) {}

  /* NBT */

  @Override
  public void loadAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.loadAdditional(tags, registries);
    if (saveSizeToNBT) {
      this.resizeInternal(tags.getInt(TAG_INVENTORY_SIZE));
    }
    this.readInventoryFromNBT(tags, registries);
  }

  @Override
  public void saveSynced(CompoundTag tags, HolderLookup.Provider registries) {
    super.saveSynced(tags, registries);
    // only sync the size to the client by default
    if (saveSizeToNBT) {
      tags.putInt(TAG_INVENTORY_SIZE, this.inventory.size());
    }
  }

  @Override
  public void saveAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.saveAdditional(tags, registries);
    this.writeInventoryToNBT(tags, registries);
  }

  /**
   * Writes the contents of the inventory to the tag
   */
  public void writeInventoryToNBT(CompoundTag tag, HolderLookup.Provider registries) {
    Container inventory = this;
    ListTag nbttaglist = new ListTag();

    for (int i = 0; i < inventory.getContainerSize(); i++) {
      if (!inventory.getItem(i).isEmpty()) {
        CompoundTag itemTag = new CompoundTag();
        itemTag.putByte(TAG_SLOT, (byte) i);
        // 1.21: ItemStack.save() RETURNS the encoded tag and does not mutate the prefix in place
        // (1.20 mutated it). The old code ignored the return, so each entry was written as just
        // {Slot:i} with no item id -> items were lost on save and never synced (casting table items
        // invisible, "No key id in MapLike[{Slot:Nb}]" errors). Store the returned merged tag.
        nbttaglist.add(inventory.getItem(i).save(registries, itemTag));
      }
    }

    tag.put(TAG_ITEMS, nbttaglist);
  }

  /**
   * Reads an inventory from the tag. Overwrites current content
   */
  public void readInventoryFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);

    for (int i = 0; i < list.size(); ++i) {
      CompoundTag itemTag = list.getCompound(i);
      int slot = itemTag.getByte(TAG_SLOT) & 255;
      if (slot < this.inventory.size()) {
        // 1.21: the slot compound carries a "Slot" byte alongside the item; an empty slot saved as
        // just {Slot:N} (no "id") makes the strict ItemStack codec throw ("No key id in MapLike"),
        // which dropped the synced item client-side (casting table contents looked invisible).
        ItemStack stack = itemTag.contains("id") ? ItemStack.parseOptional(registries, itemTag) : ItemStack.EMPTY;
        int limit = this.getMaxStackSize(stack);
        if (!stack.isEmpty() && stack.getCount() > limit) {
          stack.setCount(limit);
        }
        this.inventory.set(slot, stack);
      }
    }
  }

  @Override
  public boolean isEmpty() {
    for (ItemStack itemstack : this.inventory) {
      if (!itemstack.isEmpty()) {
        return false;
      }
    }

    return true;
  }
}
