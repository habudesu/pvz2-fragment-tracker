package com.pvz2tool.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pvz2tool.R

/** 思源黑体（Noto Sans SC，SIL OFL 免费可商用）：常规 + 粗体两个字重。 */
val SourceHanSans: FontFamily = FontFamily(
    Font(R.font.noto_sans_sc_regular, FontWeight.Normal),
    Font(R.font.noto_sans_sc_bold, FontWeight.Bold)
)

/**
 * 全局排版：正文用常规字重，标题和标签用粗体拉开层级。
 * 只打包 400/700 两个字重，Medium/SemiBold 的样式就近取粗体。
 */
val AppTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = SourceHanSans),
        displayMedium = displayMedium.copy(fontFamily = SourceHanSans),
        displaySmall = displaySmall.copy(fontFamily = SourceHanSans),
        headlineLarge = headlineLarge.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Bold),
        bodyLarge = bodyLarge.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Normal),
        bodyMedium = bodyMedium.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Normal),
        bodySmall = bodySmall.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Normal),
        labelLarge = labelLarge.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Bold),
        labelMedium = labelMedium.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Bold),
        labelSmall = labelSmall.copy(fontFamily = SourceHanSans, fontWeight = FontWeight.Normal)
    )
}
