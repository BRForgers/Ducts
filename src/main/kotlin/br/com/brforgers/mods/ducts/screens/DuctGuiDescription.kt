package br.com.brforgers.mods.ducts.screens

import br.com.brforgers.mods.ducts.Ducts
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.WLabel
import io.github.cottonmc.cotton.gui.widget.data.Insets
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext


class DuctGuiDescription(
    syncId: Int,
    playerInv: PlayerInventory,
    context: ScreenHandlerContext
) : SyncedGuiDescription(
    Ducts.DUCT_SCREEN_HANDLER,
    syncId,
    playerInv,
    getBlockInventory(context),
    getBlockPropertyDelegate(context)

) {
    init {
        val root = WGridPanel()
        setRootPanel(root)
        root.insets = Insets.ROOT_PANEL
//        (blockInventory as? ExtendedScreenHandlerFactory)?.let { inv ->
//            val label = WLabel(inv.displayName)
//            root.add(label, 0, 0, 9, 1)
//        }
        val itemSlot = WItemSlot.of(blockInventory, 0)
        root.add(itemSlot, 4, 1)
        root.add(this.createPlayerInventoryPanel(), 0, 3)
        root.validate(this)
    }
}