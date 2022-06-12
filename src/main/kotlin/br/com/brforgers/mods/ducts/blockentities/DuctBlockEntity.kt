package br.com.brforgers.mods.ducts.blockentities

import br.com.brforgers.mods.ducts.Ducts
import br.com.brforgers.mods.ducts.blocks.DuctBlock
import br.com.brforgers.mods.ducts.inventories.DuctInventory
import br.com.brforgers.mods.ducts.readNbt
import br.com.brforgers.mods.ducts.writeNbt
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.level.block.state.BlockState


class DuctBlockEntity(
    pos : BlockPos, state: BlockState, private val inventory: Container = SimpleContainer(1)
)
    : BaseContainerBlockEntity(type, pos, state), Container by inventory {

    init {
        instance = this
    }

    var transferCooldown: Int = -1

    override fun getDefaultName(): Component{
        return Component.translatable("block.ducts.duct")
    }

    override fun createMenu(syncId: Int, playerInventory: Inventory): AbstractContainerMenu {
        return DuctInventory(syncId, playerInventory, this)
    }

    private fun attemptInsert(world: Level?, pos: BlockPos?, state: BlockState?, blockEntity: DuctBlockEntity?): Boolean {
        val stack = blockEntity!!.getItem(0)
        if (stack.isEmpty) return false

        if (blockEntity.blockState.getValue(DuctBlock.Props.powered)/*&& stack.count <= 1*/) return false

        val outputDir = blockEntity.blockState.getValue(DuctBlock.Props.output)
        val outputInv = HopperBlockEntity.getContainerAt(world!!, pos?.relative(outputDir)!!) ?: return false

        val stackCopy = blockEntity.getItem(0).copy()
        val ret = HopperBlockEntity.addItem(blockEntity, outputInv, blockEntity.removeItem(0, 1), outputDir.opposite)
        if (ret.isEmpty) {
            outputInv.setChanged()
            return true
        }
        blockEntity.setItem(0, stackCopy)

        return false
    }

    fun tick(world: Level?, pos: BlockPos?, state: BlockState?, blockEntity: DuctBlockEntity?) {
        val world = world ?: return
        if(world.isClientSide) return

        blockEntity!!.transferCooldown--

        if (blockEntity.transferCooldown > 0) return

        blockEntity.transferCooldown = 0

        if (attemptInsert(world, pos, state, blockEntity)) {
            blockEntity.transferCooldown = 8
            blockEntity.setChanged()
        }
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        inventory.writeNbt(tag)
        tag.putInt("TransferCooldown", transferCooldown)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        inventory.readNbt(tag)
        transferCooldown = tag.getInt("TransferCooldown")
    }

    override fun setChanged() {
        super.setChanged()
        inventory.setChanged()
    }

    companion object {
        val type: BlockEntityType<DuctBlockEntity> = BlockEntityType.Builder.of(::DuctBlockEntity, Ducts.DUCT_BLOCK).build(null)
        @JvmStatic lateinit var instance: DuctBlockEntity
    }

    override fun getContainerSize(): Int {
        return 1
    }
}