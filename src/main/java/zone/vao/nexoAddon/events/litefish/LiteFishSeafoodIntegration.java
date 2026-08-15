package zone.vao.nexoAddon.events.litefish;

import com.nexomc.nexo.api.NexoItems;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import zone.vao.nexoAddon.NexoAddon;
import zone.vao.nexoAddon.items.Mechanics;
import zone.vao.nexoAddon.items.mechanics.LiteFishSeafood;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies temporary LiteFish bonuses after players consume configured Nexo foods.
 *
 * <p>LiteFish is accessed reflectively so it remains an optional dependency. LiteFish
 * 5.9.8 does not honor replacement drop lists from its API setters, so bonus catches
 * must be appended to the list returned by CatchEvent#getDrop().</p>
 */
public final class LiteFishSeafoodIntegration implements Listener {
  private static final String CONFIG_PATH = "litefish_seafood.foods";

  private final NexoAddon plugin;
  private final Map<UUID, ActiveBuff> activeBuffs = new ConcurrentHashMap<>();
  private boolean bridgeErrorLogged;

  public LiteFishSeafoodIntegration(NexoAddon plugin) {
    this.plugin = plugin;
  }

  public void registerLiteFishEvents() {
    if (!plugin.getConfig().getBoolean("litefish_seafood.enabled", true)) return;

    Plugin liteFish = Bukkit.getPluginManager().getPlugin("LiteFish");
    if (liteFish == null || !liteFish.isEnabled()) {
      plugin.getLogger().info("LiteFish not found; seafood fishing buffs are disabled.");
      return;
    }

    try {
      ClassLoader classLoader = liteFish.getClass().getClassLoader();
      Class<? extends Event> startEventClass = classLoader
          .loadClass("dev.nekomadev.liteFish.api.StartFishingEvent")
          .asSubclass(Event.class);
      Class<? extends Event> catchEventClass = classLoader
          .loadClass("dev.nekomadev.liteFish.api.CatchEvent")
          .asSubclass(Event.class);

      Method startGetPlayer = startEventClass.getMethod("getPlayer");
      Method startGetModificator = startEventClass.getMethod("getModificator");
      Method catchGetPlayer = catchEventClass.getMethod("getPlayer");
      Method catchGetDrop = catchEventClass.getMethod("getDrop");
      Method catchGetReason = catchEventClass.getMethod("getReason");

      // LiteFish's LiteEvent superclass shares one HandlerList across its API events.
      // Explicit type checks prevent one event's executor from handling another event.
      EventExecutor startExecutor = (listener, event) -> {
        if (startEventClass.isInstance(event)) {
          handleStart(event, startGetPlayer, startGetModificator);
        }
      };
      EventExecutor catchExecutor = (listener, event) -> {
        if (catchEventClass.isInstance(event)) {
          handleCatch(event, catchGetPlayer, catchGetDrop, catchGetReason);
        }
      };

      Bukkit.getPluginManager().registerEvent(
          startEventClass, this, EventPriority.NORMAL, startExecutor, plugin, true);
      Bukkit.getPluginManager().registerEvent(
          catchEventClass, this, EventPriority.NORMAL, catchExecutor, plugin, false);
      plugin.getLogger().info("LiteFish seafood buffs enabled.");
    } catch (ReflectiveOperationException exception) {
      logBridgeError(exception);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onConsume(PlayerItemConsumeEvent event) {
    if (!plugin.getConfig().getBoolean("litefish_seafood.enabled", true)) return;

    String itemId = NexoItems.idFromItem(event.getItem());
    if (itemId == null) return;

    LiteFishSeafood buff = loadBuff(itemId);
    if (buff == null || buff.durationSeconds() <= 0) return;

    long expiresAt = System.currentTimeMillis() + Math.round(buff.durationSeconds() * 1000.0);
    activeBuffs.put(event.getPlayer().getUniqueId(), new ActiveBuff(buff, expiresAt));
    sendConfiguredMessage(event.getPlayer(), "messages.seafood_buff.activated", buff);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    activeBuffs.remove(event.getPlayer().getUniqueId());
  }

  private void handleStart(Event event, Method getPlayer, Method getModificator) {
    try {
      Player player = (Player) getPlayer.invoke(event);
      ActiveBuff active = activeBuff(player);
      if (active == null) return;

      Object modificator = getModificator.invoke(event);
      if (modificator == null) return;

      LiteFishSeafood buff = active.buff();
      addIntField(modificator, "speed_inc", buff.gameSpeed());
      addIntField(modificator, "size_inc", buff.gameSize());
      addIntField(modificator, "player_health", buff.playerHealth());
      addIntField(modificator, "drop_health", buff.dropHealth());
    } catch (ReflectiveOperationException exception) {
      logBridgeError(exception);
    }
  }

  @SuppressWarnings("unchecked")
  private void handleCatch(Event event, Method getPlayer, Method getDrop, Method getReason) {
    try {
      if (!"SUCCESS".equals(String.valueOf(getReason.invoke(event)))) return;

      Player player = (Player) getPlayer.invoke(event);
      ActiveBuff active = activeBuff(player);
      if (active == null) return;

      double chance = active.buff().bonusCatchChance();
      if (chance <= 0 || ThreadLocalRandom.current().nextDouble() >= chance) return;

      Object rawDrops = getDrop.invoke(event);
      if (!(rawDrops instanceof List<?> drops) || drops.isEmpty()) return;

      List<ItemStack> bonusDrops = new ArrayList<>();
      for (Object drop : drops) {
        if (drop instanceof ItemStack item && !item.isEmpty()) bonusDrops.add(item.clone());
      }
      if (bonusDrops.isEmpty()) return;

      ((List<Object>) rawDrops).addAll(bonusDrops);
      sendConfiguredMessage(player, "messages.seafood_buff.bonus_catch", active.buff());
    } catch (ReflectiveOperationException | UnsupportedOperationException exception) {
      logBridgeError(exception);
    }
  }

  private ActiveBuff activeBuff(Player player) {
    ActiveBuff active = activeBuffs.get(player.getUniqueId());
    if (active == null) return null;
    if (active.expiresAt() > System.currentTimeMillis()) return active;

    activeBuffs.remove(player.getUniqueId(), active);
    return null;
  }

  private LiteFishSeafood loadBuff(String itemId) {
    Mechanics mechanics = plugin.getMechanics().get(itemId);
    if (mechanics != null && mechanics.getLiteFishSeafood() != null) {
      return mechanics.getLiteFishSeafood();
    }

    ConfigurationSection section = plugin.getConfig().getConfigurationSection(CONFIG_PATH + "." + itemId);
    if (section == null) return null;

    return new LiteFishSeafood(
        section.getString("name", itemId),
        Math.max(0.0, section.getDouble("duration_seconds", 300.0)),
        Math.clamp(section.getDouble("bonus_catch_chance", 0.0), 0.0, 1.0),
        section.getInt("minigame.speed", 0),
        section.getInt("minigame.size", 0),
        section.getInt("minigame.player_health", 0),
        section.getInt("minigame.drop_health", 0));
  }

  private void addIntField(Object target, String fieldName, int amount) throws ReflectiveOperationException {
    if (amount == 0) return;
    Field field = target.getClass().getField(fieldName);
    field.setInt(target, field.getInt(target) + amount);
  }

  private void sendConfiguredMessage(Player player, String path, LiteFishSeafood buff) {
    String message = plugin.getConfig().getString(path, "");
    if (message == null || message.isBlank()) return;

    message = message
        .replace("%buff%", buff.name())
        .replace("%duration%", formatDuration(buff.durationSeconds()));
    player.sendActionBar(MiniMessage.miniMessage().deserialize(message));
  }

  private String formatDuration(double seconds) {
    if (seconds % 60.0 == 0) return Math.round(seconds / 60.0) + "m";
    return Math.round(seconds) + "s";
  }

  private void logBridgeError(Exception exception) {
    if (bridgeErrorLogged) return;
    bridgeErrorLogged = true;
    plugin.getLogger().warning(
        "LiteFish seafood integration is incompatible with this LiteFish version: " + exception.getMessage());
  }

  private record ActiveBuff(LiteFishSeafood buff, long expiresAt) {}
}
