package slimeknights.mantle.util;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.MantleNetwork;
import slimeknights.mantle.network.packet.SwingArmPacket;

import javax.annotation.Nullable;

/**
 * Logic to handle offhand having its own cooldown.
 * <p>
 * In NeoForge 1.21 the per-player data that was previously stored via a Forge capability is stored as a
 * {@link AttachmentType data attachment}. The runtime cooldown state is transient (not serialized).
 */
public class OffhandCooldownTracker {
  /** Deferred register for the attachment type. Must be registered on the mod bus during construction via {@link #register(IEventBus)}. */
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mantle.modId);

  /** Attachment holding the offhand cooldown for a player. The default value captures the owning player so cooldowns can be timed against its tick count. */
  public static final DeferredHolder<AttachmentType<?>, AttachmentType<OffhandCooldownTracker>> ATTACHMENT =
    ATTACHMENTS.register("offhand_cooldown", () -> AttachmentType
      .builder(holder -> new OffhandCooldownTracker(holder instanceof Player player ? player : null))
      .build());

  /** Registers the attachment type with the mod event bus. Call during mod construction. */
  public static void register(IEventBus bus) {
    ATTACHMENTS.register(bus);
  }

  /** Retained for compatibility; the attachment requires no additional setup. */
  public static void init() {}

  /** Player receiving cooldowns */
  @Nullable
  private final Player player;
  /** Scale of the last cooldown */
  private int lastCooldown = 0;
  /** Time in ticks when the player can next attack for full power */
  private int attackReady = 0;

  /** Enables the cooldown tracker if above 0. Intended to be set in equipment change events, not serialized */
  private int enabled = 0;

  public OffhandCooldownTracker(@Nullable Player player) {
    this.player = player;
  }

  /** Null safe way to get the player's ticks existed */
  private int getTicksExisted() {
    if (player == null) {
      return 0;
    }
    return player.tickCount;
  }

  /** If true, the tracker is enabled despite a cooldown item not being held */
  @Deprecated(forRemoval = true)
  public boolean isEnabled() {
    return enabled > 0;
  }

  /**
   * Call this method when your item causing offhand cooldown to be needed is enabled and disabled. If multiple placces call this, the tracker will automatically keep enabled until all places disable
   * @param enable  If true, enable. If false, disable
   * @deprecated No longer used, so you can just remove calls.
   */
  @Deprecated(forRemoval = true)
  public void setEnabled(boolean enable) {
    if (enable) {
      enabled++;
    } else {
      enabled--;
    }
  }

  /**
   * Applies the given amount of cooldown
   * @param cooldown  Coolddown amount
   */
  public void applyCooldown(int cooldown) {
    this.lastCooldown = cooldown;
    this.attackReady = getTicksExisted() + cooldown;
  }

  /**
   * Returns a number from 0 to 1 denoting the current cooldown amount, akin to {@link Player#getAttackStrengthScale(float)}
   * @return  number from 0 to 1, with 1 being no cooldown
   */
  public float getCooldown() {
    int ticksExisted = getTicksExisted();
    if (ticksExisted > this.attackReady || this.lastCooldown == 0) {
      return 1.0f;
    }
    return Mth.clamp((this.lastCooldown + ticksExisted - this.attackReady) / (float) this.lastCooldown, 0f, 1f);
  }

  /**
   * Checks if we can perform another attack yet.
   * This counteracts rapid attacks via click macros, in a similar way to vanilla by limiting to once every 10 ticks
   */
  public boolean isAttackReady() {
    return getTicksExisted() + this.lastCooldown > this.attackReady;
  }


  /* Helpers */

  /** Gets the tracker instance for the target entity */
  @Nullable
  public static OffhandCooldownTracker get(Player player) {
    return player.getData(ATTACHMENT);
  }

  /**
   * Gets the offhand cooldown for the given player
   * @param player  Player
   * @return  Offhand cooldown
   */
  public static float getCooldown(Player player) {
    OffhandCooldownTracker tracker = get(player);
    return tracker != null ? tracker.getCooldown() : 1.0f;
  }

  /**
   * Applies cooldown to the given player
   * @param player  Player
   * @param cooldown  Cooldown to apply
   */
  public static void applyCooldown(Player player, int cooldown) {
    OffhandCooldownTracker tracker = get(player);
    if (tracker != null) {
      tracker.applyCooldown(cooldown);
    }
  }

  /**
   * Applies cooldown to the given player
   * @param player  Player
   */
  public static boolean isAttackReady(Player player) {
    OffhandCooldownTracker tracker = get(player);
    return tracker == null || tracker.isAttackReady();
  }

  /**
   * Applies cooldown using attack speed
   * @param attackSpeed   Attack speed of the held item
   * @param cooldownTime  Relative cooldown time for the given source, 20 is vanilla
   */
  public static void applyCooldown(Player player, float attackSpeed, int cooldownTime) {
    applyCooldown(player, Math.round(cooldownTime / attackSpeed));
  }

  /** Swings the entities hand without resetting cooldown */
  public static void swingHand(LivingEntity entity, InteractionHand hand, boolean updateSelf) {
    if (!entity.swinging || entity.swingTime >= entity.getCurrentSwingDuration() / 2 || entity.swingTime < 0) {
      entity.swingTime = -1;
      entity.swinging = true;
      entity.swingingArm = hand;
      if (!entity.level().isClientSide) {
        SwingArmPacket packet = new SwingArmPacket(entity, hand);
        if (updateSelf) {
          MantleNetwork.INSTANCE.sendToTrackingAndSelf(packet, entity);
        } else {
          MantleNetwork.INSTANCE.sendToTracking(packet, entity);
        }
      }
    }
  }
}
