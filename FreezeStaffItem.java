package dev.konosuba.magic;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Посох Заморозки (Визу). Замораживает врагов, превращает воду в лёд, лаву в обсидиан. */
public class FreezeStaffItem extends SpellItem {
	private static final double RANGE = 32.0;
	private static final int RADIUS = 6;

	public FreezeStaffItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack);

		ServerWorld server = (ServerWorld) world;
		Vec3d center = SpellUtil.targetPos(user, RANGE);

		// Враги в радиусе
		Box box = new Box(center.x - RADIUS, center.y - RADIUS, center.z - RADIUS,
			center.x + RADIUS, center.y + RADIUS, center.z + RADIUS);
		for (LivingEntity e : server.getEntitiesByClass(LivingEntity.class, box, en -> en != user && en.squaredDistanceTo(center) <= RADIUS * RADIUS)) {
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 6));
			e.setFrozenTicks(e.getMinFreezeDamageTicks() + 150);
			e.damage(server.getDamageSources().freeze(), 4.0f);
		}

		// Блоки: вода -> лёд, лава -> обсидиан, воздух над твёрдым -> снег
		BlockPos c = BlockPos.ofFloored(center);
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dy = -RADIUS; dy <= RADIUS; dy++) {
				for (int dz = -RADIUS; dz <= RADIUS; dz++) {
					if (dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) continue;
					p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
					var state = server.getBlockState(p);
					if (state.getFluidState().isEqualAndStill(Fluids.WATER)) {
						server.setBlockState(p, Blocks.ICE.getDefaultState());
					} else if (state.getFluidState().isEqualAndStill(Fluids.LAVA)) {
						server.setBlockState(p, Blocks.OBSIDIAN.getDefaultState());
					} else if (state.isAir() && Blocks.SNOW.getDefaultState().canPlaceAt(server, p)
						&& server.random.nextFloat() < 0.5f) {
						server.setBlockState(p, Blocks.SNOW.getDefaultState());
					}
				}
			}
		}

		SpellUtil.ring(server, center, RADIUS, ParticleTypes.SNOWFLAKE, 60);
		SpellUtil.star(server, center, RADIUS, ParticleTypes.SNOWFLAKE, 6, 12);
		server.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1, center.z, 120, RADIUS / 2.0, 1.5, RADIUS / 2.0, 0.05);
		world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2.0f, 0.5f);

		user.getItemCooldownManager().set(this, 80);
		return TypedActionResult.consume(stack);
	}
}
