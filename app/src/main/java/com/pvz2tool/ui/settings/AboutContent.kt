package com.pvz2tool.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pvz2tool.BuildConfig
import com.pvz2tool.R

/**
 * 「关于」内容：直接渲染 res/raw/about.md。
 * 只支持 #/## 标题、- 列表和 **加粗**，改文案只需替换该 md 文件。
 */
@Composable
fun AboutContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lines = remember {
        context.resources.openRawResource(R.raw.about)
            .bufferedReader()
            .use { it.readText() }
            .lineSequence()
            .map { it.trim() }
            // 版本号以构建配置为准，避免 md 里的版本落后于实际安装包
            .map { line -> if (line.startsWith("版本 ")) "版本 ${BuildConfig.VERSION_NAME}" else line }
            .toList()
    }
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val strongColor = MaterialTheme.colorScheme.onSurface

    Column(modifier) {
        lines.forEach { line ->
            when {
                // 文档标题：设置页已有「关于」表头，不重复渲染
                line.startsWith("# ") -> Unit

                line.startsWith("## ") -> Text(
                    text = line.removePrefix("## "),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                )

                line.startsWith("- ") -> Row(Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = bodyColor,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = boldText(line.removePrefix("- "), strongColor),
                        style = MaterialTheme.typography.bodySmall,
                        color = bodyColor
                    )
                }

                line.isEmpty() -> Unit

                else -> Text(
                    text = boldText(line, strongColor),
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/** 把 **文字** 解析为加粗片段。 */
private fun boldText(raw: String, boldColor: Color): AnnotatedString = buildAnnotatedString {
    var index = 0
    BOLD_REGEX.findAll(raw).forEach { match ->
        append(raw.substring(index, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
            append(match.groupValues[1])
        }
        index = match.range.last + 1
    }
    append(raw.substring(index))
}

private val BOLD_REGEX = Regex("\\*\\*(.+?)\\*\\*")
