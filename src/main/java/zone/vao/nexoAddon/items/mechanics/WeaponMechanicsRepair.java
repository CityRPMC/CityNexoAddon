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

public record WeaponMechanicsRepair(double ratio, int fixedAmount, List<String> whitelist, List<String> blacklist) {

  private static final NamespacedKey WEAPON_TITLE_KEY = new NamespacedKey("weaponmechanics", "weapon-title");

  public static class WeaponMechanicsRepairListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player)
          || !event.isLeftClick()
          || event.getCursor() == null
          || event.getCurrentItem() == null) return;

      ItemStack repairItem = event.getCursor().clone();
      ItemStack weapon = event.getCurrentItem().clone();
      String repairItemId = NexoItems.idFromItem(repairItem);
      if (repairItemId == null) return;

      Mechanics mechanics = NexoAddon.getInstance().getMechanics().get(repairItemId);
      if (mechanics == null || mechanics.getWeaponMechanicsRepair() == null) return;

      WeaponMechanicsRepair repair = mechanics.getWeaponMechanicsRepair();
      String weaponTitle = getWeaponTitle(weapon);
      if (weaponTitle == null || !isAllowed(weaponTitle, repair)) return;
      if (!(weapon.getItemMeta() instanceof Damageable weaponMeta) || !weaponMeta.hasDamage()) return;

      event.setCancelled(true);
      int repairAmount = repair.ratio() > 0
          ? (int) Math.ceil(weaponMeta.getDamage() * repair.ratio())
          : repair.fixedAmount();
      if (repairAmount <= 0) return;

      weaponMeta.setDamage(Math.max(0, weaponMeta.getDamage() - repairAmount));
      weapon.setItemMeta(weaponMeta);
      consumeRepairItem(repairItem, repairItemId);

      event.getClickedInventory().setItem(event.getSlot(), weapon);
      player.setItemOnCursor(repairItem);
      player.updateInventory();
    }

    private static void consumeRepairItem(ItemStack repairItem, String repairItemId) {
      Integer configuredMaxDamage = NexoItems.itemFromId(repairItemId).getMaxDamage();
      int maxDamage = configuredMaxDamage != null
          ? configuredMaxDamage
          : repairItem.getType().getMaxDurability();

      if (maxDamage <= 0 || !(repairItem.getItemMeta() instanceof Damageable repairMeta)) {
        repairItem.setAmount(repairItem.getAmount() - 1);
        return;
      }

      int newDamage = repairMeta.getDamage() + 1;
      if (newDamage < maxDamage) {
        repairMeta.setDamage(newDamage);
        repairItem.setItemMeta(repairMeta);
        return;
      }

      repairItem.setAmount(repairItem.getAmount() - 1);
      if (repairItem.getAmount() <= 0) repairItem.setType(Material.AIR);
      else {
        repairMeta.setDamage(0);
        repairItem.setItemMeta(repairMeta);
      }
    }

    private static String getWeaponTitle(ItemStack item) {
      return item.getItemMeta().getPersistentDataContainer()
          .get(WEAPON_TITLE_KEY, PersistentDataType.STRING);
    }

    private static boolean isAllowed(String weaponTitle, WeaponMechanicsRepair repair) {
      if (matchesAny(weaponTitle, repair.blacklist())) return false;
      return repair.whitelist().isEmpty() || matchesAny(weaponTitle, repair.whitelist());
    }

    private static boolean matchesAny(String weaponTitle, List<String> patterns) {
      for (String pattern : patterns) if (matches(weaponTitle, pattern)) return true;
      return false;
    }

    /** Supports exact titles plus '*' and '?' wildcards, case-insensitively. */
    private static boolean matches(String weaponTitle, String pattern) {
      int titleIndex = 0;
      int patternIndex = 0;
      int starIndex = -1;
      int matchAfterStar = -1;

      while (titleIndex < weaponTitle.length()) {
        if (patternIndex < pattern.length()
            && (pattern.charAt(patternIndex) == '?'
                || Character.toLowerCase(pattern.charAt(patternIndex))
                    == Character.toLowerCase(weaponTitle.charAt(titleIndex)))) {
          patternIndex++;
          titleIndex++;
        } else if (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
          starIndex = patternIndex++;
          matchAfterStar = titleIndex;
        } else if (starIndex != -1) {
          patternIndex = starIndex + 1;
          titleIndex = ++matchAfterStar;
        } else return false;
      }

      while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') patternIndex++;
      return patternIndex == pattern.length();
    }
  }
}
