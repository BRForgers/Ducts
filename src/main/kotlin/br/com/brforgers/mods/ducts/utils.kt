package br.com.brforgers.mods.ducts

import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.collection.DefaultedList

fun Inventory.writeNbt(tag: NbtCompound): NbtCompound {
    val items = (0 until size()).map { i -> getStack(i) }.toTypedArray()
    return Inventories.writeNbt(tag, DefaultedList.copyOf(ItemStack.EMPTY, *items))
}

fun Inventory.readNbt(tag: NbtCompound) {
    val savedContent = DefaultedList.ofSize(size(), ItemStack.EMPTY)
    Inventories.readNbt(tag, savedContent)
    savedContent.forEachIndexed(this::setStack)
}

