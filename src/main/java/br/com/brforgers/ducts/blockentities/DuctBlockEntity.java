package br.com.brforgers.ducts.blockentities;

import br.com.brforgers.ducts.Ducts;
import br.com.brforgers.ducts.blocks.DuctBlock.DuctProperties;
import br.com.brforgers.ducts.inventories.DuctInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DuctBlockEntity extends BaseContainerBlockEntity
{
	private Container inventory = new SimpleContainer(1);
	private int transferCooldown = -1;
	
	public DuctBlockEntity(BlockPos pPos, BlockState pBlockState) 
	{
		super(Ducts.DUCT_BLOCKENTITY_TYPE.get(), pPos, pBlockState);
	}
	
	public static void tick(Level world, BlockPos pos, BlockState state, DuctBlockEntity blockEntity) 
	{
		if (world == null || world.isClientSide())
			return;
		
		if (blockEntity != null) 
		{
			blockEntity.transferCooldown--;
			
			if (blockEntity.transferCooldown > 0)
				return;
			
	        blockEntity.transferCooldown = 0;
	        
	        if (attemptInsert(world, pos, state, blockEntity)) 
	        {
	            blockEntity.transferCooldown = 8;
	            blockEntity.setChanged();
	        }
		}
	}
	
	private static boolean attemptInsert(Level world, BlockPos pos, BlockState state, DuctBlockEntity blockEntity)
	{
		ItemStack stack = blockEntity.getItem(0);
		
        if (stack.isEmpty()) 
        	return false;

        
        if (blockEntity.getBlockState().getValue(DuctProperties.POWERED)) 
        	return false;

        Direction outputDir = blockEntity.getBlockState().getValue(DuctProperties.OUTPUT);
        Container outputInv = HopperBlockEntity.getContainerAt(world, pos.relative(outputDir));
        
        if (outputInv == null)
        	return false;

        ItemStack stackCopy = blockEntity.getItem(0).copy();
        ItemStack ret = HopperBlockEntity.addItem(blockEntity, outputInv, blockEntity.removeItem(0, 1), outputDir.getOpposite());
        
        if (ret.isEmpty()) 
        {
            outputInv.setChanged();
            return true;
        }
        
        blockEntity.setItem(0, stackCopy);

        return false;
    }
	
	@Override
	protected Component getDefaultName() 
	{
		return Component.translatable("block.ducts.duct");
	}

	@Override
	protected AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory) 
	{
		return new DuctInventory(pContainerId, pInventory, this);
	}
	
	@Override
	protected void saveAdditional(CompoundTag tag) 
	{
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, NonNullList.of(inventory.getItem(0)));
        tag.putInt("TransferCooldown", transferCooldown);
    }

	@Override
    public void load(CompoundTag tag) 
	{
        super.load(tag);
        NonNullList<ItemStack> items = NonNullList.of(ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        inventory.setItem(0, items.get(0));
        transferCooldown = tag.getInt("TransferCooldown");
    }

	@Override
    public void setChanged() 
	{
        super.setChanged();
        inventory.setChanged();
    }

	@Override
	public int getContainerSize() 
	{
		return inventory.getContainerSize();
	}


	@Override
	public boolean isEmpty() 
	{
		return inventory.isEmpty();
	}


	@Override
	public ItemStack getItem(int pSlot) 
	{
		return inventory.getItem(pSlot);
	}


	@Override
	public ItemStack removeItem(int pSlot, int pAmount) 
	{
		return inventory.removeItem(pSlot, pAmount);
	}


	@Override
	public ItemStack removeItemNoUpdate(int pSlot) 
	{
		return inventory.removeItemNoUpdate(pSlot);
	}


	@Override
	public void setItem(int pSlot, ItemStack pStack) 
	{
		inventory.setItem(pSlot, pStack);
	}


	@Override
	public boolean stillValid(Player pPlayer) 
	{
		return inventory.stillValid(pPlayer);
	}


	@Override
	public void clearContent() 
	{
		inventory.clearContent();
	}

}
