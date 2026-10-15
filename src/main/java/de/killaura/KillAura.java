package de.killaura;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

/** Die eigentliche Angriffslogik, wird jeden Tick aufgerufen. */
public final class KillAura {
	private static int delayTicks = 0;

	private KillAura() {}

	public static void tick(MinecraftClient client) {
		KillAuraConfig cfg = KillAuraMod.CONFIG;
		ClientPlayerEntity player = client.player;

		if (!cfg.enabled || player == null || client.world == null || client.interactionManager == null) return;
		// Nur in eigenen Einzelspieler-Welten - auf Servern ist so etwas Cheating und fuehrt zu Bans.
		if (!client.isInSingleplayer()) return;
		if (client.currentScreen != null || player.isDead() || player.isSpectator()) return;
		if (player.isUsingItem()) return; // z.B. beim Essen oder Blocken nicht unterbrechen

		if (delayTicks > 0) {
			delayTicks--;
			return;
		}
		if (cfg.waitCooldown && player.getAttackCooldownProgress(0.5f) < 1.0f) return;

		LivingEntity target = findTarget(client, player, cfg);
		if (target == null) return;

		if (cfg.rotate) lookAt(player, target);

		client.interactionManager.attackEntity(player, target);
		player.swingHand(Hand.MAIN_HAND);
		delayTicks = cfg.extraDelay;
	}

	private static LivingEntity findTarget(MinecraftClient client, ClientPlayerEntity player, KillAuraConfig cfg) {
		double range = cfg.range;
		Vec3d eyes = player.getEyePos();
		Box area = player.getBoundingBox().expand(range + 1.0);

		List<LivingEntity> candidates = client.world.getEntitiesByClass(LivingEntity.class, area,
				e -> isValidTarget(player, e, cfg) && distanceToBox(eyes, e.getBoundingBox()) <= range);

		Comparator<LivingEntity> order = cfg.lowestHealthFirst
				? Comparator.comparingDouble(LivingEntity::getHealth)
				: Comparator.comparingDouble(e -> distanceToBox(eyes, e.getBoundingBox()));

		return candidates.stream().min(order).orElse(null);
	}

	private static boolean isValidTarget(ClientPlayerEntity player, LivingEntity e, KillAuraConfig cfg) {
		if (e == player || !e.isAlive() || e.isInvulnerable()) return false;
		if (cfg.lineOfSight && !player.canSee(e)) return false;

		if (e instanceof PlayerEntity p) return cfg.players && !p.getAbilities().creativeMode && !p.isSpectator();
		if (e instanceof MerchantEntity) return cfg.villagers;
		if (e instanceof TameableEntity t && t.isTamed()) return cfg.pets;

		boolean isNeutral = e instanceof Angerable;
		if (isNeutral) return cfg.neutral;
		if (e instanceof Monster) return cfg.hostile;
		if (e instanceof AnimalEntity) return cfg.animals;
		return false; // Ruestungsstaender, Fische, Fledermaeuse usw. ignorieren
	}

	private static double distanceToBox(Vec3d p, Box b) {
		double x = MathHelper.clamp(p.x, b.minX, b.maxX);
		double y = MathHelper.clamp(p.y, b.minY, b.maxY);
		double z = MathHelper.clamp(p.z, b.minZ, b.maxZ);
		return p.distanceTo(new Vec3d(x, y, z));
	}

	private static void lookAt(ClientPlayerEntity player, LivingEntity target) {
		double dx = target.getX() - player.getX();
		double dz = target.getZ() - player.getZ();
		double dy = (target.getY() + target.getHeight() * 0.5) - player.getEyeY();
		double horizontal = Math.sqrt(dx * dx + dz * dz);

		float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
		float pitch = (float) -(MathHelper.atan2(dy, horizontal) * (180.0 / Math.PI));

		player.setYaw(yaw);
		player.setHeadYaw(yaw);
		player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
	}
}
