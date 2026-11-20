package com.pvz2tool.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pvz2tool.data.entity.PlantColor

/** 培养液图标：打包在 assets/icons 下的透明底 PNG，按品质颜色一一对应。 */
private val SOLUTION_ICON_PATHS = mapOf(
    PlantColor.WHITE to "icons/solution_white.png",
    PlantColor.GREEN to "icons/solution_green.png",
    PlantColor.BLUE to "icons/solution_blue.png",
    PlantColor.PURPLE to "icons/solution_purple.png",
    PlantColor.ORANGE to "icons/solution_orange.png"
)

/** 培养液小图标，尺寸由调用方 modifier 决定（设置页 30dp、主页 22dp）；透明底，等比完整显示。 */
@Composable
fun SolutionIcon(color: PlantColor, modifier: Modifier = Modifier) {
    val path = SOLUTION_ICON_PATHS.getValue(color)
    val context = LocalContext.current
    val request = remember(path) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/$path")
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = "${color.displayName}色培养液",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
