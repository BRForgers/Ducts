package br.com.brforgers.mods.ducts.blockentities

import br.com.brforgers.mods.ducts.Ducts
import br.com.brforgers.mods.ducts.blocks.DuctBlock
import br.com.brforgers.mods.ducts.inventories.DuctInventory
import br.com.brforgers.mods.ducts.readNbt
import br.com.brforgers.mods.ducts.writeNbt
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.container.Container
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.HopperTileEntity
import net.minecraft.tileentity.ITickableTileEntity
import net.minecraft.tileentity.LockableTileEntity
import net.minecraft.tileentity.TileEntityType
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TranslationTextComponent


class DuctBlockEntity(
    private val inventory: IInventory = Inventory(1)
)
    : LockableTileEntity(type), IInventory by inventory, ITickableTileEntity {

    init {
        instance = this
    }

    var transferCooldown: Int = -1

    override fun getDefaultName(): ITextComponent {
        return TranslationTextComponent("block.ducts.duct")
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory): Container {
        return DuctInventory(syncId, playerInventory, this)
    }

    private fun attemptInsert(): Boolean {
        val stack = getItem(0)
        if (stack.isEmpty) return false

        if (blockState.getValue(DuctBlock.Props.powered)/*&& stack.count <= 1*/) return false

        val outputDir = blockState.getValue(DuctBlock.Props.output)
        val outputInv = HopperTileEntity.getContainerAt(level!!, blockPos.relative(outputDir)) ?: return false

        val stackCopy = getItem(0).copy()
        val ret = HopperTileEntity.addItem(this, outputInv, removeItem(0, 1), outputDir.opposite)
        if (ret.isEmpty) {
            outputInv.setChanged()
            return true
        }
        setItem(0, stackCopy)

        return false
    }

    override fun tick() {
        val world = level ?: return
        if(world.isClientSide) return

        transferCooldown--

        if (transferCooldown > 0) return

        transferCooldown = 0

        if (attemptInsert()) {
            transferCooldown = 8
            setChanged()
        }
    }

    override fun save(tag: CompoundNBT): CompoundNBT {
        super.save(tag)
        inventory.writeNbt(tag)
        tag.putInt("TransferCooldown", transferCooldown)
        return tag
    }

    override fun load(state: BlockState, tag: CompoundNBT) {
        super.load(state, tag)
        inventory.readNbt(tag)
        transferCooldown = tag.getInt("TransferCooldown")
    }

    override fun setChanged() {
        super.setChanged()
        inventory.setChanged()
    }

    companion object {
        val type: TileEntityType<DuctBlockEntity> = TileEntityType.Builder.of(::DuctBlockEntity, Ducts.DUCT_BLOCK).build(null)
        @JvmStatic lateinit var instance: DuctBlockEntity
    }

    override fun getContainerSize(): Int {
        return 1
    }
}