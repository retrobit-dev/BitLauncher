package dev.retrobit.bitlauncher.util

import dev.retrobit.bitlauncher.data.LaunchableApp

sealed interface DrawerItem {
    data class LetterHeader(val letter: Char) : DrawerItem
    data class RowItem(val apps: List<LaunchableApp>) : DrawerItem
}

/**
 * Flattens apps into alphabetical sections. In grid mode each section becomes rows of
 * [columns] cells; in list mode every app is its own row.
 */
fun buildDrawerItems(
    apps: List<LaunchableApp>,
    grid: Boolean,
    columns: Int,
): List<DrawerItem> {
    if (apps.isEmpty()) return emptyList()
    val grouped = LinkedHashMap<Char, MutableList<LaunchableApp>>()
    apps.forEach { app ->
        grouped.getOrPut(app.sectionLetter) { mutableListOf() }.add(app)
    }
    val items = ArrayList<DrawerItem>(apps.size + grouped.size)
    grouped.toSortedMap().forEach { (letter, sectionApps) ->
        items += DrawerItem.LetterHeader(letter)
        if (grid) {
            sectionApps.chunked(columns.coerceAtLeast(1)).forEach { chunk ->
                items += DrawerItem.RowItem(chunk)
            }
        } else {
            sectionApps.forEach { app -> items += DrawerItem.RowItem(listOf(app)) }
        }
    }
    return items
}
