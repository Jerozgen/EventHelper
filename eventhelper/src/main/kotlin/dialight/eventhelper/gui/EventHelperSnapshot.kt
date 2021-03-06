package dialight.eventhelper.gui

import dialight.eventhelper.EventHelperPlugin
import dialight.extensions.ItemStackBuilderEx
import dialight.extensions.closeInventoryLater
import dialight.guilib.View
import dialight.guilib.events.ItemClickEvent
import dialight.guilib.simple.SimpleItem
import dialight.guilib.snapshot.Snapshot
import dialight.modulelib.Module
import dialight.modulelib.ModuleMessages
import dialight.toollib.Tool
import jekarus.colorizer.Text_colorized
import jekarus.colorizer.Text_colorizedList
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.type.DyeColors
import org.spongepowered.api.data.type.SkullTypes
import org.spongepowered.api.item.ItemTypes
import org.spongepowered.api.item.inventory.property.Identifiable
import org.spongepowered.api.profile.GameProfile
import org.spongepowered.api.profile.property.ProfileProperty
import org.spongepowered.api.text.Text
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import kotlin.streams.toList
import java.util.UUID



class EventHelperSnapshot(val plugin: EventHelperPlugin, id: Identifiable) : Snapshot<Snapshot.Page>(plugin.guilib, id) {


    fun createDefault(k: String, v: Tool): View.Item {
        return SimpleItem(
            ItemStackBuilderEx(v.type)
                .name(v.title)
                .lore(
                    Text_colorizedList(
                        "|g|ЛКМ|y|: Получить инструмент"
//                    "",
//                    "|g|Версия: |y|v" + plugin.container.version.orElse("null")
                    )
                )
                .build()
        ) {
            when(it.type) {
                ItemClickEvent.Type.LEFT -> {
                    it.player.closeInventoryLater(plugin)
                    plugin.toollib.giveTool(it.player, v.id)
                }
            }
        }
    }

    class ModuleItem(val mod: Module): View.Item {

        override val item get() = ItemStackBuilderEx(ItemTypes.ANVIL)
            .name(Text_colorized("|y|${mod.name}"))
            .lore(
                Text_colorizedList(
                    "|g|ЛКМ|y|: ${if(mod.enabled) "Вкл" else "Выкл"} модуль"
//                    "",
//                    "|g|Версия: |y|v" + plugin.container.version.orElse("null")
                )
            )
            .build()

        override fun onClick(event: ItemClickEvent) {
            when(event.type) {
                ItemClickEvent.Type.LEFT -> {
                    val newState = !mod.enabled
                    if(!mod.toggle()) {
                        if(newState) {
                            event.player.sendMessage(ModuleMessages.cantEnable(mod))
                        } else {
                            event.player.sendMessage(ModuleMessages.cantDisable(mod))
                        }
                    } else {
                        if(newState) {
                            event.player.sendMessage(ModuleMessages.successEnable(mod))
                        } else {
                            event.player.sendMessage(ModuleMessages.successDisable(mod))
                        }
                    }
                }
            }
        }
    }

    fun updateItems() {
        pages.clear()

        val maxLines = 6
        val maxColumns = 9
        var index = 0

        // tools
        val toolItems = plugin.toollib.toolregistry.entries.stream()
            .sorted { o1, o2 -> o1.key.compareTo(o2.key) }
            .map { (id, tool) -> plugin.toolItemRegistry[id] ?: createDefault(id, tool) }
            .toList()
        val rawToolPages = EventHelperPageBuilder(maxColumns - 2, maxLines, toolItems).toList()

        // modules
        val moduleItems = plugin.modulelib.moduleregistry.entries.stream()
            .sorted { o1, o2 -> o1.key.compareTo(o2.key) }
            .map { (id, module) -> plugin.moduleItemRegistry[id] ?: ModuleItem(module) }
            .toList()
        val rawModulePages = EventHelperPageBuilder(maxColumns - 2, maxLines, moduleItems).toList()

        val total = rawToolPages.size + rawModulePages.size

        rawToolPages.forEach { slotCache ->
            val gui = ToolsPage(this, Text_colorized("EventHelper(${index + 1}/$total): Tools"), maxColumns, maxLines, index, total)
            slotCache.forEach { pos, slot ->
                gui[(pos.x + 1) + pos.y * maxColumns] = slot
            }
            pages.add(gui)
            index++
        }
        rawModulePages.forEach { slotCache ->
            val gui = ModulesPage(this, Text_colorized("EventHelper(${index + 1}/$total): Modules"), maxColumns, maxLines, index, total)
            slotCache.forEach { pos, slot ->
                gui[(pos.x + 1) + pos.y * maxColumns] = slot
            }
            pages.add(gui)
            index++
        }

        if(pages.isEmpty()) pages.add(Page(this, Text_colorized("EventHelper: tools or modules not found"), maxColumns, maxLines, 0))

    }


