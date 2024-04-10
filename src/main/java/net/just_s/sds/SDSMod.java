package net.just_s.sds;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SDSMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("sds");
	public static ConfigurationManager.Config config = new ConfigurationManager.Config();

	@Override
	public void onInitialize() {
		ConfigurationManager.onInit();
		LOGGER.info("SDS initialized successfully!");
	}

	public static boolean isBlockStateAllowed(BlockState state) {
		String name = Registries.BLOCK.getId(state.getBlock()).toString();

		//If the whitelist is enabled, checking if the block is in it or if it has an allowed tag.
		if(config.whitelist) return config.allowed.blocks.containsKey(name) || state.streamTags().anyMatch(blockTagKey -> config.allowed.tags.contains(blockTagKey.id().toString()));

		//If the block is not allowed, and it doesn't have property-specific limations, returning false.
		if(config.forbidden.blocks.containsKey(name) && config.forbidden.blocks.get(name).isEmpty()) return false;

        //Returnig true if there are no forbidden tags in this block state
        return state.streamTags().noneMatch(blockTagKey -> config.forbidden.tags.contains(blockTagKey.id().toString()));
	}

	public static boolean isPropertyAllowed(String property, Block block, BlockState state) {
		String blockName = Registries.BLOCK.getId(block).toString();
		if(config.whitelist) {
			//If the block is in allowed tags, returning true
			if(state.streamTags().anyMatch(blockTagKey -> config.allowed.tags.contains(blockTagKey.id().toString()))) return true;

			//If the block has property-specific limitations, and it matches with property from args, or properties are empty, returning true.
			List<String> blockProperties = config.allowed.blocks.get(blockName);
			if(blockProperties != null && (blockProperties.isEmpty() || blockProperties.contains(property))) return true;
			return config.allowed.properties.contains(property);
		}
		//If the whitelist is off, the block is allowed and the property from args isn't specified in the config, returning false.
		if(config.allowed.blocks.containsKey(blockName) && (!config.allowed.blocks.get(blockName).isEmpty() && !config.allowed.blocks.get(blockName).contains(property))) return false;

		//If the block has specified forbidden properties and one of them matches, returning false.
		List<String> properties = config.forbidden.blocks.get(blockName);
		if(properties != null && properties.contains(property)) return false;

		return !config.forbidden.properties.contains(property);
	}
}
