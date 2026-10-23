package dev.konosuba.magic;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

/** Посох Молнии (Юнъюн). Бьёт молнией в цель, и ещё до 3 врагов рядом получают цепную молнию. */
public class LightningStaffItem extends SpellItem {
	private static final double RANGE = 48.0;

	public LightningStaffItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack);

		ServerWorld server = (ServerWorld) world;
		LivingEntity target = SpellUtil.targetEntity(user, RANGE);
		Vec3d pos = target != null ? target.getPos() : SpellUtil.targetPos(user, RANGE);

		strike(server, pos, user);

		// Цепная молния по врагам рядом
		Box area = new Box(pos.x - 7, pos.y - 4, pos.z - 7, pos.x + 7, pos.y + 4, pos.z + 7);
		List<LivingEntity> victims = server.getEntitiesByClass(LivingEntity.class, area,
			e -> e != user && e != target && e instanceof Monster);
		victims.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(pos)));
		for (int i = 0; i < Math.min(3, victims.size()); i++) {
			strike(server, victims.get(i).getPos(), user);
		}

		// Дуга из частиц от посоха к цели
		Vec3d from = user.getEyePos();
		for (int i = 0; i <= 20; i++) {
			Vec3d p = from.lerp(pos, i / 20.0);
			server.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
		}

		user.getItemCooldownManager().set(this, 100);
		return TypedActionResult.consume(stack);
	}

	private static void strike(ServerWorld world, Vec3d pos, PlayerEntity caster) {
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
		if (bolt == null) return;
		bolt.refreshPositionAfterTeleport(pos);
		if (caster instanceof ServerPlayerEntity sp) bolt.setChanneler(sp);
		world.spawnEntity(bolt);
	}
}
