package br.com.brforgers.mods.hoppersplus.screens

import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text

class DuctScreen(gui: DuctGuiDescription?, player: PlayerEntity?, title: Text?) :
    CottonInventoryScreen<DuctGuiDescription?>(gui, player, title)