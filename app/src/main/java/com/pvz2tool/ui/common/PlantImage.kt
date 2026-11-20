package com.pvz2tool.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition

/**
 * 卡片图片（参考文档 §13）。
 * - 尺寸完全由调用方的 modifier 决定（列表用 52dp，网格卡片撑满图片区）。
 * - 本地路径（打包在 assets 里，如 "cards/xxx.webp"）直接读 APK，不联网；
 *   自定义卡片的 http 地址仍走 Coil 网络加载。
 * - 无 URL：显示一个灰底方块，中间放标题首字，避免列表高度跳动。
 * - contentScale 默认裁切填满；网格卡片传 Fit 保证整张图完整可见。
 */
@Composable
fun PlantImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val shape = RoundedCornerShape(10.dp)
    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    } else {
        val context = LocalContext.current
        val isNetwork = url.startsWith("http", ignoreCase = true)
        val request = remember(url) {
            val builder = ImageRequest.Builder(context)
                .data(if (isNetwork) url else "file:///android_asset/" + url.trimStart('/'))
            // 网络图淡入避免突兀；本地图是瞬时的，淡入反而像闪一下
            if (isNetwork) builder.transitionFactory(CrossfadeTransition.Factory(180))
            builder.build()
        }
        AsyncImage(
            model = request,
            contentDescription = title,
            contentScale = contentScale,
            modifier = modifier.clip(shape)
        )
    }
}
