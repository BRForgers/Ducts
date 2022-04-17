package br.com.brforgers.mods.ducts.blockentities

import br.com.brforgers.mods.ducts.Ducts
import br.com.brforgers.mods.ducts.blocks.DuctBlock
import br.com.brforgers.mods.ducts.readNbt
import br.com.brforgers.mods.ducts.screens.DuctGuiDescription
import br.com.brforgers.mods.ducts.writeNbt
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.gnomecraft.cooldowncoordinator.*
import net.minecraft.block.BlockState
import net.minecraft.block.entity.HopperBlockEntity
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World


class DuctBlockEntity(
    pos : BlockPos, state: BlockState,private val inventory: Inventory = SimpleInventory(1))
    : LockableContainerBlockEntity(type,pos,state), Inventory by inventory,
        ExtendedScreenHandlerFactory, CoordinatedCooldown {

    init {
        instance = this
    }

    private var lastTickTime: Long = Long.MAX_VALUE
    var transferCooldown: Int = -1

    override fun getContainerName(): Text {
        return TranslatableText("block.ducts.duct")
    }

    override fun createScreenHandler(syncId: Int, playerInventory: PlayerInventory): ScreenHandler {
        return DuctGuiDescription(syncId, playerInventory, ScreenHandlerContext.create(world, pos))
    }

    override fun writeScreenOpeningData(p0: ServerPlayerEntity?, p1: PacketByteBuf?) {
        p1?.writeBlockPos(this.pos)
    }

    private fun attemptInsert(world: World?, pos: BlockPos?, state: BlockState?, blockEntity: DuctBlockEntity?): Boolean {
        val stack = blockEntity!!.getStack(0)
        if (stack!!.isEmpty) return false

        if (blockEntity.cachedState[DuctBlock.Props.powered]/*&& stack.count <= 1*/) return false

        val outputDir = blockEntity.cachedState[DuctBlock.Props.output]
        val outputInv = HopperBlockEntity.getInventoryAt(world, pos?.offset(outputDir))

        val targetEntity = world!!.getBlockEntity(pos!!.offset(outputDir))
        val targetWasEmpty: Boolean
        val transferSucceeded: Boolean

        if (outputInv != null) {
            val stackCopy = blockEntity.getStack(0).copy()
            targetWasEmpty = CooldownCoordinator.isInventoryEmpty(outputInv)
            transferSucceeded = HopperBlockEntity.transfer(blockEntity, outputInv, blockEntity.removeStack(0, 1), outputDir.opposite).isEmpty
            if (!transferSucceeded) {
                blockEntity.setStack(0, stackCopy)
            }
        } else {
            val target = ItemStorage.SIDED.find(world, pos?.offset(outputDir),outputDir.opposite) ?: return false
            targetWasEmpty = CooldownCoordinator.isStorageEmpty(target)
            transferSucceeded = StorageUtil.move(InventoryStorage.of(blockEntity.inventory, outputDir), target, { iv: ItemVariant? -> true }, 1, null) > 0
        }

        if (transferSucceeded) {
            if (targetWasEmpty) {
                CooldownCoordinator.notify(targetEntity)
            }
            targetEntity?.markDirty()
            return true
        }

        return false
    }

    fun tick(world: World?, pos: BlockPos?, state: BlockState?, blockEntity: DuctBlockEntity?) {
        val world = world ?: return
        if(world.isClient) return

        blockEntity!!.transferCooldown--
        blockEntity.lastTickTime = world.time

        if (blockEntity.transferCooldown > 0) return

        blockEntity.transferCooldown = 0

        if (attemptInsert(world, pos, state, blockEntity)) {
            blockEntity.transferCooldown = 8
            blockEntity.markDirty()
        }
    }

    override fun writeNbt(tag: NbtCompound) {
        super.writeNbt(tag)
        inventory.writeNbt(tag)
        tag.putInt("TransferCooldown", transferCooldown)
    }

    override fun readNbt(tag: NbtCompound) {
        super.readNbt(tag)
        inventory.readNbt(tag)
        transferCooldown = tag.getInt("TransferCooldown")
    }

    override fun markDirty() {
        super.markDirty()
        inventory.markDirty()
    }

    companion object {
        val type = FabricBlockEntityTypeBuilder.create(::DuctBlockEntity, Ducts.DUCT_BLOCK).build(null)!!
        @JvmStatic lateinit var instance: DuctBlockEntity
    }

    override fun notifyCooldown() {
        val worldTime = this.world?.time ?: Long.MAX_VALUE
        if (this.lastTickTime >= worldTime) {
            this.transferCooldown = 7
        } else {
            this.transferCooldown = 8
        }
        super.markDirty()
    }
}
