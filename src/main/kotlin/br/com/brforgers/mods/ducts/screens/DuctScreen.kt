package br.com.brforgers.mods.ducts.screens

import br.com.brforgers.mods.ducts.Ducts
import br.com.brforgers.mods.ducts.blockentities.DuctBlockEntity
import br.com.brforgers.mods.ducts.inventories.DuctInventory
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import javax.annotation.Nonnull


@OnlyIn(Dist.CLIENT)
class DuctScreen(screenContainer: DuctInventory, inv: Inventory, title: Component) :
    AbstractContainerScreen<DuctInventory>(screenContainer, inv, title) {
    private val duct: DuctBlockEntity

    override fun render(@Nonnull matrixStack: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        this.renderBackground(matrixStack)
        super.render(matrixStack, mouseX, mouseY, partialTicks)
        this.renderTooltip(matrixStack, mouseX, mouseY)
    }

    override fun renderBg(@Nonnull matrixStack: PoseStack, partialTicks: Float, x: Int, y: Int) {
        if (minecraft != null) {
            RenderSystem.setShader { GameRenderer.getPositionTexShader() }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            RenderSystem.setShaderTexture(0, HOPPER_GUI_TEXTURE)
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