package dialight.random.gui

import dialight.extensions.ItemStackBuilderEx
import dialight.guilib.View
import dialight.guilib.events.ItemClickEvent
import dialight.guilib.simple.SimpleItem
import dialight.modulelib.ModuleMessages
import dialight.random.RandomPlugin
import jekarus.colorizer.Text_colorized
import jekarus.colorizer.Text_colorizedList
import org.spongepowered.api.item.ItemTypes
import org.spongepowered.api.item.inventory.ItemStackSnapshot

class RandomItem(val plugin: RandomPlugin) : View.Item {

    override val item get() = ItemStackBuilderEx(ItemTypes.ENDER_EYE)
        .name(Text_colorized("|y|${plugin.moduule.name}"))
        .lore(
            Text_colorizedList(
                "|g|ЛКМ|y|: ${if(!plugin.moduule.enabled) "Включить" else "Выключить"} модуль",
                "",
                "|g|Версия: |y|v" + plugin.container.version.orElse("null")
            )
        )
        .build()

    override fun onClick(event: ItemClickEvent) {
        when(event.type) {
            ItemClickEvent.Type.LEFT -> {
                val newState = !plugin.moduule.enabled
                if(!plugin.moduule.toggle()) {
                    if(newState) {
                        event.player.sendMessage(ModuleMessages.cantEnable(plugin.moduule))
                    } else {
                        event.player.sendMessage(ModuleMessages.cantDisable(plugin.moduule))
                    }
                } else {
                    if(newState) {
                        event.player.sendMessage(ModuleMessages.successEnable(plugin.moduule))
                    } else {
                        event.player.sendMessage(ModuleMessages.successDisable(plugin.moduule))
                    }
                }
            }
        }
    }

}
