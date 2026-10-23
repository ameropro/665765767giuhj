package dev.konosuba.magic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/** Рука Визу (Drain Touch). ПКМ по существу: высасывает здоровье и лечит мага. */
public class DrainTouchItem extends SpellItem {
	private static final float DRAIN = 8.0f;

	public DrainTouchItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity target, Hand hand) {
		if (user.getWorld().isClient) return ActionResult.SUCCESS;
		ServerWorld world = (ServerWorld) user.getWorld();

		target.damage(world.getDamageSources().indirectMagic(user, user), DRAIN);
		user.heal(DRAIN);
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
		user.getHungerManager().add(2, 0.5f);

		// Поток души от цели к магу
		Vec3d from = target.getPos().add(0, target.getHeight() / 2, 0);
		Vec3d to = user.getPos().add(0, user.getHeight() / 2, 0);
		for (int i = 0; i <= 16; i++) {
			Vec3d p = from.lerp(to, i / 16.0);
			world.spawnParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
		}
		world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.8f, 1.2f);

		user.getItemCooldownManager().set(this, 60);
		return ActionResult.CONSUME;
	}
}
