package br.com.brforgers.mods.ducts.config

import blue.endless.jankson.Comment;
import br.com.brforgers.mods.ducts.Ducts
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry

// Configuration file definition.
@SuppressWarnings("unused")
@Config(name = Ducts.MOD_ID)
class DuctsConfig : ConfigData {
    @Comment("Allow redstone signal to disable Ducts?")
    @ConfigEntry.Gui.Tooltip
    public var redstoneLocksDucts : Boolean = false

    @Comment("Transfer cooldown for target Duct (true for stable sorters)?")
    @ConfigEntry.Gui.Tooltip
    public var targetCooldownEnabled : Boolean = true

    @Comment("Transfer cooldown in game ticks (8+ for stable sorters)?")
    @ConfigEntry.Gui.Tooltip
    public var maxCooldown : Int = 8

}