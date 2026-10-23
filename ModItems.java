package dev.konosuba.magic;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {
	private ModItems() {}

	public static final Item EXPLOSION_STAFF = register("explosion_staff",
		new ExplosionStaffItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
	public static final Item STEAL_WAND = register("steal_wand",
		new StealWandItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
	public static final Item LIGHTNING_STAFF = register("lightning_staff",
		new LightningStaffItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
	public static final Item FREEZE_STAFF = register("freeze_staff",
		new FreezeStaffItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
	public static final Item AQUA_STAFF = register("aqua_staff",
		new AquaStaffItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
	public static final Item WIND_STAFF = register("wind_staff",
		new WindStaffItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
	public static final Item DRAIN_TOUCH = register("drain_touch",
		new DrainTouchItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));

	public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP,
		Identifier.of(KonosubaMagic.MOD_ID, "magic"),
		FabricItemGroup.builder()
			.icon(() -> new ItemStack(EXPLOSION_STAFF))
			.displayName(Text.translatable("itemGroup.konosuba_magic.magic"))
			.entries((ctx, entries) -> {
				entries.add(EXPLOSION_STAFF);
				entries.add(STEAL_WAND);
				entries.add(LIGHTNING_STAFF);
				entries.add(FREEZE_STAFF);
				entries.add(AQUA_STAFF);
				entries.add(WIND_STAFF);
				entries.add(DRAIN_TOUCH);
			})
			.build());

	private static Item register(String name, Item item) {
		return Registry.register(Registries.ITEM, Identifier.of(KonosubaMagic.MOD_ID, name), item);
	}

	/** Вызывается из onInitialize, чтобы статические поля точно инициализировались. */
	public static void register() {}
}
