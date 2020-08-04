package br.com.brforgers.mods.hoppersplus

import br.com.brforgers.mods.hoppersplus.blocks.DuctBlock
import br.com.brforgers.mods.hoppersplus.screens.DuctGuiDescription
import br.com.brforgers.mods.hoppersplus.screens.DuctScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text


object HoppersPlusClient : ClientModInitializer {
    override fun onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(HoppersPlus.DUCT_BLOCK, RenderLayer.getCutoutMipped())

        ScreenRegistry.register(
            HoppersPlus.DUCT_SCREEN_HANDLER
        ) { gui: DuctGuiDescription?, inventory: PlayerInventory, title: Text? ->
            DuctScreen(
                gui,
                inventory.player,
                title
            )
        }
    }
}