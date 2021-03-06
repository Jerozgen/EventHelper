package dialight.teams.gui

import dialight.extensions.ItemStackBuilderEx
import dialight.extensions.Server_getUser
import dialight.extensions.dyeColor
import dialight.guilib.View
import dialight.guilib.events.ItemClickEvent
import dialight.guilib.simple.SimpleItem
import dialight.guilib.simple.TextInputGui
import dialight.guilib.snapshot.SequentialPageBuilder
import dialight.guilib.snapshot.Snapshot
import dialight.teams.Server_getScoreboard
import dialight.teams.TeamsMessages
import dialight.teams.TeamsPlugin
import dialight.teleporter.Teleporter
import dialight.teleporter.TeleporterTool
import jekarus.colorizer.Text_colorized
import jekarus.colorizer.Text_colorizedList
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.item.ItemTypes
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.item.inventory.property.Identifiable
import org.spongepowered.api.scoreboard.Team
import org.spongepowered.api.text.Text
import java.util.*
import java.util.stream.Collectors

class TeamsSnapshot(val plugin: TeamsPlugin, val uuid: UUID, id: Identifiable) : Snapshot<TeamsSnapshot.Page>(plugin.guilib!!, id) {

    val addTeamItem = SimpleItem(ItemStackBuilderEx(ItemTypes.NETHER_STAR)
        .name(Text_colorized("Добавить команду"))
        .lore(Text_colorizedList(
            "|g|ЛКМ|y|: добавить команду"
        ))
        .build()) {
        when(it.type) {
            ItemClickEvent.Type.LEFT -> {
                plugin.guilib!!.openGui(it.player, AddTeamGui(plugin))
            }
        }
    }

    init {
        updateItems()
    }

    fun updateItems() {
        val items = arrayListOf<View.Item>()
        val scoreboard = Server_getScoreboard()
        for(team in scoreboard.teams) {
            items += Item(team)
        }
        items.add(addTeamItem)

        pages.clear()
        val maxLines = 6
        val maxColumns = 9
        val pagesitems = SequentialPageBuilder(maxColumns, maxLines - 1, maxColumns, items).toList()
        var index = 0
        for(pageitems in pagesitems) {
            val page = Page(this, Text_colorized("Команды ${index + 1}/${pagesitems.size}"), maxColumns, maxLines, index, pagesitems.size)
            pageitems.forEach { slotIndex, slot ->
                page[slotIndex] = slot
            }
            pages += page
            index++
        }
    }

    class Page(
        snap: TeamsSnapshot,
        title: Text,
        width: Int,
        height: Int,
        index: Int,
        total: Int
    ): Snapshot.Page(snap, title, width, height, index) {

    }

    inner class Item(val team: Team) : View.Item {

        override val item: ItemStackSnapshot
            get() {
                val selected = plugin.selected[uuid] == team
                return ItemStackBuilderEx(if(selected) ItemTypes.LEATHER_HELMET else ItemTypes.LEATHER_CHESTPLATE)
                    .name(Text_colorized(team.name))
                    .also {
                        offer(Keys.COLOR, team.color.color)
//                            offer(Keys.DYE_COLOR, team.color.dyeColor)
                    }
                    .lore(Text_colorizedList(
                        "|g|ЛКМ|y|: выбрать текущую команду",
                        "|y| и игроков в телепортере",
                        "|g|СКМ|y|: удалить команду"
                    ))
                    .build()
            }

        override fun onClick(event: ItemClickEvent) {
            val users = team.members.stream()
                .map { Server_getUser(it.toPlain()) }
                .filter { it != null }
                .map { it!! }
                .collect(Collectors.toList())
            when(event.type) {
                ItemClickEvent.Type.LEFT -> {
                    plugin.teleporter?.let { teleporter ->
                        val selected = plugin.selected.remove(event.player.uniqueId)
                        teleporter.teleporter.invoke(event.player, Teleporter.Action.UNTAG, Teleporter.Group.ALL)
                        teleporter.teleporter.invoke(event.player, Teleporter.Action.TAG, users)
                        if(selected == null || selected.name != team.name) {
                            plugin.selected[event.player.uniqueId] = team
                            event.player.sendMessage(TeamsMessages.selectedTeam(team))
                        } else {
                            event.player.sendMessage(TeamsMessages.unselectedTeam(selected))
                        }
                    }
                }
                ItemClickEvent.Type.MIDDLE -> {
                    if(team.unregister()) {
                        event.player.sendMessage(TeamsMessages.removeTeam(team))
                    }
                }
            }
        }

    }


}