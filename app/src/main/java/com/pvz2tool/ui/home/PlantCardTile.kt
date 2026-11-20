package com.pvz2tool.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pvz2tool.domain.DisplaySection
import com.pvz2tool.domain.PlantCardUi
import com.pvz2tool.ui.common.PlantImage

/**
 * 网格中的单张卡片：完整图片 + 标题 + 阶数状态 + 碎片加减。
 * - 点击卡片：编辑进度。
 * - 长按：弹出菜单（编辑进度 / 编辑卡片信息）。不提供删除。
 * 图片用 Fit 完整显示，不再裁掉上下两截。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlantCardTile(
    card: PlantCardUi,
    onAdd: (Int) -> Unit,
    onEdit: () -> Unit,
    onEditInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = card.entity
    var menuOpen by remember { mutableStateOf(false) }

    // 已满待五阶的卡片不写「待五阶」，直接给出还差多少碎片
    val statusText = when {
        card.section == DisplaySection.PENDING_MAX ->
            "${entity.currentTier}/${entity.maxTier}阶 · 还需${card.remainingFragments}"
        card.isMaxed -> "${entity.currentTier}/${entity.maxTier}阶 · 已到顶"
        else -> "${entity.currentTier}/${entity.maxTier}阶 · 还需${card.remainingFragments}"
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .combinedClickable(
                    onClick = onEdit,
                    onLongClick = { menuOpen = true }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图片占满剩余高度并用 Fit，整张图完整可见；可升阶时右上角加小徽标
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PlantImage(
                        url = entity.imageUrl,
                        title = entity.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    PromoteBadge(
                        visible = card.canPromote,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = entity.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CounterButton(
                        text = "−",
                        enabled = entity.currentFragments > 0,
                        label = "减一",
                        onClick = { onAdd(-1) }
                    )
                    AnimatedContent(
                        targetState = entity.currentFragments,
                        transitionSpec = {
                            // 加时新数字从下方滑入，减时从上方滑入
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInVertically(tween(160)) { direction * it } + fadeIn(tween(160)))
                                .togetherWith(
                                    slideOutVertically(tween(160)) { -direction * it } + fadeOut(tween(120))
                                )
                        },
                        contentAlignment = Alignment.Center,
                        label = "fragmentCount",
                        modifier = Modifier.weight(1f)
                    ) { value ->
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CounterButton(
                        text = "+",
                        enabled = true,
                        label = "加一",
                        onClick = { onAdd(1) }
                    )
                }
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text("编辑进度") },
                onClick = {
                    menuOpen = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("编辑卡片信息") },
                onClick = {
                    menuOpen = false
                    onEditInfo()
                }
            )
        }
    }
}

/** 「可升阶」小徽标：出现/消失时淡入淡出并轻微缩放。 */
@Composable
private fun PromoteBadge(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.7f, animationSpec = tween(200)),
        exit = fadeOut(tween(120)),
        modifier = modifier
    ) {
        Text(
            text = "可升阶",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

/**
 * 紧凑加减按钮，卡片与其他道具行共用。
 * 三列布局下放不下 M3 的 48dp 最小触达，这里用 30dp 圆钮换取宽度；
 * 其他道具行沿横向排得更满，传 24dp 让按钮不至于抢视觉。
 */
@Composable
internal fun CounterButton(
    text: String,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    size: Dp = 30.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
