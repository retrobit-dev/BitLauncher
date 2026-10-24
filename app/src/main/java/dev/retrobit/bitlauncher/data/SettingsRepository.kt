package dev.retrobit.bitlauncher.data

import android.content.Context
import android.text.format.DateFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED, KALEIDO, KALEIDO_NIGHT }

/**
 * Visual identity of the launcher.
 * MINIMAL is the original Niagara/Minima inspired look, FOLIO turns the home screen into a
 * newspaper front page printed on e-paper (Kaleido palette).
 */
enum class LauncherStyle { MINIMAL, FOLIO }

/** Actions that can be assigned to home gestures without changing the launcher layout. */
enum class GestureAction {
    NONE,
    OPEN_DRAWER,
    OPEN_SEARCH,
    OPEN_SETTINGS,
    OPEN_HOME_MENU,
    OPEN_NOTIFICATIONS,
    OPEN_QUICK_SETTINGS,
    OPEN_RECENTS,
    LOCK_SCREEN,
    TAKE_SCREENSHOT,
    OPEN_CALENDAR,
    OPEN_NOTES,
}

enum class LauncherFont { STYLE_DEFAULT, LORA, ROBOTO, TYPEWRITER, PIXEL, OPERATOR }

enum class AppLayout { LIST, GRID }

data class LauncherSettings(
    val style: LauncherStyle = LauncherStyle.FOLIO,
    val themeMode: ThemeMode = ThemeMode.KALEIDO,
    val dynamicColor: Boolean = true,
    val fontChoice: LauncherFont = LauncherFont.STYLE_DEFAULT,
    val iconPack: String = IconPackSelection.SYSTEM,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val use24Hour: Boolean = false,
    val layout: AppLayout = AppLayout.LIST,
    val gridColumns: Int = 4,
    val autoFocusSearch: Boolean = false,
    val showAlphabetRail: Boolean = true,
    val haptics: Boolean = true,
    val compactRows: Boolean = false,
    val newsLocation: String = "Rembang",
    val showNewsArticle: Boolean = true,
    val showNewsAppLinks: Boolean = true,
    val showNewsUnread: Boolean = true,
    val showNewsPhoto: Boolean = true,
    val newsImageUri: String = "",
    val showNewsWidgets: Boolean = true,
    val notesPackage: String = "",
    val showNotificationBadges: Boolean = true,
    val doubleTapAction: GestureAction = GestureAction.OPEN_SEARCH,
    val longPressAction: GestureAction = GestureAction.OPEN_HOME_MENU,
    val swipeLeftAction: GestureAction = GestureAction.OPEN_SETTINGS,
    val swipeRightAction: GestureAction = GestureAction.OPEN_NOTIFICATIONS,
    val swipeUpAction: GestureAction = GestureAction.OPEN_DRAWER,
    val twoFingerSwipeUpAction: GestureAction = GestureAction.NONE,
    val twoFingerSwipeDownAction: GestureAction = GestureAction.NONE,
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
)

