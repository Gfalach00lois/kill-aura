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

/** Angriffslogik (jeden Tick) und Zielhilfe (jedes Bild). */
public final class KillAura {
	private static int delayTicks = 0;
	private static LivingEntity aimTarget = null;
	private static long lastFrameNanos = 0;

	private KillAura() {}

	/** Darf die Mod gerade etwas tun? Nur in eigenen Einzelspieler-Welten. */
	private static boolean active(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		return KillAuraMod.CONFIG.enabled
				&& player != null && client.world != null && client.interactionManager != null
				&& client.isInSingleplayer()
				&& client.currentScreen == null
				&& !player.isDead() && !player.isSpectator();
	}

	/** Wird 20x pro Sekunde aufgerufen. */
	public static void tick(MinecraftClient client) {
		KillAuraConfig cfg = KillAuraMod.CONFIG;
		if (!active(client)) {
			aimTarget = null;
			return;
		}
		ClientPlayerEntity player = client.player;

		// Zielhilfe: Ziel aussuchen, die Kamera wird in onFrame() weich nachgefuehrt.
		aimTarget = cfg.aimAssist ? findTarget(client, player, cfg, cfg.aimRange) : null;

		// Automatisch schlagen
		if (!cfg.autoAttack) return;
		if (player.isUsingItem()) return; // z.B. beim Essen oder Blocken nicht unterbrechen
		if (delayTicks > 0) {
			delayTicks--;
			return;
		}
		if (cfg.waitCooldown && player.getAttackCooldownProgress(0.5f) < 1.0f) return;

		LivingEntity target = findTarget(client, player, cfg, cfg.range);
		if (target == null) return;

		if (cfg.rotate) lookAt(player, target);

		client.interactionManager.attackEntity(player, target);
		player.swingHand(Hand.MAIN_HAND);
		delayTicks = cfg.extraDelay;
	}

	/** Wird jedes Bild aufgerufen und dreht das Visier weich zum Ziel. */
	public static void onFrame(MinecraftClient client) {
		long now = System.nanoTime();
		float dt = lastFrameNanos == 0 ? 0f : Math.min((now - lastFrameNanos) / 1_000_000_000f, 0.1f);
		lastFrameNanos = now;

		LivingEntity target = aimTarget;
		if (target == null || !target.isAlive() || !active(client)) return;

		// Tempo 1 = sehr sanft, 10 = fast sofort
		float speed = (float) KillAuraMod.CONFIG.aimSpeed * 3.0f;
		float factor = 1.0f - (float) Math.exp(-speed * dt);
		turnTowards(client.player, target, factor);
	}

	private static void turnTowards(ClientPlayerEntity player, LivingEntity target, float factor) {
		double dx = target.getX() - player.getX();
		double dz = target.getZ() - player.getZ();
		double dy = (target.getY() + target.getHeight() * 0.6) - player.getEyeY();
		double horizontal = Math.sqrt(dx * dx + dz * dz);

		float wantYaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
		float wantPitch = (float) -(MathHelper.atan2(dy, horizontal) * (180.0 / Math.PI));

		float yaw = player.getYaw() + MathHelper.wrapDegrees(wantYaw - player.getYaw()) * factor;
		float pitch = MathHelper.clamp(player.getPitch() + (wantPitch - player.getPitch()) * factor, -90.0f, 90.0f);

		player.setYaw(yaw);
		player.setHeadYaw(yaw);
		player.setPitch(pitch);
	}

	private static LivingEntity findTarget(MinecraftClient client, ClientPlayerEntity player, KillAuraConfig cfg, double range) {
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

		if (e instanceof Angerable) return cfg.neutral;
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
		turnTowards(player, target, 1.0f);
	}
}
