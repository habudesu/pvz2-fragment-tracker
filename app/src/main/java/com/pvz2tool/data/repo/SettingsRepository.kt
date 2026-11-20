package com.pvz2tool.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pvz2tool_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 配色方案；默认方案即紫色，不再单列一项紫色。 */
enum class ColorSchemeChoice { DEFAULT, BLUE, GREEN, ORANGE, DYNAMIC }

/** 设置页「管理计数器」栏目的折叠 id，与 SettingsScreen 共用。 */
const val SETTINGS_CARDS_GROUP_ID = "settings-cards"

/** 设置页「关于」栏目的折叠 id，与 SettingsScreen 共用。 */
const val SETTINGS_ABOUT_GROUP_ID = "settings-about"

/** 默认折叠的栏目：设置页的长栏目先收起。主页折叠状态只存内存，不走这里。 */
private val DEFAULT_COLLAPSED_GROUPS = setOf(
    SETTINGS_CARDS_GROUP_ID,
    SETTINGS_ABOUT_GROUP_ID
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorScheme: ColorSchemeChoice = ColorSchemeChoice.DEFAULT,
    /** 首次启动引导是否已经展示过。 */
    val guideShown: Boolean = false,
    /** 设置页里被折叠的栏目 id，属于设备本地偏好。 */
    val collapsedGroupIds: Set<String> = DEFAULT_COLLAPSED_GROUPS
)

/** 设置仓库：用 DataStore 持久化主题、配色、折叠状态与引导标记。 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val SCHEME = stringPreferencesKey("color_scheme")
        val GUIDE_SHOWN = booleanPreferencesKey("guide_shown")
        val COLLAPSED = stringSetPreferencesKey("collapsed_groups")
        val ABOUT_DEFAULT_APPLIED = booleanPreferencesKey("about_default_collapsed_applied")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            colorScheme = prefs[Keys.SCHEME]
                ?.let { runCatching { ColorSchemeChoice.valueOf(it) }.getOrNull() }
                ?: ColorSchemeChoice.DEFAULT,
            guideShown = prefs[Keys.GUIDE_SHOWN] ?: false,
            collapsedGroupIds = prefs[Keys.COLLAPSED] ?: DEFAULT_COLLAPSED_GROUPS
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setColorScheme(scheme: ColorSchemeChoice) {
        context.dataStore.edit { it[Keys.SCHEME] = scheme.name }
    }

    /** 标记首次引导已展示，之后不再弹出。 */
    suspend fun markGuideShown() {
        context.dataStore.edit { it[Keys.GUIDE_SHOWN] = true }
    }

    /** 折叠或展开某个块/小节/设置栏目（id 由 Grouping 或设置页生成）。 */
    suspend fun toggleCollapsedGroup(id: String) {
        context.dataStore.edit { prefs ->
            // 与读取处保持一致：键不存在时按默认折叠集合处理，否则「展开默认折叠项」会失效
            val current = prefs[Keys.COLLAPSED] ?: DEFAULT_COLLAPSED_GROUPS
            prefs[Keys.COLLAPSED] = if (id in current) current - id else current + id
        }
    }

    /**
     * 把后来新增的默认折叠栏目补进已有安装的折叠集合。
     * 只执行一次：用户之后手动展开「关于」不会被下次启动重新收起。
     */
    suspend fun applyNewDefaultCollapsedGroups() {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.ABOUT_DEFAULT_APPLIED] == true) return@edit
            val current = prefs[Keys.COLLAPSED] ?: DEFAULT_COLLAPSED_GROUPS
            prefs[Keys.COLLAPSED] = current + SETTINGS_ABOUT_GROUP_ID
            prefs[Keys.ABOUT_DEFAULT_APPLIED] = true
        }
    }

}
