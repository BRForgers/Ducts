package br.com.brforgers.mods.ducts.blockentities

import alexiil.mc.lib.attributes.SearchOptions
import alexiil.mc.lib.attributes.item.ItemAttributes
import alexiil.mc.lib.attributes.item.ItemInvUtil
import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable
import br.com.brforgers.mods.ducts.Ducts
import br.com.brforgers.mods.ducts.blocks.DuctBlock
import br.com.brforgers.mods.ducts.readNbt
import br.com.brforgers.mods.ducts.screens.DuctGuiDescription
import br.com.brforgers.mods.ducts.writeNbt
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.HopperBlockEntity
import net.minecraft.entity.player.PlayerEntity
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
    : BlockEntity(type,pos,state), Inventory by inventory,
        ExtendedScreenHandlerFactory {

    init {
        instance = this
    }

    var transferCooldown: Int = -1

    var customName: Text? = null

    override fun getDisplayName(): Text {
        return customName ?: TranslatableText("block.ducts.duct")
    }


    override fun writeScreenOpeningData(p0: ServerPlayerEntity?, p1: PacketByteBuf?) {
        p1?.writeBlockPos(this.pos)
    }

    override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity): ScreenHandler? {
        return DuctGuiDescription(syncId, inv, ScreenHandlerContext.create(world, pos))
    }

    private fun attemptInsert(world: World?, pos: BlockPos?, state: BlockState?, blockEntity: DuctBlockEntity?): Boolean {
        val world = world ?: return false

        val stack = blockEntity!!.getStack(0)
        if (stack!!.isEmpty) return false

        if (blockEntity.cachedState[DuctBlock.Props.powered]/*&& stack.count <= 1*/) return false

        val outputDir = blockEntity.cachedState[DuctBlock.Props.output]
        val outputInv = HopperBlockEntity.getInventoryAt(world, pos?.offset(outputDir))

        if(outputInv != null){
            val stackCopy = blockEntity.getStack(0).copy()
            val ret = HopperBlockEntity.transfer(blockEntity, outputInv, blockEntity.removeStack(0, 1), outputDir.opposite)
            if (ret.isEmpty) {
                outputInv.markDirty()
                return true
            }
            blockEntity.setStack(0, stackCopy)
        } else {
            val insertable = ItemAttributes.INSERTABLE[world, pos?.offset(outputDir), SearchOptions.inDirection(outputDir)]
            if (insertable == RejectingItemInsertable.NULL) {
                return false
            }
            val extractable = FixedInventoryVanillaWrapper(this).extractable

            return ItemInvUtil.move(extractable, insertable, 1) > 0
        }
        return false
    }

    fun tick(world: World?, pos: BlockPos?, state: BlockState?, blockEntity: DuctBlockEntity?) {
        blockEntity!!.transferCooldown--

        if (blockEntity.transferCooldown > 0) return

        blockEntity.transferCooldown = 0

        if (attemptInsert(world, pos, state, blockEntity)) {
            blockEntity.transferCooldown = 8
            blockEntity.markDirty()
        }
    }

    override fun writeNbt(tag: NbtCompound): NbtCompound {
        super.writeNbt(tag)
        inventory.writeNbt(tag)
        tag.putInt("TransferCooldown", transferCooldown)
        if (customName != null) {
            tag.putString("CustomName", Text.Serializer.toJson(customName))
        }
        return tag
    }

    override fun readNbt(tag: NbtCompound) {
        super.readNbt(tag)
        inventory.readNbt(tag)
        transferCooldown = tag.getInt("TransferCooldown")
        if (tag.contains("CustomName", 8)) {
            customName = Text.Serializer.fromJson(tag.getString("CustomName"))
        }
    }

    override fun markDirty() {
        super.markDirty()
        inventory.markDirty()
    }

    companion object {
        val type = FabricBlockEntityTypeBuilder.create(::DuctBlockEntity, Ducts.DUCT_BLOCK).build(null)!!
        @JvmStatic lateinit var instance: DuctBlockEntity
    }
}
