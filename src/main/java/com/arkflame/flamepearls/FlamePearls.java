package com.arkflame.flamepearls;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import com.arkflame.flamepearls.listeners.CreatureSpawnListener;
import com.arkflame.flamepearls.listeners.ProjectileHitListener;

public class FlamePearls extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Save default config
        this.saveDefaultConfig();

        // Set static instance
        FlamePearls.instance = this;

        // Register the event listener
        getServer().getPluginManager().registerEvents(this, this);
        // Register ProjectileHitListener
        getServer().getPluginManager().registerEvents(new ProjectileHitListener(), this);
        // Register CreatureSpawnListener
        getServer().getPluginManager().registerEvents(new CreatureSpawnListener(), this);
    }

    private static FlamePearls instance;

    public static FlamePearls getInstance() {
        return FlamePearls.instance;
    }

    // Origin of launch of projectiles
    private Map<Projectile, Location> projectileOrigins = new ConcurrentHashMap<>();

    // Last time pearl was thrown by a player (Cooldown checks)
    private Map<Player, Long> lastPearlThrows = new ConcurrentHashMap<>();

    private void setOrigin(Projectile projectile, Location location) {
        // Insert the projectile-origin
        projectileOrigins.put(projectile, location);
    }

    public Location getOriginAndRemove(Projectile projectile) {
        // Return the value removed
        return projectileOrigins.remove(projectile);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if the action is right click
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Get the player who interacted
            Player player = event.getPlayer();

            // Get the held item
            ItemStack heldItem = player.getInventory().getItem(player.getInventory().getHeldItemSlot());

            // Check if the player is holding an ender pearl in their main hand
            if (heldItem != null && heldItem.getType() == Material.ENDER_PEARL) {
                // Get the current time
                long currentTime = System.currentTimeMillis();

                // Get time since last pearl
                long timeSinceLastPearl = currentTime - lastPearlThrows.getOrDefault(player, 0L);

                // Check if enough time passed
                if (timeSinceLastPearl < 500L) {
                    // Create a decimal format object with 0.0 pattern
                    DecimalFormat df = new DecimalFormat("0.0");
                    // Apply the format to the time
                    String cooldownSeconds = df.format(0.5 - timeSinceLastPearl / 1000D);
                    // Cancel the interaction event
                    event.setCancelled(true);
                    // Send a message to the player
                    player.sendMessage("You cannot throw ender pearls! Wait " + cooldownSeconds + "s");
                } else {
                    // Set the current time as last pearl thrown
                    lastPearlThrows.put(player, currentTime);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Check if the teleport cause is ENDER_PEARL
        if (event.getCause() == TeleportCause.ENDER_PEARL) {
            // Cancel the event
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if the entity is a player and the damager is an ender pearl
        if (event.getDamager() instanceof EnderPearl) {
            // Get the player and the ender pearl
            EnderPearl pearl = (EnderPearl) event.getDamager();
            // Check if the pearl was thrown by a different entity than the shooter
            if (pearl.getShooter() != event.getEntity()) {
                // Set the damage
                event.setDamage(0);
            } else {
                // Set the damage
                event.setDamage(0);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // Get the projectile
        Projectile projectile = event.getEntity();

        // Check if the projectile is an ender pearl
        if (projectile instanceof EnderPearl) {
            // Get the shooter
            ProjectileSource shooter = projectile.getShooter();

            // Check if shooter is entity
            if (shooter instanceof Player) {
                // Set the origin to the shooter location
                setOrigin(projectile, ((Player) shooter).getLocation());
            }
        }
    }
}