/**
 * Tiny SharedPreferences-backed store. Launchers must start instantly, so we deliberately avoid
 * DataStore/Room startup cost for a handful of preferences.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val systemUses24Hour = DateFormat.is24HourFormat(context)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private fun load(): LauncherSettings {
        val theme = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.KALEIDO.name)!!)
        }.getOrDefault(ThemeMode.KALEIDO)
        val style = runCatching {
            LauncherStyle.valueOf(prefs.getString(KEY_STYLE, LauncherStyle.FOLIO.name)!!)
        }.getOrDefault(LauncherStyle.FOLIO)
        val layout = runCatching {
            AppLayout.valueOf(prefs.getString(KEY_LAYOUT, AppLayout.LIST.name)!!)
        }.getOrDefault(AppLayout.LIST)
        val favorites = prefs.getString(KEY_FAVORITES, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        fun gesture(key: String, default: GestureAction) = runCatching {
            GestureAction.valueOf(prefs.getString(key, default.name)!!)
        }.getOrDefault(default)
        return LauncherSettings(
            style = style,
            themeMode = theme,
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            fontChoice = runCatching {
                LauncherFont.valueOf(prefs.getString(KEY_FONT, LauncherFont.STYLE_DEFAULT.name)!!)
            }.getOrDefault(LauncherFont.STYLE_DEFAULT),
            iconPack = prefs.getString(KEY_ICON_PACK, IconPackSelection.SYSTEM)
                ?: IconPackSelection.SYSTEM,
            showClock = prefs.getBoolean(KEY_SHOW_CLOCK, true),
            showDate = prefs.getBoolean(KEY_SHOW_DATE, true),
            use24Hour = prefs.getBoolean(KEY_24H, systemUses24Hour),
            layout = layout,
            gridColumns = prefs.getInt(KEY_COLUMNS, 4).coerceIn(MIN_COLUMNS, MAX_COLUMNS),
            autoFocusSearch = prefs.getBoolean(KEY_AUTOFOCUS, false),
            showAlphabetRail = prefs.getBoolean(KEY_ALPHABET_RAIL, true),
            haptics = prefs.getBoolean(KEY_HAPTICS, true),
            compactRows = prefs.getBoolean(KEY_COMPACT, false),
            newsLocation = prefs.getString(KEY_NEWS_LOCATION, "Rembang") ?: "Rembang",
            showNewsArticle = prefs.getBoolean(KEY_NEWS_ARTICLE, true),
            showNewsAppLinks = prefs.getBoolean(KEY_NEWS_APP_LINKS, true),
            showNewsUnread = prefs.getBoolean(KEY_NEWS_UNREAD, true),
            showNewsPhoto = prefs.getBoolean(KEY_NEWS_PHOTO, true),
            newsImageUri = prefs.getString(KEY_NEWS_IMAGE, "") ?: "",
            showNewsWidgets = prefs.getBoolean(KEY_NEWS_WIDGETS, true),
            notesPackage = prefs.getString(KEY_NOTES_PACKAGE, "") ?: "",
            showNotificationBadges = prefs.getBoolean(KEY_NOTIFICATION_BADGES, true),
            doubleTapAction = gesture(KEY_DOUBLE_TAP, GestureAction.OPEN_SEARCH),
            longPressAction = gesture(KEY_LONG_PRESS, GestureAction.OPEN_HOME_MENU),
            swipeLeftAction = gesture(KEY_SWIPE_LEFT, GestureAction.OPEN_SETTINGS),
            swipeRightAction = gesture(KEY_SWIPE_RIGHT, GestureAction.OPEN_NOTIFICATIONS),
            swipeUpAction = gesture(KEY_SWIPE_UP, GestureAction.OPEN_DRAWER),
            twoFingerSwipeUpAction = gesture(KEY_TWO_FINGER_UP, GestureAction.NONE),
            twoFingerSwipeDownAction = gesture(KEY_TWO_FINGER_DOWN, GestureAction.NONE),
            favorites = favorites,
            hidden = prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet(),
        )
    }

    private fun persist(settings: LauncherSettings) {
        prefs.edit()
            .putString(KEY_STYLE, settings.style.name)
            .putString(KEY_THEME, settings.themeMode.name)
            .putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
            .putString(KEY_FONT, settings.fontChoice.name)
            .putString(KEY_ICON_PACK, settings.iconPack)
            .putBoolean(KEY_SHOW_CLOCK, settings.showClock)
            .putBoolean(KEY_SHOW_DATE, settings.showDate)
            .putBoolean(KEY_24H, settings.use24Hour)
            .putString(KEY_LAYOUT, settings.layout.name)
            .putInt(KEY_COLUMNS, settings.gridColumns)
            .putBoolean(KEY_AUTOFOCUS, settings.autoFocusSearch)
            .putBoolean(KEY_ALPHABET_RAIL, settings.showAlphabetRail)
            .putBoolean(KEY_HAPTICS, settings.haptics)
            .putBoolean(KEY_COMPACT, settings.compactRows)
            .putString(KEY_NEWS_LOCATION, settings.newsLocation)
            .putBoolean(KEY_NEWS_ARTICLE, settings.showNewsArticle)
            .putBoolean(KEY_NEWS_APP_LINKS, settings.showNewsAppLinks)
            .putBoolean(KEY_NEWS_UNREAD, settings.showNewsUnread)
            .putBoolean(KEY_NEWS_PHOTO, settings.showNewsPhoto)
            .putString(KEY_NEWS_IMAGE, settings.newsImageUri)
            .putBoolean(KEY_NEWS_WIDGETS, settings.showNewsWidgets)
            .putString(KEY_NOTES_PACKAGE, settings.notesPackage)
            .putBoolean(KEY_NOTIFICATION_BADGES, settings.showNotificationBadges)
            .putString(KEY_DOUBLE_TAP, settings.doubleTapAction.name)
            .putString(KEY_LONG_PRESS, settings.longPressAction.name)
            .putString(KEY_SWIPE_LEFT, settings.swipeLeftAction.name)
            .putString(KEY_SWIPE_RIGHT, settings.swipeRightAction.name)
            .putString(KEY_SWIPE_UP, settings.swipeUpAction.name)
            .putString(KEY_TWO_FINGER_UP, settings.twoFingerSwipeUpAction.name)
            .putString(KEY_TWO_FINGER_DOWN, settings.twoFingerSwipeDownAction.name)
            .putString(KEY_FAVORITES, settings.favorites.joinToString(SEPARATOR))
            .putStringSet(KEY_HIDDEN, settings.hidden)
            .apply()
    }

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        persist(updated)
    }

    fun toggleFavorite(key: String) = update { settings ->
        if (key in settings.favorites) {
            settings.copy(favorites = settings.favorites - key)
        } else {
            settings.copy(favorites = settings.favorites + key)
        }
    }

    fun setHidden(key: String, hidden: Boolean) = update { settings ->
        if (hidden) {
            settings.copy(hidden = settings.hidden + key, favorites = settings.favorites - key)
        } else {
            settings.copy(hidden = settings.hidden - key)
        }
    }

    // ---------------------------------------------------------------- backup

    fun exportJson(): String {
        val settings = _settings.value
        val root = JSONObject()
        root.put("schema", SCHEMA)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("style", settings.style.name)
        root.put("themeMode", settings.themeMode.name)
        root.put("dynamicColor", settings.dynamicColor)
        root.put("fontChoice", settings.fontChoice.name)
        root.put("iconPack", settings.iconPack)
        root.put("showClock", settings.showClock)
        root.put("showDate", settings.showDate)
        root.put("use24Hour", settings.use24Hour)
        root.put("layout", settings.layout.name)
        root.put("gridColumns", settings.gridColumns)
        root.put("autoFocusSearch", settings.autoFocusSearch)
        root.put("showAlphabetRail", settings.showAlphabetRail)
        root.put("haptics", settings.haptics)
        root.put("compactRows", settings.compactRows)
        root.put("newsLocation", settings.newsLocation)
        root.put("showNewsArticle", settings.showNewsArticle)
        root.put("showNewsAppLinks", settings.showNewsAppLinks)
        root.put("showNewsUnread", settings.showNewsUnread)
        root.put("showNewsPhoto", settings.showNewsPhoto)
        root.put("newsImageUri", settings.newsImageUri)
        root.put("showNewsWidgets", settings.showNewsWidgets)
        root.put("notesPackage", settings.notesPackage)
        root.put("showNotificationBadges", settings.showNotificationBadges)
        root.put("doubleTapAction", settings.doubleTapAction.name)
        root.put("longPressAction", settings.longPressAction.name)
        root.put("swipeLeftAction", settings.swipeLeftAction.name)
        root.put("swipeRightAction", settings.swipeRightAction.name)
        root.put("swipeUpAction", settings.swipeUpAction.name)
        root.put("twoFingerSwipeUpAction", settings.twoFingerSwipeUpAction.name)
        root.put("twoFingerSwipeDownAction", settings.twoFingerSwipeDownAction.name)
        root.put("favorites", JSONArray(settings.favorites))
        root.put("hidden", JSONArray(settings.hidden.toList()))
        return root.toString(2)
    }

    /** @return true when the file was a valid BitLauncher backup. */
    fun importJson(raw: String): Boolean = runCatching {
        val root = JSONObject(raw)
        if (root.optInt("schema", 0) != SCHEMA) return false
        val current = _settings.value
        val favorites = root.optJSONArray("favorites")?.let { array ->
            (0 until array.length()).mapNotNull { array.optString(it, null) }
        } ?: current.favorites
        val hidden = root.optJSONArray("hidden")?.let { array ->
            (0 until array.length()).mapNotNull { array.optString(it, null) }.toSet()
        } ?: current.hidden
        fun restoredGesture(key: String, default: GestureAction) = runCatching {
            GestureAction.valueOf(root.optString(key, default.name))
        }.getOrDefault(default)
        val restored = current.copy(
            style = runCatching {
                LauncherStyle.valueOf(root.optString("style", current.style.name))
            }.getOrDefault(current.style),
            themeMode = runCatching {
                ThemeMode.valueOf(root.optString("themeMode", current.themeMode.name))
            }.getOrDefault(current.themeMode),
            dynamicColor = root.optBoolean("dynamicColor", current.dynamicColor),
            fontChoice = runCatching {
                LauncherFont.valueOf(root.optString("fontChoice", current.fontChoice.name))
            }.getOrDefault(current.fontChoice),
            iconPack = root.optString("iconPack", current.iconPack),
            showClock = root.optBoolean("showClock", current.showClock),
            showDate = root.optBoolean("showDate", current.showDate),
            use24Hour = root.optBoolean("use24Hour", current.use24Hour),
            layout = runCatching {
                AppLayout.valueOf(root.optString("layout", current.layout.name))
            }.getOrDefault(current.layout),
            gridColumns = root.optInt("gridColumns", current.gridColumns)
                .coerceIn(MIN_COLUMNS, MAX_COLUMNS),
            autoFocusSearch = root.optBoolean("autoFocusSearch", current.autoFocusSearch),
            showAlphabetRail = root.optBoolean("showAlphabetRail", current.showAlphabetRail),
            haptics = root.optBoolean("haptics", current.haptics),
            compactRows = root.optBoolean("compactRows", current.compactRows),
            newsLocation = root.optString("newsLocation", current.newsLocation),
            showNewsArticle = root.optBoolean("showNewsArticle", current.showNewsArticle),
            showNewsAppLinks = root.optBoolean("showNewsAppLinks", current.showNewsAppLinks),
            showNewsUnread = root.optBoolean("showNewsUnread", current.showNewsUnread),
            showNewsPhoto = root.optBoolean("showNewsPhoto", current.showNewsPhoto),
            newsImageUri = root.optString("newsImageUri", current.newsImageUri),
            showNewsWidgets = root.optBoolean("showNewsWidgets", current.showNewsWidgets),
            notesPackage = root.optString("notesPackage", current.notesPackage),
            showNotificationBadges = root.optBoolean("showNotificationBadges", current.showNotificationBadges),
            doubleTapAction = restoredGesture("doubleTapAction", current.doubleTapAction),
            longPressAction = restoredGesture("longPressAction", current.longPressAction),
            swipeLeftAction = restoredGesture("swipeLeftAction", current.swipeLeftAction),
            swipeRightAction = restoredGesture("swipeRightAction", current.swipeRightAction),
            swipeUpAction = restoredGesture("swipeUpAction", current.swipeUpAction),
            twoFingerSwipeUpAction = restoredGesture("twoFingerSwipeUpAction", current.twoFingerSwipeUpAction),
            twoFingerSwipeDownAction = restoredGesture("twoFingerSwipeDownAction", current.twoFingerSwipeDownAction),
            favorites = favorites,
            hidden = hidden,
        )
        _settings.value = restored
        persist(restored)
        true
    }.getOrDefault(false)

    companion object {
        const val SCHEMA = 1
        const val MIN_COLUMNS = 3
        const val MAX_COLUMNS = 6

        private const val PREFS = "bitlauncher_settings"
        private const val SEPARATOR = "\u001f"

        private const val KEY_STYLE = "style"
        private const val KEY_THEME = "theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_FONT = "font_choice"
        private const val KEY_ICON_PACK = "icon_pack"
        private const val KEY_SHOW_CLOCK = "show_clock"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_24H = "use_24h"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_COLUMNS = "columns"
        private const val KEY_AUTOFOCUS = "autofocus"
        private const val KEY_ALPHABET_RAIL = "alphabet_rail"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_COMPACT = "compact"
        private const val KEY_NEWS_LOCATION = "news_location"
        private const val KEY_NEWS_ARTICLE = "news_article"
        private const val KEY_NEWS_APP_LINKS = "news_app_links"
        private const val KEY_NEWS_UNREAD = "news_unread"
        private const val KEY_NEWS_PHOTO = "news_photo"
        private const val KEY_NEWS_IMAGE = "news_image_uri"
        private const val KEY_NEWS_WIDGETS = "news_widgets"
        private const val KEY_NOTES_PACKAGE = "notes_package"
        private const val KEY_NOTIFICATION_BADGES = "notification_badges"
        private const val KEY_DOUBLE_TAP = "gesture_double_tap"
        private const val KEY_LONG_PRESS = "gesture_long_press"
        private const val KEY_SWIPE_LEFT = "gesture_swipe_left"
        private const val KEY_SWIPE_RIGHT = "gesture_swipe_right"
        private const val KEY_SWIPE_UP = "gesture_swipe_up"
        private const val KEY_TWO_FINGER_UP = "gesture_two_finger_up"
        private const val KEY_TWO_FINGER_DOWN = "gesture_two_finger_down"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_HIDDEN = "hidden"
    }
}
