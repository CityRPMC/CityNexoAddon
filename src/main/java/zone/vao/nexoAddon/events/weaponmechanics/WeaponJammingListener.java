package zone.vao.nexoAddon.events.weaponmechanics;

import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponPreReloadEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponPreShootEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import zone.vao.nexoAddon.NexoAddon;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles firearm malfunctions and jamming when weapon condition deteriorates below 20%.
 * Jammed firearms are cleared intuitively when the player presses Reload.
 */
public final class WeaponJammingListener implements Listener {

    private final NexoAddon plugin;
    private final Map<UUID, Long> jammedPlayers = new ConcurrentHashMap<>();

    private static final Component JAM_MESSAGE = Component.text("⚠ ", NamedTextColor.RED, TextDecoration.BOLD)
            .append(Component.text("Malfunction! Chamber jammed — Press Reload to clear.", NamedTextColor.GOLD));

    private static final Component CLEARED_MESSAGE = Component.text("✔ ", NamedTextColor.GREEN, TextDecoration.BOLD)
            .append(Component.text("Chamber cleared!", NamedTextColor.WHITE));

    public WeaponJammingListener(NexoAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeaponPreShoot(WeaponPreShootEvent event) {
        if (!(event.getShooter() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // If weapon is actively jammed, prevent shooting until cleared
        if (jammedPlayers.containsKey(uuid)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.6f);
            player.sendActionBar(JAM_MESSAGE);
            return;
        }

        ItemStack weapon = event.getWeaponStack();
        if (weapon == null || !weapon.hasItemMeta()) {
            return;
        }

        ItemMeta meta = weapon.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }

        int maxDamage = damageable.hasMaxDamage() ? damageable.getMaxDamage() : weapon.getType().getMaxDurability();
        if (maxDamage <= 0) {
            return;
        }

        int damage = damageable.getDamage();
        double conditionRatio = 1.0 - ((double) damage / maxDamage);

        // Jamming threshold: under 20% durability remaining
        if (conditionRatio > 0.20) {
            return;
        }

        // Jam chance scales smoothly from 8% at 20% durability up to 35% at 0% durability
        double wearFactor = (0.20 - Math.max(0.0, conditionRatio)) / 0.20; // 0.0 at 20%, 1.0 at 0%
        double jamChance = 0.08 + (wearFactor * 0.27);

        if (ThreadLocalRandom.current().nextDouble() >= jamChance) {
            return;
        }

        // Malfunction occurred: Cancel the shot & mark as jammed
        event.setCancelled(true);
        jammedPlayers.put(uuid, System.currentTimeMillis());

        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_DISPENSER_FAIL, 0.9f, 1.4f);
        player.sendActionBar(JAM_MESSAGE);

        // Small mechanical smoke puff at firearm position
        Location eyeLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.6));
        player.getWorld().spawnParticle(Particle.SMOKE, eyeLoc, 2, 0.05, 0.05, 0.05, 0.01);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWeaponPreReload(WeaponPreReloadEvent event) {
        if (!(event.getShooter() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (jammedPlayers.remove(uuid) != null) {
            // Player pressed reload to rack the slide / cycle the chamber: clear jam!
            Location loc = player.getLocation();
            loc.getWorld().playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_OPEN, 0.8f, 1.8f);
            loc.getWorld().playSound(loc, Sound.ITEM_ARMOR_EQUIP_IRON, 0.8f, 1.6f);
            player.sendActionBar(CLEARED_MESSAGE);

            // Small eject particle
            Location eyeLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.4));
            player.getWorld().spawnParticle(Particle.CRIT, eyeLoc, 3, 0.1, 0.1, 0.1, 0.05);
        }
    }
}
