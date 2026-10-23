package dev.konosuba.magic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Посох Ветра (Wind Breath).
 * ПКМ: мощный порыв конусом, отбрасывает всех впереди.
 * Shift + ПКМ: ветер подбрасывает самого мага вверх.
 */
public class WindStaffItem extends SpellItem {
	private static final double RANGE = 12.0;

	public WindStaffItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack);
		ServerWorld server = (ServerWorld) world;

		if (user.isSneaking()) {
			user.setVelocity(user.getVelocity().add(0, 1.2, 0));
			user.velocityModified = true;
			user.fallDistance = 0;
			server.spawnParticles(ParticleTypes.CLOUD, user.getX(), user.getY(), user.getZ(), 30, 0.4, 0.1, 0.4, 0.15);
			world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 1.5f, 0.7f);
			user.getItemCooldownManager().set(this, 30);
			return TypedActionResult.consume(stack);
		}

		Vec3d eye = user.getEyePos();
		Vec3d dir = user.getRotationVec(1.0f).normalize();
		Box box = user.getBoundingBox().stretch(dir.multiply(RANGE)).expand(3.0);

		for (LivingEntity e : server.getEntitiesByClass(LivingEntity.class, box, en -> en != user)) {
			Vec3d to = e.getPos().add(0, e.getHeight() / 2, 0).subtract(eye);
			double dist = to.length();
			if (dist > RANGE || dist < 0.001) continue;
			if (to.normalize().dotProduct(dir) < 0.7) continue; // конус ~45 градусов
			double power = 2.2 * (1.0 - dist / RANGE) + 0.8;
			e.setVelocity(e.getVelocity().add(dir.x * power, 0.4 + 0.2 * power, dir.z * power));
			e.velocityModified = true;
		}

		// Визуальный конус
		for (int i = 0; i < 40; i++) {
			double d = server.random.nextDouble() * RANGE;
			double spread = d * 0.35;
			Vec3d p = eye.add(dir.multiply(d)).add(
				(server.random.nextDouble() - 0.5) * spread,
				(server.random.nextDouble() - 0.5) * spread,
				(server.random.nextDouble() - 0.5) * spread);
			server.spawnParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0, 0, 0, 0.05);
		}
		world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 2.0f, 0.5f);

		user.getItemCooldownManager().set(this, 40);
		return TypedActionResult.consume(stack);
	}
}
