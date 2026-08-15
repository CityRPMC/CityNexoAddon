package zone.vao.nexoAddon.items.mechanics;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import zone.vao.nexoAddon.NexoAddon;
import zone.vao.nexoAddon.items.Mechanics;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Harvests mature LiteFarm crops without using Nexo's vanilla harvesting mechanic.
 *
 * <p>LiteFarm does not expose a public harvest method. A BlockBreakEvent is therefore
 * fired for each mature crop so LiteFarm remains responsible for protection checks,
 * level and permission requirements, rewards, commands, events, and replanting.</p>
 */
public record LiteFarmHarvesting(int radius, int height, double cooldown, boolean lowerItemDurability) {

  public static boolean isLiteFarmHarvestingTool(String itemId) {
    if (itemId == null) return false;
    Mechanics mechanics = NexoAddon.getInstance().getMechanics().get(itemId);
    return mechanics != null && mechanics.getLiteFarmHarvesting() != null;
  }

  public static class LiteFarmHarvestingListener implements Listener {
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final LiteFarmBridge liteFarm = new LiteFarmBridge();

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
      if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

      Block clickedBlock = event.getClickedBlock();
      if (clickedBlock == null || !liteFarm.isAvailable()) return;

      Player player = event.getPlayer();
      ItemStack tool = player.getInventory().getItemInMainHand();
      String itemId = NexoItems.idFromItem(tool);
      if (!isLiteFarmHarvestingTool(itemId)) return;

      Block clickedPlant = liteFarm.findPlant(clickedBlock);
      if (clickedPlant == null) return;

      LiteFarmHarvesting mechanic = NexoAddon.getInstance().getMechanics().get(itemId).getLiteFarmHarvesting();
      long now = System.currentTimeMillis();
      if (cooldowns.getOrDefault(player.getUniqueId(), 0L) > now) return;

      event.setCancelled(true);
      int harvested = harvestArea(player, clickedPlant, mechanic);
      if (harvested == 0) return;

      if (mechanic.cooldown() > 0) {
        cooldowns.put(player.getUniqueId(), now + (long) (mechanic.cooldown() * 1000L));
      }
      if (mechanic.lowerItemDurability()) {
        tool.damage(harvested, player);
      }
      player.swingMainHand();
    }

    private int harvestArea(Player player, Block origin, LiteFarmHarvesting mechanic) {
      int harvested = 0;
      int horizontalOffset = mechanic.radius() / 2;
      int verticalOffset = mechanic.height() / 2;
      Location center = origin.getLocation();

      for (int x = center.getBlockX() - horizontalOffset; x <= center.getBlockX() + horizontalOffset; x++) {
        for (int y = center.getBlockY() - verticalOffset; y <= center.getBlockY() + verticalOffset; y++) {
          for (int z = center.getBlockZ() - horizontalOffset; z <= center.getBlockZ() + horizontalOffset; z++) {
            Block block = center.getWorld().getBlockAt(x, y, z);
            if (!liteFarm.isMature(block)) continue;
            if (!ProtectionLib.canBreak(player, block.getLocation()) || !ProtectionLib.canBuild(player, block.getLocation())) continue;

            BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
            Bukkit.getPluginManager().callEvent(breakEvent);

            // LiteFarm either removes the plant or resets its harvest timer when successful.
            if (!liteFarm.isPlant(block) || !liteFarm.isMature(block)) harvested++;
          }
        }
      }
      return harvested;
    }
  }

  private static final class LiteFarmBridge {
    private Method isPlantMethod;
    private Method getPlantMethod;
    private Field harvestTimeField;
    private boolean initialized;
    private boolean available;
    private boolean errorLogged;

    boolean isAvailable() {
      initialize();
      return available;
    }

    Block findPlant(Block block) {
      if (isPlant(block)) return block;
      if (isPlant(block.getRelative(0, -1, 0))) return block.getRelative(0, -1, 0);
      if (isPlant(block.getRelative(0, 1, 0))) return block.getRelative(0, 1, 0);
      return null;
    }

    boolean isPlant(Block block) {
      initialize();
      if (!available) return false;
      try {
        return (boolean) isPlantMethod.invoke(null, block.getLocation());
      } catch (IllegalAccessException | InvocationTargetException exception) {
        logError(exception);
        return false;
      }
    }

    boolean isMature(Block block) {
      initialize();
      if (!available) return false;
      try {
        Object plant = getPlantMethod.invoke(null, block);
        return plant != null && harvestTimeField.getInt(plant) <= 0;
      } catch (IllegalAccessException | InvocationTargetException exception) {
        logError(exception);
        return false;
      }
    }

    private void initialize() {
      if (initialized) return;
      initialized = true;
      if (!Bukkit.getPluginManager().isPluginEnabled("LiteFarm")) return;

      try {
        Class<?> apiClass = Class.forName("com.azlagor.litefarm.API.API");
        Class<?> plantClass = Class.forName("com.azlagor.litefarm.data.SimplePlant");
        isPlantMethod = apiClass.getMethod("isPlant", Location.class);
        getPlantMethod = apiClass.getMethod("get_plant", Block.class);
        harvestTimeField = plantClass.getField("ht");
        available = true;
      } catch (ReflectiveOperationException exception) {
        logError(exception);
      }
    }

    private void logError(Exception exception) {
      available = false;
      if (errorLogged) return;
      errorLogged = true;
      NexoAddon.getInstance().getLogger().warning(
          "LiteFarm harvesting integration could not be initialized for this LiteFarm version: " + exception.getMessage());
    }
  }
}
