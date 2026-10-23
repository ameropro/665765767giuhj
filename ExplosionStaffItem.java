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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Посох Взрыва (Мегумин). Зажми ПКМ — идёт "чтение заклинания", рисуется магический круг.
 * Отпусти после полной зарядки — EXPLOSION! После каста маг обессилен.
 */
public class ExplosionStaffItem extends SpellItem {
	private static final int HOLD_TIME = 72000;
	private static final int MIN_CHARGE = 40;   // 2 секунды
	private static final int MAX_CHARGE = 100;  // 5 секунд
	private static final double RANGE = 80.0;

	public ExplosionStaffItem(Settings settings) {
		super(settings);
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BOW;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return HOLD_TIME;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		user.setCurrentHand(hand);
		return TypedActionResult.consume(user.getStackInHand(hand));
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!(world instanceof ServerWorld server) || !(user instanceof PlayerEntity player)) return;
		int charged = Math.min(HOLD_TIME - remainingUseTicks, MAX_CHARGE);

		Vec3d target = SpellUtil.targetPos(player, RANGE);
		double radius = 1.5 + 3.5 * (charged / (double) MAX_CHARGE);

		if (charged % 2 == 0) {
			SpellUtil.ring(server, target, radius, ParticleTypes.FLAME, 40);
			SpellUtil.ring(server, target, radius * 0.6, ParticleTypes.ENCHANT, 24);
			SpellUtil.star(server, target, radius, ParticleTypes.SOUL_FIRE_FLAME, 5, 8);
		}
		// Частицы вокруг самого мага
		server.spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(),
			3, 0.4, 0.6, 0.4, 0.5);

		if (charged == MIN_CHARGE) {
			world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.5f);
		}
		if (charged % 5 == 0) {
			int filled = charged * 20 / MAX_CHARGE;
			String bar = "█".repeat(filled) + "░".repeat(20 - filled);
			Formatting color = charged >= MIN_CHARGE ? Formatting.RED : Formatting.DARK_GRAY;
			SpellUtil.actionBar(player, Text.literal(bar).formatted(color));
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (world.isClient || !(user instanceof PlayerEntity player)) return;
		int charged = Math.min(HOLD_TIME - remainingUseTicks, MAX_CHARGE);

		if (charged < MIN_CHARGE) {
			SpellUtil.actionBar(player, Text.translatable("message.konosuba_magic.explosion.interrupted").formatted(Formatting.GRAY));
			return;
		}

		Vec3d pos = SpellUtil.targetPos(player, RANGE);
		float power = 4.0f + 4.0f * (charged - MIN_CHARGE) / (float) (MAX_CHARGE - MIN_CHARGE); // 4..8
		ServerWorld server = (ServerWorld) world;

		// MOB = учитывает gamerule mobGriefing
		world.createExplosion(player, pos.x, pos.y, pos.z, power, true, World.ExplosionSourceType.MOB);
		server.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1, pos.z, 5, power / 3, 1.0, power / 3, 0.0);

		player.sendMessage(Text.translatable("message.konosuba_magic.explosion.cast").formatted(Formatting.RED, Formatting.BOLD), true);

		if (!player.isCreative()) {
			// Мегумин после каста падает без сил
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 4));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 600, 2));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 600, 2));
			player.getItemCooldownManager().set(this, 1200);
		}
	}
}
