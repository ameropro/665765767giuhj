package dev.konosuba.magic;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KonosubaMagic implements ModInitializer {
	public static final String MOD_ID = "konosuba_magic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.register();
		LOGGER.info("Konosuba Magic loaded. EXPLOSION!");
	}
}
