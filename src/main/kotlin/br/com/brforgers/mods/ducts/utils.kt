package br.com.brforgers.mods.ducts

import net.minecraft.inventory.IInventory
import net.minecraft.inventory.ItemStackHelper
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.NonNullList

fun IInventory.writeNbt(tag: CompoundNBT): CompoundNBT {
    val items = (0 until containerSize).map { i -> getItem(i) }.toTypedArray()
    return ItemStackHelper.saveAllItems(tag, NonNullList.of(ItemStack.EMPTY, *items))
}

fun IInventory.readNbt(tag: CompoundNBT) {
    val savedContent = NonNullList.withSize(containerSize, ItemStack.EMPTY)
    ItemStackHelper.loadAllItems(tag, savedContent)
    savedContent.forEachIndexed(this::setItem)
}

