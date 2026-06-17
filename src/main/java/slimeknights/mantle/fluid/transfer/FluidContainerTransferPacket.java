package slimeknights.mantle.fluid.transfer;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.NetworkEvent.Context;
import net.neoforged.neoforge.registries.ForgeRegistries;
import slimeknights.mantle.network.packet.IThreadsafePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Packet to sync fluid container transfer */
@RequiredArgsConstructor
public class FluidContainerTransferPacket implements IThreadsafePacket {
  private final Set<Item> items;

  public FluidContainerTransferPacket(FriendlyByteBuf buffer) {
    int size = buffer.readVarInt();
    List<Item> builder = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      builder.add(buffer.readRegistryIdUnsafe(ForgeRegistries.ITEMS));
    }
    this.items = Set.copyOf(builder);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeVarInt(items.size());
    for (Item item : items) {
      buffer.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, item);
    }
  }

  @Override
  public void handleThreadsafe(Context context) {
    FluidContainerTransferManager.INSTANCE.setContainerItems(items);
  }
}
