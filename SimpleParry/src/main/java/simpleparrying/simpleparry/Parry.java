package simpleparrying.simpleparry;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Parry implements Listener {
    private static final long PENALTY_RESET_TIME = 1000000000; // 0.5 seconds in nanoseconds
    private static final double PENALTY_MULTIPLIER = 0.25; // 75% reduction
    private static final double BLOCKING_DAMAGE_REDUCTION = 0.9; // 10% reduction
    Material[] MainSwords;
    Material[] OffSwords;
    private SimpleParry plugin; // Added reference to SimpleParry

    public Parry(SimpleParry plugin) {
        this.plugin = plugin; // Initialize the reference
        this.MainSwords = new Material[]{Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.GOLDEN_SWORD, Material.IRON_SWORD, Material.STONE_SWORD, Material.WOODEN_SWORD};
        this.OffSwords = new Material[]{Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.GOLDEN_SWORD, Material.IRON_SWORD, Material.STONE_SWORD, Material.WOODEN_SWORD, Material.SHIELD, Material.BOW};
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void OnInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        PlayerInventory inventory = player.getInventory();
        ItemStack MainHand = inventory.getItemInMainHand();
        ItemStack OffHand = inventory.getItemInOffHand(); // Get the item in the offhand
        Material MainHandType = MainHand.getType();
        Material OffHandType = OffHand.getType(); // Get the type of the offhand item
        boolean isMainSword = Arrays.asList(this.MainSwords).contains(MainHandType);
        boolean isOffhand = Arrays.asList(this.OffSwords).contains(OffHandType); // Check if the offhand item is a sword or axe
        ItemMeta meta = MainHand.getItemMeta();
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && isMainSword && !isOffhand && !player.hasCooldown(Material.NETHERITE_SWORD)) {
            if (!player.hasCooldown(MainHandType)) { // Check if the player has a cooldown
                player.setMetadata("parryStart", new FixedMetadataValue(plugin, System.nanoTime()));
                player.setMetadata("parryAttempt", new FixedMetadataValue(plugin, true));
                world.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 10.0F, 1.0F);
                if (meta != null) { // Check if meta is not null
                    meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_UNBREAKABLE}); // This is the disword effect
                    MainHand.setItemMeta(meta);
                }
                player.setCooldown(MainHandType, 24); // Set a cooldown of 1.2 seconds
                long parryWindow = player.hasMetadata("fullParry") ? 500000000 : 625000000; // Parry window in nanoseconds
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(SimpleParry.plugin, () -> {
                    long parryEnd = player.getMetadata("parryStart").get(0).asLong() + parryWindow;
                    if (System.nanoTime() >= parryEnd && meta.hasItemFlag(ItemFlag.HIDE_UNBREAKABLE)) {
                        meta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_UNBREAKABLE});
                        MainHand.setItemMeta(meta);
                    }
                }, parryWindow / 50000000); // Convert nanoseconds to ticks (1 tick = 0.05 seconds)
            } else if (meta.hasItemFlag(ItemFlag.HIDE_UNBREAKABLE)) { // If the sword is currently disappeared (disword)
                meta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_UNBREAKABLE}); // Make the sword reappear
                MainHand.setItemMeta(meta);
                player.setCooldown(MainHandType, 24); // Set a cooldown of 1.2 seconds
            }
        }
    }

    @EventHandler
    public void OnEntityDamaged(EntityDamageByEntityEvent event) {
        World world = event.getEntity().getWorld();
        if (event.getEntity().getType() == EntityType.PLAYER) {
            Player player1 = (Player)event.getEntity();
            Material MainHandType = player1.getInventory().getItemInMainHand().getType();
            boolean isMainSword = Arrays.asList(this.MainSwords).contains(MainHandType);
            long parryStart = player1.getMetadata("parryStart").get(0).asLong();
            long parryWindow = player1.hasMetadata("fullParry") ? 500000000 : 625000000; // Parry window in nanoseconds
            if (isMainSword && System.nanoTime() - parryStart < parryWindow && !player1.isBlocking()) {
                world.playSound(player1.getLocation(), Sound.BLOCK_ANVIL_PLACE, 10.0F, 1.0F);
                event.setCancelled(true);
                if (event.getDamager() instanceof LivingEntity) {
                    LivingEntity damager = (LivingEntity) event.getDamager();
                    damager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 5)); // Slowness V for 2 seconds
                    damager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40, 10)); // Mining Fatigue 10 for 2 seconds
                    world.spawnParticle(Particle.SWEEP_ATTACK, damager.getLocation().add(0.0D, 1.0D, 0.0D), 10); // Add particle effect
                }
                player1.removeMetadata("fullParry", plugin);
                if (player1.getInventory().getItemInMainHand().getItemMeta().hasItemFlag(ItemFlag.HIDE_UNBREAKABLE)) {
                    ItemMeta meta = player1.getInventory().getItemInMainHand().getItemMeta();
                    meta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_UNBREAKABLE}); // Make the sword reappear after a successful parry
                    player1.getInventory().getItemInMainHand().setItemMeta(meta);
                }
            } else if (isMainSword && player1.isBlocking()) {
                event.setDamage(event.getDamage() * BLOCKING_DAMAGE_REDUCTION);
                long newParryWindow = player1.hasMetadata("parryWindow") ? (long) (player1.getMetadata("parryWindow").get(0).asLong() * PENALTY_MULTIPLIER) : (long) (parryWindow * PENALTY_MULTIPLIER);
                player1.setMetadata("parryWindow", new FixedMetadataValue(plugin, newParryWindow));
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player1.hasMetadata("parryWindow")) {
                    player1.removeMetadata("parryWindow", plugin);
                }
            }, PENALTY_RESET_TIME / 50000000); // Convert nanoseconds to ticks (1 tick = 0.05 seconds)
        }
    }
}