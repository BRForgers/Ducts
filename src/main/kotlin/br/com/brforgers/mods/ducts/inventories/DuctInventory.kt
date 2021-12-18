package br.com.brforgers.mods.ducts.inventories

import br.com.brforgers.mods.ducts.Ducts
import br.com.brforgers.mods.ducts.blockentities.DuctBlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.container.Container
import net.minecraft.inventory.container.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import javax.annotation.Nonnull


class DuctInventory(syncId: Int, playerInventory: PlayerInventory, inventory: DuctBlockEntity) :
    Container(Ducts.DUCT_MENU, syncId) {
    val duct: DuctBlockEntity

    init {
        duct = inventory
        checkContainerSize(inventory, 1)
        inventory.startOpen(playerInventory.player)
        addSlot(Slot(inventory, 0, 80, 20))
        for (l in 0..2) {
            for (k in 0..8) {
                addSlot(Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, l * 18 + 51))
            }
        }
        for (i1 in 0..8) {
            addSlot(Slot(playerInventory, i1, 8 + i1 * 18, 109))
        }
    }

    constructor(id: Int, playerInventory: PlayerInventory, pos: BlockPos?) : this(
        id,
        playerInventory,
        playerInventory.player.level.getBlockEntity(pos!!) as DuctBlockEntity
    )

    override fun stillValid(@Nonnull player: PlayerEntity): Boolean {
        return duct.stillValid(player)
    }

    @Nonnull
    override fun quickMoveStack(@Nonnull player: PlayerEntity, index: Int): ItemStack {
        var itemstack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val itemstack1 = slot.item
            itemstack = itemstack1.copy()
            if (index < duct.containerSize) {
                if (!moveItemStackTo(itemstack1, duct.containerSize, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else if (!moveItemStackTo(itemstack1, 0, duct.containerSize, false)) {
                return ItemStack.EMPTY
            }
            if (itemstack1.isEmpty) {
                slot.set(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
        }
        return itemstack
    }

    override fun removed(@Nonnull player: PlayerEntity) {
        super.removed(player)
        duct.stopOpen(player)
    }


}