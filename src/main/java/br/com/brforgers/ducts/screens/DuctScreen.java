package br.com.brforgers.ducts.screens;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import br.com.brforgers.ducts.Ducts;
import br.com.brforgers.ducts.inventories.DuctInventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DuctScreen extends AbstractContainerScreen<DuctInventory>
{
	private static final ResourceLocation HOPPER_GUI_TEXTURE = new ResourceLocation(Ducts.MOD_ID, "textures/gui/container/duct.png");
	
	public DuctScreen(DuctInventory pMenu, Inventory pPlayerInventory, Component pTitle) 
	{
		super(pMenu, pPlayerInventory, pTitle);
		
		passEvents = false;
		imageHeight = 133;
		inventoryLabelY = imageHeight - 94;
	}
	
	@Override
	public void render(@Nonnull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) 
	{
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

	@Override
	protected void renderBg(PoseStack matrixStack, float pPartialTick, int pMouseX, int pMouseY) 
	{
		if (minecraft != null) 
		{
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.setShaderTexture(0, HOPPER_GUI_TEXTURE);
            int i = (width - imageWidth) / 2;
            int j = (height - imageHeight) / 2;
            this.blit(matrixStack, i, j, 0, 0, imageWidth, imageHeight);
        }
	}
}
