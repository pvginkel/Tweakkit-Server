package net.minecraft.server;

// CraftBukkit start
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Location;
// CraftBukkit end

public class EntityEnderPearl extends EntityProjectile {

    public EntityEnderPearl(World world) {
        super(world);
    }

    public EntityEnderPearl(World world, EntityLiving entityliving) {
        super(world, entityliving);
    }

    public EntityEnderPearl(World world, double d0, double d1, double d2) {
        super(world, d0, d1, d2);
    }

    protected void a(MovingObjectPosition movingobjectposition) {
        if (movingobjectposition.entity != null && movingobjectposition.entity.damageEntity(DamageSource.projectile(this, this.shooter), 0)) {
            ;
        }

        for (int i = 0; i < 32; ++i) {
            this.world.a("portal", this.locX, this.locY + this.random.nextDouble() * 2.0D, this.locZ, this.random.nextGaussian(), 0.0D, this.random.nextGaussian());
        }

        if (!this.world.isStatic) {
            // CraftBukkit start - dupe fix + damage event
            boolean teleport = false;
            PlayerTeleportEvent teleEvent = null;

            if (this.shooter != null) {
                if (this.shooter instanceof EntityPlayer) {
                    CraftPlayer player = (CraftPlayer) this.shooter.bukkitEntity;
                    teleport = player.isOnline() && player.getWorld() == getBukkitEntity().getWorld();
                    Location location = getBukkitEntity().getLocation();
                    location.setPitch(player.getLocation().getPitch());
                    location.setYaw(player.getLocation().getYaw());

                    if (teleport) {
                        teleEvent = new PlayerTeleportEvent(player, player.getLocation(), location, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
                        Bukkit.getPluginManager().callEvent(teleEvent);
                        teleport = !teleEvent.isCancelled();
                    }
                } else {
                    teleport = true;
                }
            }

            if (teleport) {
                if (this.shooter instanceof EntityPlayer) {
                    ((EntityPlayer) this.shooter).netServerHandler.teleport(teleEvent.getTo());
                } else {
                    this.shooter.enderTeleportTo(this.locX, this.locY, this.locZ);
                }
                this.shooter.fallDistance = 0.0F;
                EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(this.getBukkitEntity(), this.shooter.getBukkitEntity(), EntityDamageByEntityEvent.DamageCause.FALL, 5);
                Bukkit.getPluginManager().callEvent(damageEvent);

                if (!damageEvent.isCancelled()) {
                    org.bukkit.entity.Player bPlayer = Bukkit.getPlayerExact(((EntityPlayer) this.shooter).name);
                    ((CraftPlayer) bPlayer).getHandle().invulnerableTicks = -1; // Remove spawning invulnerability.
                    bPlayer.setLastDamageCause(damageEvent);
                    ((CraftPlayer) bPlayer).getHandle().damageEntity(DamageSource.FALL, damageEvent.getDamage()); // Damage the new player instead of the old
                }
            }
            // CraftBukkit end

            this.die();
        }
    }
}