    class ToolsPage(
        snap: EventHelperSnapshot,
        title: Text,
        width: Int,
        height: Int,
        index: Int,
        total: Int
    ) : Snapshot.Page(
        snap, title, width, height, index
    ) {

        init {
            val description = SimpleItem(ItemStackBuilderEx(ItemTypes.STAINED_GLASS_PANE)
                .also {
                    offer(Keys.DYE_COLOR, DyeColors.LIGHT_BLUE)
                }
                .name(Text_colorized("Инструменты"))
                .build()) {
            }
            val backwardItem = SimpleItem(ItemStackBuilderEx(ItemTypes.SKULL)
                .builder {
                    add(Keys.SKULL_TYPE, SkullTypes.PLAYER)
                    val profile = GameProfile.of(UUID.randomUUID(), "Head")
                    profile.propertyMap.put("textures", ProfileProperty.of("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODQxZGQxMjc1OTVhMjVjMjQzOWM1ZGIzMWNjYjQ5MTQ1MDdhZTE2NDkyMWFhZmVjMmI5NzlhYWQxY2ZlNyJ9fX0="))
                    add(Keys.REPRESENTED_PLAYER, profile)
                }
                .name(Text_colorized("Инструменты"))
                .lore(Text_colorizedList(
                    "|g|ЛКМ|y|: Перейти на предыдущую страницу"
                ))
                .build()) {
                when(it.type) {
                    ItemClickEvent.Type.LEFT -> {
                        snap.backward(it.player)
                    }
                }
            }
            val forwardItem = SimpleItem(ItemStackBuilderEx(ItemTypes.SKULL)
                .builder {
                    add(Keys.SKULL_TYPE, SkullTypes.PLAYER)
                    val profile = GameProfile.of(UUID.randomUUID(), "Head")
                    profile.propertyMap.put("textures", ProfileProperty.of("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDJkMDMxM2I2NjgwMTQxMjg2Mzk2ZTcxYzM2MWU1OTYyYTM5YmFmNTk2ZDdlNTQ3NzE3NzVkNWZhM2QifX19"))
                    add(Keys.REPRESENTED_PLAYER, profile)
                }
                .name(Text_colorized("Инструменты"))
                .lore(Text_colorizedList(
                    "|g|ЛКМ|y|: Перейти на следующую страницу"
                ))
                .build()) {
                when(it.type) {
                    ItemClickEvent.Type.LEFT -> {
                        snap.forward(it.player)
                    }
                }
            }
            val middle = height / 2
            for(y in (0..(height - 1))) {
                this[y * width] = if(index != 0 && (y == middle - 1 || y == middle)) {
                    backwardItem
                } else {
                    description
                }
                this[(width - 1) + y * width] = if(index + 1 != total && (y == middle - 1 || y == middle)) {
                    forwardItem
                } else {
                    description
                }
            }
        }

    }

    class ModulesPage(
        snap: EventHelperSnapshot,
        title: Text,
        width: Int,
        height: Int,
        index: Int,
        total: Int
    ) : Snapshot.Page(
        snap, title, width, height, index
    ) {

        init {
            val description = SimpleItem(ItemStackBuilderEx(ItemTypes.STAINED_GLASS_PANE)
                .also {
                    offer(Keys.DYE_COLOR, DyeColors.GREEN)
                }
                .name(Text_colorized("Модули"))
                .build())
            val backwardItem = SimpleItem(ItemStackBuilderEx(ItemTypes.SKULL)
                .builder {
                    add(Keys.SKULL_TYPE, SkullTypes.PLAYER)
                    val profile = GameProfile.of(UUID.randomUUID(), "Head")
                    profile.propertyMap.put("textures", ProfileProperty.of("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODQxZGQxMjc1OTVhMjVjMjQzOWM1ZGIzMWNjYjQ5MTQ1MDdhZTE2NDkyMWFhZmVjMmI5NzlhYWQxY2ZlNyJ9fX0="))
                    add(Keys.REPRESENTED_PLAYER, profile)
                }
                .name(Text_colorized("Инструменты"))
                .lore(Text_colorizedList(
                    "|g|ЛКМ|y|: Перейти на предыдущую страницу"
                ))
                .build()) {
                when(it.type) {
                    ItemClickEvent.Type.LEFT -> {
                        snap.backward(it.player)
                    }
                }
            }
            val forwardItem = SimpleItem(ItemStackBuilderEx(ItemTypes.SKULL)
                .builder {
                    add(Keys.SKULL_TYPE, SkullTypes.PLAYER)
                    val profile = GameProfile.of(UUID.randomUUID(), "Head")
                    profile.propertyMap.put("textures", ProfileProperty.of("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDJkMDMxM2I2NjgwMTQxMjg2Mzk2ZTcxYzM2MWU1OTYyYTM5YmFmNTk2ZDdlNTQ3NzE3NzVkNWZhM2QifX19"))
                    add(Keys.REPRESENTED_PLAYER, profile)
                }
                .name(Text_colorized("Инструменты"))
                .lore(Text_colorizedList(
                    "|g|ЛКМ|y|: Перейти на следующую страницу"
                ))
                .build()) {
                when(it.type) {
                    ItemClickEvent.Type.LEFT -> {
                        snap.forward(it.player)
                    }
                }
            }
            val middle = height / 2
            for(y in (0..(height - 1))) {
                this[y * width] = if(index != 0 && (y == middle - 1 || y == middle)) {
                    backwardItem
                } else {
                    description
                }
                this[(width - 1) + y * width] = if(index + 1 != total && (y == middle - 1 || y == middle)) {
                    forwardItem
                } else {
                    description
                }
            }
        }

    }

}