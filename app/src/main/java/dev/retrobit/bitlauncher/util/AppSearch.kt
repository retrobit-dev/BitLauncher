package dev.retrobit.bitlauncher.util

import dev.retrobit.bitlauncher.data.LaunchableApp

/**
 * Small ranked search tailored for app labels:
 *   prefix match > substring match > initials match > subsequence match.
 */
object AppSearch {

    fun filter(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return apps
        return apps.mapNotNull { app ->
            val score = score(app.sortKey, normalized) ?: return@mapNotNull null
            app to score
        }
            .sortedWith(compareByDescending<Pair<LaunchableApp, Int>> { it.second }.thenBy { it.first.sortKey })
            .map { it.first }
    }

    private fun score(label: String, query: String): Int? {
        if (label.startsWith(query)) return 1000 - label.length
        val index = label.indexOf(query)
        if (index > 0) return 700 - index.coerceAtMost(100) - label.length / 4
        val initials = label.split(' ', '-', '.', '_')
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
        if (initials.startsWith(query)) return 500 - label.length / 4
        if (query.length >= 2) {
            var queryIndex = 0
            for (char in label) {
                if (char == query[queryIndex]) {
                    queryIndex++
                    if (queryIndex == query.length) return 250 - label.length / 4
                }
            }
        }
        return null
    }
}
