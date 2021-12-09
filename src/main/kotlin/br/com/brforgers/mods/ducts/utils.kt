package br.com.brforgers.mods.ducts

import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.item.ItemStack

fun Container.writeNbt(tag: CompoundTag): CompoundTag {
    val items = (0 until containerSize).map { i -> getItem(i) }.toTypedArray()
    return ContainerHelper.saveAllItems(tag, NonNullList.of(ItemStack.EMPTY, *items))
}

fun Container.readNbt(tag: CompoundTag) {
    val savedContent = NonNullList.withSize(containerSize, ItemStack.EMPTY)
    ContainerHelper.loadAllItems(tag, savedContent)
    savedContent.forEachIndexed(this::setItem)
}

