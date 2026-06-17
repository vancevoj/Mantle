package slimeknights.mantle.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Function;

/**
 * Packet interface to add common methods for registration.
 * <p>
 * In NeoForge 1.21.1 every packet is a {@link CustomPacketPayload}; the {@link #encode(FriendlyByteBuf)} method and
 * {@link #handle(IPayloadContext)} method below mirror the old Forge {@code encode}/{@code handle} pair so packet
 * implementations stay nearly identical. Implementors additionally declare a {@code Type<X> TYPE}, a {@code StreamCodec},
 * and the {@link #type()} method required by {@link CustomPacketPayload}.
 */
public interface ISimplePacket extends CustomPacketPayload {
  /**
   * Encodes a packet for the buffer
   * @param buf  Buffer instance
   */
  void encode(FriendlyByteBuf buf);

  /**
   * Handles receiving the packet
   * @param context  Packet context
   */
  void handle(IPayloadContext context);

  /**
   * Helper to build a {@link StreamCodec} for a simple packet from its {@link #encode(FriendlyByteBuf)} method and a
   * buffer constructor. Use to populate the {@code STREAM_CODEC} field on each packet.
   * @param decoder  Packet decoder, typically the buffer constructor
   * @param <MSG>    Packet type
   * @return  Stream codec for the packet
   */
  static <MSG extends ISimplePacket> StreamCodec<RegistryFriendlyByteBuf,MSG> codec(Function<FriendlyByteBuf,MSG> decoder) {
    return CustomPacketPayload.codec(ISimplePacket::encode, decoder::apply);
  }
}
