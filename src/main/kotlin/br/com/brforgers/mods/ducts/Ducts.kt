package br.com.brforgers.mods.ducts

import br.com.brforgers.mods.ducts.blockentities.DuctBlockEntity
import br.com.brforgers.mods.ducts.blocks.DuctBlock
import br.com.brforgers.mods.ducts.inventories.DuctInventory
import br.com.brforgers.mods.ducts.screens.DuctScreen
import net.minecraft.block.Block
import net.minecraft.client.gui.ScreenManager
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.container.ContainerType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.network.PacketBuffer
import net.minecraftforge.common.extensions.IForgeContainerType
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.registries.ForgeRegistries
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.forge.KDeferredRegister
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.forge.runForDist

@Mod(Ducts.ID)
object  Ducts {
    const val ID = "ducts"

    val LOGGER: Logger = LogManager.getLogger(ID)

    val DUCT_BLOCK: Block = DuctBlock()

    var DUCT_MENU: ContainerType<DuctInventory> = IForgeContainerType.create { id: Int, inv: PlayerInventory?, data: PacketBuffer ->
        DuctInventory(id, inv!!, data.readBlockPos())
    }

    init {
        LOGGER.log(Level.INFO, "Ducts!")

        val BLOCK_REGISTRY = KDeferredRegister(ForgeRegistries.BLOCKS, ID)
        BLOCK_REGISTRY.registerObject("duct") { DUCT_BLOCK }
        BLOCK_REGISTRY.register(MOD_BUS)

        val ITEM_REGISTRY = KDeferredRegister(ForgeRegistries.ITEMS, ID)
        ITEM_REGISTRY.registerObject("duct") { BlockItem(DUCT_BLOCK, Item.Properties().tab(ItemGroup.TAB_REDSTONE)) }
        ITEM_REGISTRY.register(MOD_BUS)

        val BLOCK_ENTITY_REGISTRY = KDeferredRegister(ForgeRegistries.TILE_ENTITIES, ID)
        BLOCK_ENTITY_REGISTRY.registerObject("duct") { DuctBlockEntity.type }
        BLOCK_ENTITY_REGISTRY.register(MOD_BUS)

        val CONTAINER_REGISTRY = KDeferredRegister(ForgeRegistries.CONTAINERS, ID)
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
        ScreenManager.register(DUCT_MENU, ::DuctScreen)
    }
}