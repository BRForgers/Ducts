package br.com.brforgers.mods.ducts

import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.collection.DefaultedList

fun Inventory.toTag(tag: CompoundTag): CompoundTag {
    val items = (0 until size()).map { i -> getStack(i) }.toTypedArray()
    return Inventories.toTag(tag, DefaultedList.copyOf(ItemStack.EMPTY, *items))
}

fun Inventory.fromTag(tag: CompoundTag) {
    val savedContent = DefaultedList.ofSize(size(), ItemStack.EMPTY)
    Inventories.fromTag(tag, savedContent)
    savedContent.forEachIndexed(this::setStack)
}

