package slimeknights.mantle.fluid.transfer;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Packet to sync fluid container transfer */
@RequiredArgsConstructor
public class FluidContainerTransferPacket implements IThreadsafePacket {
  public static final Type<FluidContainerTransferPacket> TYPE = new Type<>(Mantle.getResource("fluid_container_transfer"));
  public static final StreamCodec<RegistryFriendlyByteBuf,FluidContainerTransferPacket> STREAM_CODEC = ISimplePacket.codec(FluidContainerTransferPacket::new);

  private final Set<Item> items;

  public FluidContainerTransferPacket(FriendlyByteBuf buffer) {
    int size = buffer.readVarInt();
    List<Item> builder = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      builder.add(BuiltInRegistries.ITEM.byIdOrThrow(buffer.readVarInt()));
    }
    this.items = Set.copyOf(builder);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeVarInt(items.size());
    for (Item item : items) {
      buffer.writeVarInt(BuiltInRegistries.ITEM.getId(item));
    }
  }

  @Override
  public Type<FluidContainerTransferPacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    FluidContainerTransferManager.INSTANCE.setContainerItems(items);
  }
}
