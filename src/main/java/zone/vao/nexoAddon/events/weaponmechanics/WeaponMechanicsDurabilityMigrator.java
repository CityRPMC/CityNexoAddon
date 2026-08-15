package zone.vao.nexoAddon.events.weaponmechanics;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import zone.vao.nexoAddon.NexoAddon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/** Lazily adds configured WeaponMechanics durability to legacy weapon items. */
public final class WeaponMechanicsDurabilityMigrator implements Listener {

  private static final NamespacedKey WEAPON_TITLE_KEY = new NamespacedKey("weaponmechanics", "weapon-title");

  private final NexoAddon plugin;
  private Map<String, Integer> maxDamageByWeaponTitle = Map.of();

  public WeaponMechanicsDurabilityMigrator(NexoAddon plugin) {
    this.plugin = plugin;
    reloadWeaponDurability();
  }

  public int reloadWeaponDurability() {
    Plugin weaponMechanics = Bukkit.getPluginManager().getPlugin("WeaponMechanics");
    if (weaponMechanics == null) {
      maxDamageByWeaponTitle = Map.of();
      return 0;
    }

    File weaponsDirectory = new File(weaponMechanics.getDataFolder(), "weapons");
    if (!weaponsDirectory.isDirectory()) {
      maxDamageByWeaponTitle = Map.of();
      return 0;
    }

    Map<String, Integer> loaded = new HashMap<>();
    try (Stream<java.nio.file.Path> paths = Files.walk(weaponsDirectory.toPath())) {
      paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".yml"))
          .forEach(path -> loadWeaponFile(path.toFile(), loaded));
    } catch (IOException exception) {
      plugin.getLogger().warning("Unable to read WeaponMechanics weapon configs: " + exception.getMessage());
    }

    maxDamageByWeaponTitle = Map.copyOf(loaded);
    if (!loaded.isEmpty()) {
      plugin.getLogger().info("Loaded durability values for " + loaded.size() + " WeaponMechanics weapons.");
    }
    return loaded.size();
  }

  private void loadWeaponFile(File file, Map<String, Integer> loaded) {
    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
    for (String weaponTitle : config.getKeys(false)) {
      int maxDamage = config.getInt(weaponTitle + ".Info.Weapon_Item.Durability.Max_Damage", 0);
      if (maxDamage > 0) loaded.put(weaponTitle, maxDamage);
    }
  }

  public int migrateOnlinePlayers() {
    int migrated = 0;
    for (Player player : Bukkit.getOnlinePlayers()) migrated += migratePlayer(player);
    return migrated;
  }

  public int migratePlayer(Player player) {
    return migrateInventory(player.getInventory()) + migrateInventory(player.getEnderChest());
  }

  public int migrateInventory(Inventory inventory) {
    int migrated = 0;
    for (int slot = 0; slot < inventory.getSize(); slot++) {
      ItemStack item = inventory.getItem(slot);
      if (migrateItem(item)) {
        inventory.setItem(slot, item);
        migrated++;
      }
    }
    return migrated;
  }

  public boolean migrateItem(ItemStack item) {
    if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;

    ItemMeta meta = item.getItemMeta();
    String weaponTitle = meta.getPersistentDataContainer().get(WEAPON_TITLE_KEY, PersistentDataType.STRING);
    if (weaponTitle == null) return false;

    Integer configuredMaxDamage = maxDamageByWeaponTitle.get(weaponTitle);
    if (configuredMaxDamage == null || !(meta instanceof Damageable damageable)) return false;

    boolean changed = false;
    if (!damageable.hasMaxDamage()) {
      damageable.setMaxDamage(configuredMaxDamage);
      damageable.setDamage(0);
      changed = true;
    }
    if (meta.isUnbreakable()) {
      meta.setUnbreakable(false);
      changed = true;
    }

    if (changed) item.setItemMeta(meta);
    return changed;
  }

  private boolean isAutoMigrationEnabled() {
    return plugin.getGlobalConfig().getBoolean("weaponmechanics.auto_migrate_durability", true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (isAutoMigrationEnabled()) migratePlayer(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryOpen(InventoryOpenEvent event) {
    if (!isAutoMigrationEnabled()) return;
    migrateInventory(event.getInventory());
    if (event.getPlayer() instanceof Player player) migratePlayer(player);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!isAutoMigrationEnabled()) return;
    if (migrateItem(event.getCurrentItem())) event.setCurrentItem(event.getCurrentItem());
    if (migrateItem(event.getCursor())) event.setCursor(event.getCursor());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPickup(EntityPickupItemEvent event) {
    if (isAutoMigrationEnabled()) migrateItem(event.getItem().getItemStack());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onItemSpawn(ItemSpawnEvent event) {
    if (isAutoMigrationEnabled()) migrateItem(event.getEntity().getItemStack());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!isAutoMigrationEnabled() || event.getHand() == null) return;

    PlayerInventory inventory = event.getPlayer().getInventory();
    ItemStack item = event.getHand() == EquipmentSlot.HAND
        ? inventory.getItemInMainHand()
        : inventory.getItemInOffHand();
    if (!migrateItem(item)) return;

    if (event.getHand() == EquipmentSlot.HAND) inventory.setItemInMainHand(item);
    else inventory.setItemInOffHand(item);
  }
}
