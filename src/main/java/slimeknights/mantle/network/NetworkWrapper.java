package slimeknights.mantle.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import slimeknights.mantle.network.packet.ISimplePacket;

import java.util.function.Function;

/**
 * A small network implementation/wrapper around NeoForge's {@link PayloadRegistrar} payload system.
 * Instantiate in your mod class, then register your packets from a {@link RegisterPayloadHandlersEvent} listener via
 * {@link #getRegistrar(RegisterPayloadHandlersEvent)} and the {@code registerPacket} helpers.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NetworkWrapper {
  /** Channel name, also used as the payload registrar namespace */
  public final ResourceLocation channelName;
  /** Protocol version, validated by both sides */
  public final String version;

  /**
   * Creates a new network wrapper
   * @param channelName  Unique packet channel name
   * @deprecated Give your channel a version number.
   */
  @Deprecated
  public NetworkWrapper(ResourceLocation channelName) {
    this(channelName, "1");
  }

  public NetworkWrapper(ResourceLocation channelName, String version) {
    this.channelName = channelName;
    this.version = version;
  }

  /**
   * Gets the {@link PayloadRegistrar} for this network from the registration event. Register all packets on the returned
   * registrar, typically by passing it to the {@code registerPacket} helpers below.
   * @param event  Payload registration event, fired on the mod bus
   * @return  Registrar scoped to this network's namespace and version
   */
  public PayloadRegistrar getRegistrar(RegisterPayloadHandlersEvent event) {
    return event.registrar(channelName.getNamespace()).versioned(version);
  }

  /**
   * Registers a new {@link ISimplePacket} that is sent from the server to the client
   * @param registrar  Payload registrar, see {@link #getRegistrar(RegisterPayloadHandlersEvent)}
   * @param type       Packet payload type
   * @param decoder    Packet decoder, typically the buffer constructor
   * @param <MSG>      Packet class type
   */
  public <MSG extends ISimplePacket> void registerToClient(PayloadRegistrar registrar, Type<MSG> type, Function<net.minecraft.network.FriendlyByteBuf,MSG> decoder) {
    registrar.playToClient(type, ISimplePacket.codec(decoder), ISimplePacket::handle);
  }

  /**
   * Registers a new {@link ISimplePacket} that is sent from the client to the server
   * @param registrar  Payload registrar, see {@link #getRegistrar(RegisterPayloadHandlersEvent)}
   * @param type       Packet payload type
   * @param decoder    Packet decoder, typically the buffer constructor
   * @param <MSG>      Packet class type
   */
  public <MSG extends ISimplePacket> void registerToServer(PayloadRegistrar registrar, Type<MSG> type, Function<net.minecraft.network.FriendlyByteBuf,MSG> decoder) {
    registrar.playToServer(type, ISimplePacket.codec(decoder), ISimplePacket::handle);
  }

  /**
   * Registers a new generic packet, allowing a custom stream codec and handler
   * @param registrar  Payload registrar, see {@link #getRegistrar(RegisterPayloadHandlersEvent)}
   * @param type       Packet payload type
   * @param codec      Stream codec for the packet
   * @param handler    Logic to handle a packet
   * @param toServer   If true, the packet is registered to send to the server; otherwise to the client
   * @param <MSG>      Packet class type
   */
  public <MSG extends CustomPacketPayload> void registerPacket(PayloadRegistrar registrar, Type<MSG> type, StreamCodec<? super RegistryFriendlyByteBuf,MSG> codec, IPayloadHandler<MSG> handler, boolean toServer) {
    if (toServer) {
      registrar.playToServer(type, codec, handler);
    } else {
      registrar.playToClient(type, codec, handler);
    }
  }


  /* Sending packets */

  /**
   * Sends a packet to the server
   * @param payload  Packet to send
   */
  public void sendToServer(CustomPacketPayload payload) {
    PacketDistributor.sendToServer(payload);
  }

  /**
   * Sends a vanilla packet to the given entity
   * @param packet  Packet
   * @param player  Player receiving the packet
   */
  public void sendVanillaPacket(Packet<?> packet, Entity player) {
    if (player instanceof ServerPlayer sPlayer) {
      sPlayer.connection.send(packet);
    }
  }

  /**
   * Sends a packet to a player
   * @param payload  Packet
   * @param player   Player to send
   */
  public void sendTo(CustomPacketPayload payload, Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      sendTo(payload, serverPlayer);
    }
  }

  /**
   * Sends a packet to a player
   * @param payload  Packet
   * @param player   Player to send
   */
  public void sendTo(CustomPacketPayload payload, ServerPlayer player) {
    if (!(player instanceof FakePlayer)) {
      PacketDistributor.sendToPlayer(player, payload);
    }
  }

  /**
   * Sends a packet to players near a location
   * @param payload      Packet to send
   * @param serverWorld  World instance
   * @param position     Position within range
   */
  public void sendToClientsAround(CustomPacketPayload payload, ServerLevel serverWorld, BlockPos position) {
    PacketDistributor.sendToPlayersTrackingChunk(serverWorld, new ChunkPos(position), payload);
  }

  /**
   * Sends a packet to all entities tracking the given entity, plus the entity itself if it is a player
   * @param payload  Packet
   * @param entity   Entity to check
   */
  public void sendToTrackingAndSelf(CustomPacketPayload payload, Entity entity) {
    PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
  }

  /**
   * Sends a packet to all entities tracking the given entity
   * @param payload  Packet
   * @param entity   Entity to check
   */
  public void sendToTracking(CustomPacketPayload payload, Entity entity) {
    PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
  }
}
