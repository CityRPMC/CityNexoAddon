package zone.vao.nexoAddon.items.mechanics;

import com.nexomc.nexo.api.NexoItems;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import zone.vao.nexoAddon.NexoAddon;
import zone.vao.nexoAddon.items.Mechanics;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record Lifesteal(int amount, double cooldown) {
    public static class LifestealListener implements Listener {
        private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void on(EntityDamageByEntityEvent event) {
            LivingEntity attacker = resolveAttacker(event);
            if (attacker == null || !(event.getEntity() instanceof LivingEntity victim) || attacker.equals(victim)) return;
            if (attacker instanceof Player player && victim instanceof Player && !isPvpAllowed(player, victim)) return;

            EntityEquipment equipment = attacker.getEquipment();
            if (equipment == null) return;
            ItemStack weapon = equipment.getItemInMainHand();
            if (weapon.isEmpty()) return;

            String nexoItemId = NexoItems.idFromItem(weapon);
            if (nexoItemId == null) return;

            Mechanics mechanics = NexoAddon.getInstance().getMechanics().get(nexoItemId);
            if (mechanics == null) return;

            Lifesteal mechanic = mechanics.getLifesteal();
            if (mechanic == null || mechanic.amount() <= 0 || event.getFinalDamage() <= 0) return;

            if (mechanic.cooldown() > 0) {
                long now = System.currentTimeMillis();
                if (cooldowns.getOrDefault(attacker.getUniqueId(), 0L) > now) return;
            }

            AttributeInstance maxHealthAttr = attacker.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr == null) return;
            double maxHealth = maxHealthAttr.getValue();
            double healAmount = Math.min(mechanic.amount(), event.getFinalDamage());

            attacker.setHealth(Math.min(attacker.getHealth() + healAmount, maxHealth));

            if (mechanic.cooldown() > 0) {
                cooldowns.put(attacker.getUniqueId(), System.currentTimeMillis() + (long) (mechanic.cooldown() * 1000L));
            }
        }

        private LivingEntity resolveAttacker(EntityDamageByEntityEvent event) {
            if (event.getDamager() instanceof LivingEntity livingEntity) return livingEntity;
            if (!(event.getDamager() instanceof Projectile projectile)) return null;

            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof LivingEntity livingEntity ? livingEntity : null;
        }

        private boolean isPvpAllowed(Player attacker, LivingEntity victim) {
            if (!NexoAddon.getInstance().getServer().getPluginManager().isPluginEnabled("WorldGuard")) return true;

            WorldGuardPlugin worldGuardPlugin = WorldGuardPlugin.inst();
            LocalPlayer localAttacker = worldGuardPlugin.wrapPlayer(attacker);
            if (WorldGuard.getInstance().getPlatform().getSessionManager()
                    .hasBypass(localAttacker, BukkitAdapter.adapt(victim.getWorld()))) return true;

            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            StateFlag.State attackerState = query.queryState(
                    BukkitAdapter.adapt(attacker.getLocation()), localAttacker, Flags.PVP);
            StateFlag.State victimState = query.queryState(
                    BukkitAdapter.adapt(victim.getLocation()), localAttacker, Flags.PVP);

            return attackerState != StateFlag.State.DENY && victimState != StateFlag.State.DENY;
        }
    }
}
