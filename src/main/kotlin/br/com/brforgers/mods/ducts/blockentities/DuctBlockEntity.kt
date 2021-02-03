package br.com.brforgers.mods.ducts.blockentities

import br.com.brforgers.mods.ducts.Ducts
import br.com.brforgers.mods.ducts.blocks.DuctBlock
import br.com.brforgers.mods.ducts.fromTag
import br.com.brforgers.mods.ducts.screens.DuctGuiDescription
import br.com.brforgers.mods.ducts.toTag
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.HopperBlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Tickable
import java.util.function.Supplier
import alexiil.mc.lib.attributes.SearchOptions

import alexiil.mc.lib.attributes.item.ItemAttributes

import alexiil.mc.lib.attributes.item.ItemInsertable
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable
import net.minecraft.util.ActionResult

import alexiil.mc.lib.attributes.item.ItemInvUtil

import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper

import alexiil.mc.lib.attributes.item.ItemExtractable





class DuctBlockEntity(
        private val inventory: Inventory = SimpleInventory(1))
    : BlockEntity(type), Inventory by inventory,
        ExtendedScreenHandlerFactory,
        Tickable {

    private var maxCooldown = 8
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

    private fun attemptInsert(): Boolean {
        val world = world ?: return false

        val stack = getStack(0)
        if (stack.isEmpty) return false

        if (cachedState[DuctBlock.Props.powered]/*&& stack.count <= 1*/) return false

        val outputDir = cachedState[DuctBlock.Props.output]
        val outputInv = HopperBlockEntity.getInventoryAt(world, pos.offset(outputDir))

        if(outputInv != null){
            val stackCopy = this.getStack(0).copy()
            val ret = HopperBlockEntity.transfer(this, outputInv, this.removeStack(0, 1), outputDir.opposite)
            if (ret.isEmpty) {
                if (outputInv is DuctBlockEntity) {
                    (outputInv as DuctBlockEntity).transferCooldown = maxCooldown
                }
                outputInv.markDirty()
                return true
            }
            this.setStack(0, stackCopy)
        } else {
            val insertable = ItemAttributes.INSERTABLE[world, pos.offset(outputDir), SearchOptions.inDirection(outputDir)]
            if (insertable == RejectingItemInsertable.NULL) {
                return false
            }
            val extractable = FixedInventoryVanillaWrapper(this).extractable

            return ItemInvUtil.move(extractable, insertable, 1) > 0
        }
        return false
    }

    override fun tick() {
        val world = world
        if (world == null || world.isClient) return

        transferCooldown--

        if (transferCooldown > 0) return

        transferCooldown = 0

        if (attemptInsert()) {
            transferCooldown = maxCooldown
            markDirty()
        }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)
        inventory.toTag(tag)
        tag.putInt("TransferCooldown", transferCooldown)
        return tag
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        inventory.fromTag(tag)
        transferCooldown = tag.getInt("TransferCooldown")
    }

    override fun markDirty() {
        super.markDirty()
        inventory.markDirty()
    }

    companion object {
        val type = BlockEntityType.Builder.create(Supplier { DuctBlockEntity() }, Ducts.DUCT_BLOCK).build(null)!!
    }
}
