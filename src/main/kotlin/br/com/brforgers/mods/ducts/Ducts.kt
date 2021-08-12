package br.com.brforgers.mods.ducts

import br.com.brforgers.mods.ducts.blockentities.DuctBlockEntity
import br.com.brforgers.mods.ducts.blocks.DuctBlock
import br.com.brforgers.mods.ducts.config.DuctsConfig
import br.com.brforgers.mods.ducts.screens.DuctGuiDescription
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.block.Block
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger


object Ducts : ModInitializer {
    const val MOD_ID = "ducts"
    var logger: Logger = LogManager.getLogger("Ducts")

    val DUCT: Identifier = Identifier(MOD_ID, "duct")

    val DUCT_BLOCK: Block = DuctBlock(::DuctGuiDescription)

    val DUCT_SCREEN_HANDLER: ScreenHandlerType<DuctGuiDescription> = ScreenHandlerRegistry.registerExtended<DuctGuiDescription>(
            DUCT
    ){ syncId: Int, inventory: PlayerInventory, buf ->
        DuctGuiDescription(
                syncId,
                inventory,
                ScreenHandlerContext.create(inventory.player.world, buf.readBlockPos())
        )
    }

    override fun onInitialize(){
        logger.info("Ducts!")
        AutoConfig.register(DuctsConfig::class.java, ::Toml4jConfigSerializer)
        Registry.register(Registry.BLOCK, DUCT, DUCT_BLOCK)
        Registry.register(Registry.ITEM, DUCT, BlockItem(DUCT_BLOCK, Item.Settings().group(ItemGroup.REDSTONE)))
        Registry.register(Registry.BLOCK_ENTITY_TYPE, DUCT, DuctBlockEntity.type)

    }

    fun config(): DuctsConfig? {
        return AutoConfig.getConfigHolder(DuctsConfig::class.java).config
    }

}
