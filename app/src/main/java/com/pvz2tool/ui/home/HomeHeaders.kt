package com.pvz2tool.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pvz2tool.data.entity.PlantColor
import com.pvz2tool.domain.DisplaySection

/**
 * 通用可折叠面板头（主页块头、其他道具块共用）。
 * 淡色圆角底把整块圈起来，折叠时只剩这条标题，视觉边界更清楚。
 */
@Composable
fun PanelHeader(
    title: String,
    countText: String?,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CollapseArrow(
            collapsed = collapsed,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(start = 2.dp)
        )
        Spacer(Modifier.weight(1f))
        if (countText != null) {
            Text(
                text = countText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/** 一级块头：可探险 / 不可探险。 */
@Composable
fun BlockHeader(
    explorable: Boolean,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    PanelHeader(
        title = if (explorable) "可探险植物" else "不可探险植物",
        countText = "$count 张",
        collapsed = collapsed,
        onToggle = onToggle,
        modifier = modifier
    )
}

/** 二级小节头：进行中 / 已满待五阶 / 已完成。左侧竖条标色，整行可点击。 */
@Composable
fun SectionHeader(
    section: DisplaySection,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        CollapseArrow(
            collapsed = collapsed,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Text(
            text = sectionTitle(section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 2.dp)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "$count 张",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 三级颜色组表头：色点 + 色名 + 张数，缩进比状态小节更深一层。 */
@Composable
fun ColorHeader(
    color: PlantColor,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 22.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CollapseArrow(
            collapsed = collapsed,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Box(
            Modifier
                .padding(start = 6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(color.indicatorColor())
        )
        Text(
            text = "${color.displayName}色",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "$count 张",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CollapseArrow(
    collapsed: Boolean,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    // 折叠时箭头向右（-90°），展开时向下（0°），用旋转动画过渡
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        animationSpec = tween(200),
        label = "collapseArrow"
    )
    Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = if (collapsed) "展开" else "折叠",
        tint = tint,
        modifier = modifier.rotate(rotation)
    )
}

private fun sectionTitle(section: DisplaySection): String = when (section) {
    DisplaySection.IN_PROGRESS -> "进行中"
    DisplaySection.PENDING_MAX -> "已满待五阶"
    DisplaySection.COMPLETED -> "已完成"
}

/** 色点颜色，仅用于表头指示；卡片本身依赖图片底色，不再标色。 */
private fun PlantColor.indicatorColor(): Color = when (this) {
    PlantColor.WHITE -> Color(0xFFB0BEC5)
    PlantColor.GREEN -> Color(0xFF4CAF50)
    PlantColor.BLUE -> Color(0xFF2196F3)
    PlantColor.PURPLE -> Color(0xFF9C27B0)
    PlantColor.ORANGE -> Color(0xFFFB8C00)
}
