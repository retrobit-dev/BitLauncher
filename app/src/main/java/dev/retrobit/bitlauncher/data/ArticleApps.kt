package dev.retrobit.bitlauncher.data

/** Legacy automatic selection: favorites first, otherwise the first three visible apps. */
fun automaticArticleApps(
    visibleApps: List<LaunchableApp>,
    favorites: List<LaunchableApp>,
): List<LaunchableApp> =
    (if (favorites.isNotEmpty()) favorites else visibleApps)
        .distinctBy { it.packageName }
        .take(3)

/**
 * Resolves the only apps allowed to contribute shortcuts and notification counts to the article.
 * A null selection keeps the legacy automatic mode; an empty selection intentionally selects none.
 * Custom selections may intentionally include an app hidden from the drawer.
 */
fun resolveArticleApps(
    visibleApps: List<LaunchableApp>,
    allApps: List<LaunchableApp>,
    favorites: List<LaunchableApp>,
    selectedKeys: List<String>?,
): List<LaunchableApp> {
    if (selectedKeys == null) return automaticArticleApps(visibleApps, favorites)
    val byKey = allApps.associateBy { it.key }
    return selectedKeys.mapNotNull(byKey::get).distinctBy { it.packageName }
}
