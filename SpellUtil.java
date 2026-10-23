package dev.konosuba.magic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class SpellUtil {
	private SpellUtil() {}

	/** Точка, куда смотрит игрок (блок или конец дальности). */
	public static Vec3d targetPos(PlayerEntity player, double range) {
		HitResult hit = player.raycast(range, 1.0f, false);
		return hit.getPos();
	}

	/** Живая цель на линии взгляда или null. */
	public static LivingEntity targetEntity(PlayerEntity player, double range) {
		Vec3d start = player.getEyePos();
		Vec3d dir = player.getRotationVec(1.0f);
		Vec3d end = start.add(dir.multiply(range));
		Box box = player.getBoundingBox().stretch(dir.multiply(range)).expand(1.0);
		EntityHitResult result = ProjectileUtil.raycast(
			player, start, end, box,
			e -> !e.isSpectator() && e.isAttackable() && e != player && e instanceof LivingEntity,
			range * range
		);
		return result == null ? null : (LivingEntity) result.getEntity();
	}

	/** Магический круг из частиц, лежащий горизонтально. */
	public static void ring(ServerWorld world, Vec3d center, double radius, ParticleEffect particle, int points) {
		for (int i = 0; i < points; i++) {
			double a = 2 * Math.PI * i / points;
			world.spawnParticles(particle,
				center.x + Math.cos(a) * radius, center.y + 0.1, center.z + Math.sin(a) * radius,
				1, 0, 0, 0, 0);
		}
	}

	/** Многоугольник-звезда внутри круга (для красоты магического круга). */
	public static void star(ServerWorld world, Vec3d center, double radius, ParticleEffect particle, int tips, int stepsPerEdge) {
		for (int i = 0; i < tips; i++) {
			double a1 = 2 * Math.PI * i / tips;
			double a2 = 2 * Math.PI * ((i + 2) % tips) / tips;
			for (int s = 0; s < stepsPerEdge; s++) {
				double t = s / (double) stepsPerEdge;
				double x = Math.cos(a1) * (1 - t) + Math.cos(a2) * t;
				double z = Math.sin(a1) * (1 - t) + Math.sin(a2) * t;
				world.spawnParticles(particle, center.x + x * radius, center.y + 0.1, center.z + z * radius, 1, 0, 0, 0, 0);
			}
		}
	}

	public static void actionBar(PlayerEntity player, Text text) {
		player.sendMessage(text, true);
	}
}
