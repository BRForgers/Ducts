package br.com.brforgers.mods.ducts

import br.com.brforgers.mods.ducts.screens.DuctGuiDescription
import br.com.brforgers.mods.ducts.screens.DuctScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text


object DuctsClient : ClientModInitializer {
    override fun onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(Ducts.DUCT_BLOCK, RenderLayer.getCutoutMipped())

        ScreenRegistry.register(
            Ducts.DUCT_SCREEN_HANDLER
        ) { gui: DuctGuiDescription?, inventory: PlayerInventory, title: Text? ->
            DuctScreen(
                gui,
                inventory.player,
                title
            )
        }
    }
}