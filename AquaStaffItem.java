package dev.konosuba.magic;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Посох Богини (Аква).
 * ПКМ: «Turn Undead + Heal» — нежить рядом получает священный урон, союзники лечатся.
 * Shift + ПКМ: «Create Water» — ставит источник воды.
 */
public class AquaStaffItem extends SpellItem {
	private static final double RADIUS = 8.0;

	public AquaStaffItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack);
		ServerWorld server = (ServerWorld) world;

		if (user.isSneaking()) {
			createWater(server, user);
			user.getItemCooldownManager().set(this, 20);
		} else {
			turnUndeadAndHeal(server, user);
			user.getItemCooldownManager().set(this, 120);
		}
		return TypedActionResult.consume(stack);
	}

	private void createWater(ServerWorld world, PlayerEntity user) {
		HitResult hit = user.raycast(10.0, 1.0f, false);
		if (!(hit instanceof BlockHitResult bhr)) return;
		if (world.getDimension().ultrawarm()) {
			world.spawnParticles(ParticleTypes.CLOUD, bhr.getPos().x, bhr.getPos().y + 0.5, bhr.getPos().z, 10, 0.3, 0.3, 0.3, 0.02);
			return;
		}
		BlockPos pos = bhr.getBlockPos().offset(bhr.getSide());
		if (world.getBlockState(pos).isReplaceable()) {
			world.setBlockState(pos, Blocks.WATER.getDefaultState());
			world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.2f);
			world.spawnParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 25, 0.3, 0.2, 0.3, 0.1);
		}
	}

	private void turnUndeadAndHeal(ServerWorld world, PlayerEntity user) {
		Vec3d c = user.getPos();
		Box box = user.getBoundingBox().expand(RADIUS);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, en -> en.squaredDistanceTo(c) <= RADIUS * RADIUS)) {
			if (e == user) continue;
			if (e.isUndead()) {
				e.damage(world.getDamageSources().indirectMagic(user, user), 30.0f);
				e.setOnFireFor(6);
				world.spawnParticles(ParticleTypes.END_ROD, e.getX(), e.getBodyY(0.5), e.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
			} else if (e instanceof PlayerEntity || (e instanceof TameableEntity t && t.isTamed())) {
				e.heal(8.0f);
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
				world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getBodyY(0.5), e.getZ(), 12, 0.3, 0.5, 0.3, 0.0);
			}
		}
		// Сама Аква тоже лечится
		user.heal(8.0f);
		user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));

		SpellUtil.ring(world, c, RADIUS, ParticleTypes.END_ROD, 80);
		SpellUtil.ring(world, c, RADIUS * 0.7, ParticleTypes.GLOW, 50);
		SpellUtil.star(world, c, RADIUS * 0.9, ParticleTypes.END_ROD, 6, 14);
		world.spawnParticles(ParticleTypes.GLOW, c.x, c.y + 1.0, c.z, 60, 2.0, 1.5, 2.0, 0.05);
		world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 2.0f, 1.2f);
	}
}
