package br.com.brforgers.mods.ducts

import br.com.brforgers.mods.ducts.blockentities.DuctBlockEntity
import br.com.brforgers.mods.ducts.blocks.DuctBlock
import br.com.brforgers.mods.ducts.inventories.DuctInventory
import br.com.brforgers.mods.ducts.screens.DuctScreen
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraftforge.common.extensions.IForgeMenuType
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.forge.registerObject
import thedarkcolour.kotlinforforge.forge.runForDist


@Mod(Ducts.ID)
object  Ducts {
    const val ID = "ducts"

    val LOGGER: Logger = LogManager.getLogger(ID)

    val DUCT_BLOCK: Block = DuctBlock()

    var DUCT_MENU: MenuType<DuctInventory> = IForgeMenuType.create { id: Int, inv: Inventory?, data: FriendlyByteBuf ->
        DuctInventory(id, inv!!, data.readBlockPos())
    }

    init {
        LOGGER.log(Level.INFO, "Ducts!")

        val BLOCK_REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, ID)
        BLOCK_REGISTRY.registerObject("duct") { DUCT_BLOCK }
        BLOCK_REGISTRY.register(MOD_BUS)

        val ITEM_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, ID)
        ITEM_REGISTRY.registerObject("duct") { BlockItem(DUCT_BLOCK, Item.Properties().tab(CreativeModeTab.TAB_REDSTONE)) }
        ITEM_REGISTRY.register(MOD_BUS)

        val BLOCK_ENTITY_REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, ID)
        BLOCK_ENTITY_REGISTRY.registerObject("duct") { DuctBlockEntity.type }
        BLOCK_ENTITY_REGISTRY.register(MOD_BUS)

        val CONTAINER_REGISTRY = DeferredRegister.create(ForgeRegistries.CONTAINERS, ID)
        CONTAINER_REGISTRY.registerObject("duct") { DUCT_MENU }
        CONTAINER_REGISTRY.register(MOD_BUS)

        runForDist(
            clientTarget = {
                MOD_BUS.addListener(::onClientSetup)
            },
            serverTarget = {

            }
        )

    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        MenuScreens.register(DUCT_MENU, ::DuctScreen)
    }
}