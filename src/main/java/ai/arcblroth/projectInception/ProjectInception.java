package ai.arcblroth.projectInception;

import ai.arcblroth.projectInception.block.GameBlock;
import ai.arcblroth.projectInception.block.GameBlockEntity;
import ai.arcblroth.projectInception.block.InceptionBlock;
import ai.arcblroth.projectInception.item.BlockItemWithMagicness;
import ai.arcblroth.projectInception.item.InceptionInterfaceItem;
import io.github.cottonmc.cotton.datapack.recipe.RecipeUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.LinkedList;

public class ProjectInception implements ModInitializer {

	public static final String MODID = "project_inception";
	public static final Logger LOGGER = ProjectInceptionEarlyRiser.LOGGER;
	public static final boolean IS_INNER = ProjectInceptionEarlyRiser.IS_INNER;
	public static final String[] ARGUMENTS = ProjectInceptionEarlyRiser.ARGUMENTS;
	public static final String MAIN_CLASS = "net.fabricmc.loader.launch.knot.KnotClient";
	public static final String DEV_MAIN_CLASS = "net.fabricmc.devlaunchinjector.Main";

	public static ChronicleQueue toParentQueue = null;
	public static GameInstance focusedInstance = null;

	public static ItemGroup STUFF;

	public static GameBlock GAME_BLOCK;
	public static BlockItemWithMagicness GAME_BLOCK_ITEM;
	public static BlockEntityType<GameBlockEntity> GAME_BLOCK_ENTITY_TYPE;
	public static HorizontalFacingBlock INCEPTION_BLOCK;
	public static BlockItemWithMagicness INCEPTION_BLOCK_ITEM;
	public static InceptionInterfaceItem INCEPTION_INTERFACE_ITEM;

	@Override
	public void onInitialize() {
    	LOGGER.log(Level.INFO, "Registering stuff...");
		STUFF = FabricItemGroupBuilder.create(new Identifier(MODID, "stuff")).icon(() -> new ItemStack(INCEPTION_INTERFACE_ITEM)).build();

		INCEPTION_BLOCK = Registry.register(Registry.BLOCK, new Identifier(MODID, "inception_block"),
				new InceptionBlock(AbstractBlock.Settings.of(Material.METAL).strength(2).emissiveLighting((s, v, w) -> true)));
		INCEPTION_BLOCK_ITEM = Registry.register(Registry.ITEM, new Identifier(MODID, "inception_block"),
				new BlockItemWithMagicness(INCEPTION_BLOCK, new Item.Settings().group(STUFF).rarity(Rarity.RARE), false, true));
		GAME_BLOCK = Registry.register(Registry.BLOCK, new Identifier(MODID, "game_block"),
				new GameBlock(AbstractBlock.Settings.of(Material.METAL).strength(2).nonOpaque().emissiveLighting((s, v, w) -> true)));
		GAME_BLOCK_ENTITY_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MODID, "game_block"),
				BlockEntityType.Builder.create(GameBlockEntity::new, GAME_BLOCK).build(null));
		GAME_BLOCK_ITEM = Registry.register(Registry.ITEM, new Identifier(MODID, "game_block"),
				new BlockItemWithMagicness(GAME_BLOCK, new Item.Settings().group(STUFF).rarity(Rarity.RARE), false, true));
		INCEPTION_INTERFACE_ITEM = Registry.register(Registry.ITEM, new Identifier(MODID, "inception_interface"),
				new InceptionInterfaceItem(new Item.Settings().group(STUFF).rarity(Rarity.RARE)));

		if(FabricLoader.getInstance().isModLoaded("techreborn")) {
			RecipeUtil.removeRecipe(new Identifier(MODID, "inception_block_vanilla"));
			RecipeUtil.removeRecipe(new Identifier(MODID, "inception_interface_vanilla"));
		} else {
			RecipeUtil.removeRecipe(new Identifier(MODID, "inception_block_techreborn"));
			RecipeUtil.removeRecipe(new Identifier(MODID, "inception_interface_techreborn"));
		}
	}

}
