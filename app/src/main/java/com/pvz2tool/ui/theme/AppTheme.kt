package com.pvz2tool.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.pvz2tool.data.repo.ColorSchemeChoice
import com.pvz2tool.data.repo.ThemeMode

/**
 * 全局主题（参考文档 §12）。
 *
 * 两级设置：
 * 1. 深浅色：跟随系统 / 强制浅色 / 强制深色（[ThemeMode]）。
 * 2. 配色：默认、蓝、绿、紫、橙、动态取色（[ColorSchemeChoice]）。
 *
 * 动态取色只在 Android 12（API 31）及以上生效，低版本回退到默认配色。
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorScheme: ColorSchemeChoice = ColorSchemeChoice.DEFAULT,
    content: @Composable () -> Unit
) {
    // 先根据模式决定用深色还是浅色
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val targetScheme: ColorScheme =
        if (colorScheme == ColorSchemeChoice.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 动态取色：从系统壁纸提取颜色
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            presetScheme(colorScheme, dark)
        }
    // 深浅色/配色切换时所有颜色一起过渡，避免顶部栏和内容分两段跳变
    val scheme = animatedScheme(targetScheme)

    // Activity 不因 uiMode 重建，系统栏图标颜色需要跟着主题手动切
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}

private val ThemeColorSpec = tween<Color>(durationMillis = 280)

@Composable
private fun animateThemeColor(target: Color, label: String): Color =
    animateColorAsState(targetValue = target, animationSpec = ThemeColorSpec, label = label).value

/**
 * 把整套配色逐项做颜色动画。
 * 所有通道共用同一个时长，切换主题时界面整体渐变，不会出现局部先变的情况。
 */
