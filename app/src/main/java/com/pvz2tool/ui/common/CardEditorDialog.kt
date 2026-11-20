package com.pvz2tool.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pvz2tool.data.entity.FifthTierStatus
import com.pvz2tool.data.entity.PlantCardEntity
import com.pvz2tool.data.entity.PlantColor
import com.pvz2tool.data.entity.orderRank

/**
 * 新增 / 编辑卡片基础信息。initial 为 null 表示新增。
 *
 * 状态只允许单向提升：NEVER → PENDING → HAS，因此下拉项按当前状态过滤；
 * NEVER 的封顶阶数同样不允许从 3 调到 1。
 */
@Composable
fun CardEditorDialog(
    initial: PlantCardEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String?, PlantColor, Boolean, FifthTierStatus, Int) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var url by remember { mutableStateOf(initial?.imageUrl ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: PlantColor.WHITE) }
    // 默认「不可探险」，与预置数据保持一致；需要时手动打开
    var explorable by remember { mutableStateOf(initial?.explorable ?: false) }
    var status by remember {
        mutableStateOf(initial?.fifthTierStatus ?: FifthTierStatus.PENDING_FIFTH)
    }
    var neverMax by remember {
        mutableStateOf(
            if (initial != null &&
                initial.fifthTierStatus == FifthTierStatus.NEVER_FIFTH &&
                initial.maxTier <= 1
            ) 1 else 3
        )
    }

    // 只能选「当前及更高」的状态
    val minRank = initial?.fifthTierStatus?.orderRank ?: 0
    val statusOptions = FifthTierStatus.entries
        .filter { it.orderRank >= minRank }
        .sortedBy { it.orderRank }

    // NEVER 封顶 3 的卡不允许调回 1
    val neverOptions = if (initial != null &&
        initial.fifthTierStatus == FifthTierStatus.NEVER_FIFTH &&
        initial.maxTier >= 3
    ) listOf(3) else listOf(1, 3)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增计数器" else "编辑卡片") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("植物名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("图片 URL（可留空）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                EditorLabel("品质")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlantColor.entries.forEach { c ->
                        FilterChip(
                            selected = color == c,
                            onClick = { color = c },
                            label = { Text(c.displayName) }
                        )
                    }
                }

                EditorLabel("五阶状态")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statusOptions.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(statusLabel(s)) }
                        )
                    }
                }

                if (status == FifthTierStatus.NEVER_FIFTH) {
                    EditorLabel("封顶阶数")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        neverOptions.forEach { n ->
                            FilterChip(
                                selected = neverMax == n,
                                onClick = { neverMax = n },
                                label = { Text("$n 阶") }
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "封顶：${if (status == FifthTierStatus.HAS_FIFTH) 5 else 4} 阶",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("可通过探险获得", modifier = Modifier.weight(1f))
                    Switch(checked = explorable, onCheckedChange = { explorable = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val requestedMax = when (status) {
                        FifthTierStatus.HAS_FIFTH -> 5
                        FifthTierStatus.PENDING_FIFTH -> 4
                        FifthTierStatus.NEVER_FIFTH -> neverMax
                    }
                    onSave(
                        title.trim(),
                        url.trim().ifBlank { null },
                        color,
                        explorable,
                        status,
                        requestedMax
                    )
                },
                enabled = title.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EditorLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

/** 状态的界面文案，设置页与编辑弹窗共用。 */
fun statusLabel(status: FifthTierStatus): String = when (status) {
    FifthTierStatus.HAS_FIFTH -> "已开放五阶"
    FifthTierStatus.PENDING_FIFTH -> "待五阶"
    FifthTierStatus.NEVER_FIFTH -> "无五阶"
}
