package dev.konosuba.magic;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

/** Жезл Кражи (Казума). ПКМ по мобу: шанс украсть предмет из его экипировки или найти "добычу". */
public class StealWandItem extends SpellItem {
	public StealWandItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity target, Hand hand) {
		if (user.getWorld().isClient) return ActionResult.SUCCESS;
		if (target instanceof PlayerEntity) {
			SpellUtil.actionBar(user, Text.translatable("message.konosuba_magic.steal.no_players").formatted(Formatting.GRAY));
			return ActionResult.FAIL;
		}

		ServerWorld world = (ServerWorld) user.getWorld();
		user.getItemCooldownManager().set(this, 40);

		// Скилл Казумы зависит от удачи: 50% успеха
		if (world.random.nextFloat() >= 0.5f) {
			SpellUtil.actionBar(user, Text.translatable("message.konosuba_magic.steal.fail").formatted(Formatting.RED));
			world.spawnParticles(ParticleTypes.SMOKE, target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
			return ActionResult.CONSUME;
		}

		List<EquipmentSlot> filled = new ArrayList<>();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (!target.getEquippedStack(slot).isEmpty()) filled.add(slot);
		}

		ItemStack loot;
		if (!filled.isEmpty()) {
			EquipmentSlot slot = filled.get(world.random.nextInt(filled.size()));
			loot = target.getEquippedStack(slot).copy();
			target.equipStack(slot, ItemStack.EMPTY);
		} else {
			loot = bonusLoot(world.random.nextInt(6));
		}

		user.getInventory().offerOrDrop(loot);
		world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.6f);
		world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.4, 0.4, 0.4, 0.0);
		SpellUtil.actionBar(user, Text.translatable("message.konosuba_magic.steal.success", loot.getName()).formatted(Formatting.GREEN));
		return ActionResult.CONSUME;
	}

	private static ItemStack bonusLoot(int roll) {
		return switch (roll) {
			case 0 -> new ItemStack(Items.EMERALD, 1);
			case 1 -> new ItemStack(Items.GOLD_NUGGET, 3);
			case 2 -> new ItemStack(Items.BREAD, 2);
			case 3 -> new ItemStack(Items.ARROW, 4);
			case 4 -> new ItemStack(Items.IRON_NUGGET, 4);
			default -> new ItemStack(Items.EXPERIENCE_BOTTLE, 1);
		};
	}
}