@Composable
private fun animatedScheme(target: ColorScheme): ColorScheme = target.copy(
    primary = animateThemeColor(target.primary, "primary"),
    onPrimary = animateThemeColor(target.onPrimary, "onPrimary"),
    primaryContainer = animateThemeColor(target.primaryContainer, "primaryContainer"),
    onPrimaryContainer = animateThemeColor(target.onPrimaryContainer, "onPrimaryContainer"),
    inversePrimary = animateThemeColor(target.inversePrimary, "inversePrimary"),
    secondary = animateThemeColor(target.secondary, "secondary"),
    onSecondary = animateThemeColor(target.onSecondary, "onSecondary"),
    secondaryContainer = animateThemeColor(target.secondaryContainer, "secondaryContainer"),
    onSecondaryContainer = animateThemeColor(target.onSecondaryContainer, "onSecondaryContainer"),
    tertiary = animateThemeColor(target.tertiary, "tertiary"),
    onTertiary = animateThemeColor(target.onTertiary, "onTertiary"),
    tertiaryContainer = animateThemeColor(target.tertiaryContainer, "tertiaryContainer"),
    onTertiaryContainer = animateThemeColor(target.onTertiaryContainer, "onTertiaryContainer"),
    background = animateThemeColor(target.background, "background"),
    onBackground = animateThemeColor(target.onBackground, "onBackground"),
    surface = animateThemeColor(target.surface, "surface"),
    onSurface = animateThemeColor(target.onSurface, "onSurface"),
    surfaceVariant = animateThemeColor(target.surfaceVariant, "surfaceVariant"),
    onSurfaceVariant = animateThemeColor(target.onSurfaceVariant, "onSurfaceVariant"),
    surfaceTint = animateThemeColor(target.surfaceTint, "surfaceTint"),
    inverseSurface = animateThemeColor(target.inverseSurface, "inverseSurface"),
    inverseOnSurface = animateThemeColor(target.inverseOnSurface, "inverseOnSurface"),
    error = animateThemeColor(target.error, "error"),
    onError = animateThemeColor(target.onError, "onError"),
    errorContainer = animateThemeColor(target.errorContainer, "errorContainer"),
    onErrorContainer = animateThemeColor(target.onErrorContainer, "onErrorContainer"),
    outline = animateThemeColor(target.outline, "outline"),
    outlineVariant = animateThemeColor(target.outlineVariant, "outlineVariant"),
    scrim = animateThemeColor(target.scrim, "scrim"),
    surfaceBright = animateThemeColor(target.surfaceBright, "surfaceBright"),
    surfaceDim = animateThemeColor(target.surfaceDim, "surfaceDim"),
    surfaceContainer = animateThemeColor(target.surfaceContainer, "surfaceContainer"),
    surfaceContainerHigh = animateThemeColor(target.surfaceContainerHigh, "surfaceContainerHigh"),
    surfaceContainerHighest = animateThemeColor(target.surfaceContainerHighest, "surfaceContainerHighest"),
    surfaceContainerLow = animateThemeColor(target.surfaceContainerLow, "surfaceContainerLow"),
    surfaceContainerLowest = animateThemeColor(target.surfaceContainerLowest, "surfaceContainerLowest")
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 预置配色：每个方案给出一套完整的 Material 3 主色/容器色（深浅各一份）。
 * 色值参考 Material Theme Builder 生成的调色板；容器色用于徽标、Chip 等浅底元素。
 * 动态取色在低版本回退到「默认」的紫色方案。
 */
private fun presetScheme(choice: ColorSchemeChoice, dark: Boolean): ColorScheme = when (choice) {
    ColorSchemeChoice.BLUE -> if (dark) darkScheme(
        primary = 0xFF9ECAFF, primaryContainer = 0xFF00497D, onPrimaryContainer = 0xFFD1E4FF,
        secondary = 0xFFBBC7DB, secondaryContainer = 0xFF3B4858, onSecondaryContainer = 0xFFD7E3F7,
        tertiary = 0xFFD6BEE4, tertiaryContainer = 0xFF523F5F, onTertiaryContainer = 0xFFF2DAFF
    ) else lightScheme(
        primary = 0xFF0061A4, primaryContainer = 0xFFD1E4FF, onPrimaryContainer = 0xFF001D36,
        secondary = 0xFF535F70, secondaryContainer = 0xFFD7E3F7, onSecondaryContainer = 0xFF101C2B,
        tertiary = 0xFF6B5778, tertiaryContainer = 0xFFF2DAFF, onTertiaryContainer = 0xFF251431
    )

    ColorSchemeChoice.GREEN -> if (dark) darkScheme(
        primary = 0xFF9CD67D, primaryContainer = 0xFF235600, onPrimaryContainer = 0xFFB7F397,
        secondary = 0xFFBDCBB0, secondaryContainer = 0xFF3D4B36, onSecondaryContainer = 0xFFD9E7CA,
        tertiary = 0xFF9FCFD0, tertiaryContainer = 0xFF1E4E4F, onTertiaryContainer = 0xFFBBEBEC
    ) else lightScheme(
        primary = 0xFF386A20, primaryContainer = 0xFFB7F397, onPrimaryContainer = 0xFF042100,
        secondary = 0xFF55624C, secondaryContainer = 0xFFD9E7CA, onSecondaryContainer = 0xFF131F0D,
        tertiary = 0xFF386667, tertiaryContainer = 0xFFBBEBEC, onTertiaryContainer = 0xFF002021
    )

    ColorSchemeChoice.ORANGE -> if (dark) darkScheme(
        primary = 0xFFFFB874, primaryContainer = 0xFF693C00, onPrimaryContainer = 0xFFFFDCBE,
        secondary = 0xFFE2C093, secondaryContainer = 0xFF58442B, onSecondaryContainer = 0xFFFEDFBD,
        tertiary = 0xFFC4CB92, tertiaryContainer = 0xFF444A21, onTertiaryContainer = 0xFFE0E7AE
    ) else lightScheme(
        primary = 0xFF8B5000, primaryContainer = 0xFFFFDCBE, onPrimaryContainer = 0xFF2E1500,
        secondary = 0xFF725B41, secondaryContainer = 0xFFFEDFBD, onSecondaryContainer = 0xFF291806,
        tertiary = 0xFF5C6237, tertiaryContainer = 0xFFE0E7AE, onTertiaryContainer = 0xFF191E00
    )

    // 默认 / 动态取色（Android 12 以下回退）：紫色，作为用户偏好的默认方案
    ColorSchemeChoice.DEFAULT, ColorSchemeChoice.DYNAMIC -> if (dark) darkScheme(
        primary = 0xFFD0BCFF, primaryContainer = 0xFF4F378B, onPrimaryContainer = 0xFFEADDFF,
        secondary = 0xFFCCC2DC, secondaryContainer = 0xFF4A4458, onSecondaryContainer = 0xFFE8DEF8,
        tertiary = 0xFFEFB8C8, tertiaryContainer = 0xFF633B48, onTertiaryContainer = 0xFFFFDBE5
    ) else lightScheme(
        primary = 0xFF6750A4, primaryContainer = 0xFFEADDFF, onPrimaryContainer = 0xFF21005D,
        secondary = 0xFF625B71, secondaryContainer = 0xFFE8DEF8, onSecondaryContainer = 0xFF1D192B,
        tertiary = 0xFF7D5260, tertiaryContainer = 0xFFFFDBE5, onTertiaryContainer = 0xFF31111D
    )
}

private fun lightScheme(
    primary: Long,
    primaryContainer: Long,
    onPrimaryContainer: Long,
    secondary: Long,
    secondaryContainer: Long,
    onSecondaryContainer: Long,
    tertiary: Long,
    tertiaryContainer: Long,
    onTertiaryContainer: Long
): ColorScheme = lightColorScheme(
    primary = Color(primary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer),
    tertiary = Color(tertiary),
    tertiaryContainer = Color(tertiaryContainer),
    onTertiaryContainer = Color(onTertiaryContainer)
)

private fun darkScheme(
    primary: Long,
    primaryContainer: Long,
    onPrimaryContainer: Long,
    secondary: Long,
    secondaryContainer: Long,
    onSecondaryContainer: Long,
    tertiary: Long,
    tertiaryContainer: Long,
    onTertiaryContainer: Long
): ColorScheme = darkColorScheme(
    primary = Color(primary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer),
    tertiary = Color(tertiary),
    tertiaryContainer = Color(tertiaryContainer),
    onTertiaryContainer = Color(onTertiaryContainer)
)
