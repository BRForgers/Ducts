package br.com.brforgers.mods.ducts.screens

import br.com.brforgers.mods.ducts.Ducts
import br.com.brforgers.mods.ducts.blockentities.DuctBlockEntity
import br.com.brforgers.mods.ducts.inventories.DuctInventory
import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.screen.inventory.ContainerScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.ITextComponent
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import javax.annotation.Nonnull


@OnlyIn(Dist.CLIENT)
class DuctScreen(screenContainer: DuctInventory, inv: PlayerInventory, title: ITextComponent) :
    ContainerScreen<DuctInventory>(screenContainer, inv, title) {
    private val duct: DuctBlockEntity

    override fun render(@Nonnull matrixStack: MatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        this.renderBackground(matrixStack)
        super.render(matrixStack, mouseX, mouseY, partialTicks)
        this.renderTooltip(matrixStack, mouseX, mouseY)
    }

    override fun renderBg(@Nonnull matrixStack: MatrixStack, partialTicks: Float, x: Int, y: Int) {
        if (minecraft != null) {
            RenderSystem.color4f(1f, 1f, 1f, 1f)
            minecraft!!.textureManager.bind(HOPPER_GUI_TEXTURE)
            val i = (width - imageWidth) / 2
            val j = (height - imageHeight) / 2
            this.blit(matrixStack, i, j, 0, 0, imageWidth, imageHeight)
        }
    }

    companion object {
        private val HOPPER_GUI_TEXTURE =
            ResourceLocation(Ducts.ID, "textures/gui/container/duct.png")
    }

    init {
        duct = screenContainer.duct
        passEvents = false
        imageHeight = 133
        inventoryLabelY = imageHeight - 94
    }
}