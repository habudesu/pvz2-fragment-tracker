package com.pvz2tool.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pvz2tool.PvzApplication
import com.pvz2tool.data.repo.AppSettings
import com.pvz2tool.ui.home.HomeRoute
import com.pvz2tool.ui.settings.SettingsRoute
import com.pvz2tool.ui.theme.AppTheme

/**
 * 应用根：单 Activity，无底部导航。
 * 用布尔状态在「主页」与「设置」之间切换；系统返回键在设置页时回到主页。
 */
@Composable
fun AppRoot() {
    val context = LocalContext.current
    // remember 避免每次重组都去拿依赖容器
    val settingsRepository = remember(context) {
        (context.applicationContext as PvzApplication).container.settingsRepository
    }
    // 主题设置从 DataStore 读取；初始给默认值，读到后自动刷新
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
    var showSettings by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showSettings) { showSettings = false }

    AppTheme(themeMode = settings.themeMode, colorScheme = settings.colorScheme) {
        // 主页/设置之间做一次简单的横向滑动 + 淡入淡出
        AnimatedContent(
            targetState = showSettings,
            transitionSpec = {
                if (targetState) {
                    // 进设置：设置页从右侧滑入，主页轻微左移并淡出
                    (slideInHorizontally(animationSpec = tween(280)) { it } + fadeIn(tween(220)))
                        .togetherWith(
                            slideOutHorizontally(animationSpec = tween(280)) { -it / 4 } +
                                fadeOut(tween(180))
                        )
                } else {
                    // 返回主页：方向相反
                    (slideInHorizontally(animationSpec = tween(280)) { -it / 4 } + fadeIn(tween(220)))
                        .togetherWith(
                            slideOutHorizontally(animationSpec = tween(280)) { it } +
                                fadeOut(tween(180))
                        )
                }
            },
            label = "screen"
        ) { settingsShown ->
            if (settingsShown) {
                SettingsRoute(onBack = { showSettings = false })
            } else {
                HomeRoute(onOpenSettings = { showSettings = true })
            }
        }
    }
}
