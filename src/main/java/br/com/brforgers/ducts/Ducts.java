package br.com.brforgers.ducts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.com.brforgers.ducts.blockentities.DuctBlockEntity;
import br.com.brforgers.ducts.blocks.DuctBlock;
import br.com.brforgers.ducts.inventories.DuctInventory;
import br.com.brforgers.ducts.screens.DuctScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Ducts.MOD_ID)
public class Ducts 
{
	public static final String MOD_ID = "ducts";
	public static final Logger LOGGER = LogManager.getLogger(Ducts.class);
	
	private static final DeferredRegister<Block> BLOCK_REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
	private static final DeferredRegister<Item> ITEM_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCKENTITY_REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
	private static final DeferredRegister<MenuType<?>> MENUTYPE_REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

	public static final RegistryObject<Block> DUCT_BLOCK = BLOCK_REGISTRY.register("duct", () -> new DuctBlock());
	public static final RegistryObject<Item> DUCT_ITEM = ITEM_REGISTRY.register("duct", () -> new BlockItem(DUCT_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_REDSTONE)));
	public static final RegistryObject<BlockEntityType<DuctBlockEntity>> DUCT_BLOCKENTITY_TYPE = BLOCKENTITY_REGISTRY.register("duct", () -> BlockEntityType.Builder.of(DuctBlockEntity::new, DUCT_BLOCK.get()).build(null));
	
	public static final RegistryObject<MenuType<DuctInventory>> DUCT_MENU = MENUTYPE_REGISTRY.register("duct", () -> IForgeMenuType.create(DuctInventory::new));
	
	public Ducts() 
	{
		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		
		BLOCK_REGISTRY.register(modEventBus);
		ITEM_REGISTRY.register(modEventBus);
		BLOCKENTITY_REGISTRY.register(modEventBus);
		MENUTYPE_REGISTRY.register(modEventBus);
		
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(Ducts::onClientSetupEvent));
	}
	
	public static void onClientSetupEvent(FMLClientSetupEvent event) 
	{
		MenuScreens.register(DUCT_MENU.get(), DuctScreen::new);
	}
}
