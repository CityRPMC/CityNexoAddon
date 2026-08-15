package zone.vao.nexoAddon.items.mechanics;

import com.nexomc.nexo.api.NexoItems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import zone.vao.nexoAddon.NexoAddon;
import zone.vao.nexoAddon.items.Mechanics;

import java.util.List;

public record Repair(double ratio, int fixedAmount, List<Material> materials, List<String> nexoIds,
                     List<Material> materialsBlacklist, List<String> nexoIdsBlacklist,
                     List<String> weaponMechanicsTitles, List<String> weaponMechanicsTitlesBlacklist) {

  private static final NamespacedKey WEAPON_TITLE_KEY = new NamespacedKey("weaponmechanics", "weapon-title");

  public static class RepairListener implements Listener {

    @EventHandler
    public static void onInventoryClick(InventoryClickEvent event) {
      if (!isValidClick(event)) return;

      Player player = (Player) event.getWhoClicked();
      ItemStack cursorItem = event.getCursor().clone();
      ItemStack currentItem = event.getCurrentItem().clone();

      String repairItemId = NexoItems.idFromItem(cursorItem);
      if (!canRepair(repairItemId, currentItem)) return;

      event.setCancelled(true);

      repairItem(player, cursorItem, currentItem, repairItemId);
      updatePlayerInventory(player, currentItem, cursorItem, event);
    }

    private static boolean isValidClick(InventoryClickEvent event) {
      return event.getWhoClicked() instanceof Player &&
          event.isLeftClick() &&
          event.getCursor() != null &&
          event.getCurrentItem() != null;
    }

    private static boolean canRepair(String repairItemId, ItemStack currentItem) {
      if (repairItemId == null) return false;

      if (!(currentItem.getItemMeta() instanceof Damageable itemMeta) || !itemMeta.hasDamage()) return false;

      Mechanics mechanics = NexoAddon.getInstance().getMechanics().get(repairItemId);
      if (mechanics == null || mechanics.getRepair() == null) return false;

      Repair repair = mechanics.getRepair();
      if (!isItemAllowed(currentItem, repair)) {
        return false;
      }

      String currentItemId = NexoItems.idFromItem(currentItem);
      if (currentItemId != null) {
        Mechanics currentMechanics = NexoAddon.getInstance().getMechanics().get(currentItemId);
        if (currentMechanics != null && currentMechanics.getRepair() != null) return false;
      }

      return true;
    }

    private static void repairItem(Player player, ItemStack cursorItem, ItemStack currentItem, String repairItemId) {
      Mechanics mechanic = NexoAddon.getInstance().getMechanics().get(repairItemId);
      double repairRatio = mechanic.getRepair().ratio();
      int fixedAmount = mechanic.getRepair().fixedAmount();
      int maxDurability = NexoItems.itemFromId(repairItemId).getMaxDamage() != null ? NexoItems.itemFromId(repairItemId).getMaxDamage() : NexoItems.itemFromId(repairItemId).build().getType().getMaxDurability();
      if(repairRatio > 0) {

        Damageable currentMeta = (Damageable) currentItem.getItemMeta();
        int repairAmount = (int) Math.ceil((currentMeta.getDamage() * repairRatio));
        currentMeta.setDamage(Math.max(0, currentMeta.getDamage() - repairAmount));
        currentItem.setItemMeta(currentMeta);

        if (maxDurability > 0) {
          updateCursorItemWithDurability(cursorItem, maxDurability);
        } else {
          reduceCursorItemAmount(cursorItem);
        }
      }else if(fixedAmount > 0){

        Damageable currentMeta = (Damageable) currentItem.getItemMeta();
        currentMeta.setDamage(Math.max(0, currentMeta.getDamage() - fixedAmount));
        currentItem.setItemMeta(currentMeta);

        if (maxDurability > 0) {
          updateCursorItemWithDurability(cursorItem, maxDurability);
        } else {
          reduceCursorItemAmount(cursorItem);
        }
      }
    }

    private static void updateCursorItemWithDurability(ItemStack cursorItem, int maxDurability) {
      Damageable cursorMeta = (Damageable) cursorItem.getItemMeta();
      int currentDamage = cursorMeta.getDamage();
      int newDamage = currentDamage + 1;

      if (newDamage >= maxDurability) {
        cursorItem.setAmount(cursorItem.getAmount() - 1);
        if (cursorItem.getAmount() <= 0) {
          cursorItem.setType(Material.AIR);
        } else {
          cursorMeta.setDamage(0);
          cursorItem.setItemMeta(cursorMeta);
        }
      } else {
        cursorMeta.setDamage(newDamage);
        cursorItem.setItemMeta(cursorMeta);
      }
    }

    private static void reduceCursorItemAmount(ItemStack cursorItem) {
      if (cursorItem.getAmount() > 1) {
        cursorItem.setAmount(cursorItem.getAmount() - 1);
      } else {
        cursorItem.setType(Material.AIR);
      }
    }

    private static void updatePlayerInventory(Player player, ItemStack currentItem, ItemStack cursorItem, InventoryClickEvent event) {
      event.getClickedInventory().setItem(event.getSlot(), currentItem);
      player.setItemOnCursor(cursorItem == null ? new ItemStack(Material.AIR) : cursorItem);
      player.updateInventory();
    }

    private static boolean isItemAllowed(ItemStack item, Repair repair) {
      if (item == null || item.getType() == Material.AIR) {
        return false;
      }
      String nexoId = NexoItems.idFromItem(item);
      String weaponTitle = getWeaponMechanicsTitle(item);
      boolean whitelistDefined = !repair.materials().isEmpty() || !repair.nexoIds().isEmpty()
          || !repair.weaponMechanicsTitles().isEmpty();
      boolean blacklistDefined = !repair.materialsBlacklist().isEmpty() || !repair.nexoIdsBlacklist().isEmpty()
          || !repair.weaponMechanicsTitlesBlacklist().isEmpty();
      if (blacklistDefined) {
        if (repair.materialsBlacklist().contains(item.getType())) {
          return false;
        }
        if (nexoId != null && repair.nexoIdsBlacklist().contains(nexoId)) {
          return false;
        }
        if (weaponTitle != null && matchesAnyWeaponTitle(weaponTitle, repair.weaponMechanicsTitlesBlacklist())) {
          return false;
        }
      }
      if (whitelistDefined) {
        boolean whitelisted = repair.materials().contains(item.getType());
        if (!whitelisted && nexoId != null) whitelisted = repair.nexoIds().contains(nexoId);
        if (!whitelisted && weaponTitle != null)
          whitelisted = matchesAnyWeaponTitle(weaponTitle, repair.weaponMechanicsTitles());
        if (!whitelisted) {
          return false;
        }
      }
      return true;
    }

    private static String getWeaponMechanicsTitle(ItemStack item) {
      return item.getItemMeta().getPersistentDataContainer()
          .get(WEAPON_TITLE_KEY, PersistentDataType.STRING);
    }

    private static boolean matchesAnyWeaponTitle(String weaponTitle, List<String> patterns) {
      for (String pattern : patterns) {
        if (matchesWeaponTitle(weaponTitle, pattern)) return true;
      }
      return false;
    }

    /** Supports exact titles as well as '*' and '?' wildcards, case-insensitively. */
    private static boolean matchesWeaponTitle(String weaponTitle, String pattern) {
      int titleIndex = 0;
      int patternIndex = 0;
      int starIndex = -1;
      int matchAfterStar = -1;

      while (titleIndex < weaponTitle.length()) {
        if (patternIndex < pattern.length()
            && (pattern.charAt(patternIndex) == '?'
                || Character.toLowerCase(pattern.charAt(patternIndex)) == Character.toLowerCase(weaponTitle.charAt(titleIndex)))) {
          patternIndex++;
          titleIndex++;
        } else if (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
          starIndex = patternIndex++;
          matchAfterStar = titleIndex;
        } else if (starIndex != -1) {
          patternIndex = starIndex + 1;
          titleIndex = ++matchAfterStar;
        } else {
          return false;
        }
      }

      while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') patternIndex++;
      return patternIndex == pattern.length();
    }
  }
}
