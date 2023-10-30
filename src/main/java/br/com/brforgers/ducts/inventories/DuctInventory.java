package br.com.brforgers.ducts.inventories;

import br.com.brforgers.ducts.Ducts;
import br.com.brforgers.ducts.blockentities.DuctBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DuctInventory extends AbstractContainerMenu
{
	private DuctBlockEntity duct;
	
	public DuctInventory(int id, Inventory playerInventory, FriendlyByteBuf packet)
	{
		this(id, playerInventory, playerInventory.player.level.getBlockEntity(packet.readBlockPos()) instanceof DuctBlockEntity ductBlock ? ductBlock : null);
	}

	public DuctInventory(int pContainerId, Inventory playerInventory, DuctBlockEntity inventory) 
	{
		super(Ducts.DUCT_MENU.get(), pContainerId);
		
		duct = inventory;
        checkContainerSize(inventory, 1);
        inventory.startOpen(playerInventory.player);
        
        addSlot(new Slot(inventory, 0, 80, 20));
        
        for (int l = 0; l < 3; l++)
        {
        	for (int k = 0; k < 9; k++)
        	{
        		addSlot(new Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, l * 18 + 51));
        	}
        }
        for (int i = 0; i < 9; i++)
        {
        	addSlot(new Slot(playerInventory, i, 8 + i * 18, 109));
        }
	}

	@Override
	public ItemStack quickMoveStack(Player pPlayer, int pIndex) 
	{
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = slots.get(pIndex);
		
		if (slot.hasItem())
		{
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			
			if (pIndex < duct.getContainerSize())
			{
				if (!moveItemStackTo(itemstack1, duct.getContainerSize(), slots.size(), true))
					return ItemStack.EMPTY;
			}
			else if (!moveItemStackTo(itemstack1, 0, duct.getContainerSize(), false))
			{
				return ItemStack.EMPTY;
			}
			
			if (itemstack1.isEmpty())
				slot.set(ItemStack.EMPTY);
			else
				slot.setChanged();
		}
		
		return itemstack;
	}

	@Override
	public boolean stillValid(Player pPlayer) 
	{
		return duct.stillValid(pPlayer);
	}

	@Override
	public void removed(Player player) 
	{
		super.removed(player);
        duct.stopOpen(player);
	}
}
