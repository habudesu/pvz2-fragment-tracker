package com.pvz2tool.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pvz2tool.data.entity.PlantCardEntity
import com.pvz2tool.data.io.ImportMode
import com.pvz2tool.data.repo.ColorSchemeChoice
import com.pvz2tool.data.repo.SETTINGS_ABOUT_GROUP_ID
import com.pvz2tool.data.repo.SETTINGS_CARDS_GROUP_ID
import com.pvz2tool.data.repo.ThemeMode
import com.pvz2tool.ui.common.CardEditorDialog
import com.pvz2tool.ui.common.statusLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设置页（参考文档 §14）：外观、显示、管理计数器、导入导出、关于。
 * 注意：计数器不提供删除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val collapsedIds = state.settings.collapsedGroupIds

    // 对话框状态
    var cardEditorOpen by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<PlantCardEntity?>(null) }
    // 导出：先生成 JSON，等用户在系统选择器里选好位置再写文件
    var pendingExport by remember { mutableStateOf<String?>(null) }
    // 导入：从选择器读到的文件内容，交给确认弹窗预览
    var pendingImportText by remember { mutableStateOf<String?>(null) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExport
        pendingExport = null
        if (uri == null || json == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入文件")
                }.isSuccess
            }
            showMessage(if (ok) "已导出到文件" else "导出失败，请重试")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                }.getOrNull()
            }
            if (text.isNullOrBlank()) {
                showMessage("读取文件失败")
            } else {
                pendingImportText = text
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ---- 外观（选项少，直接平铺，不做折叠）----
            item(key = "appearance") {
                PlainSection(title = "外观") {
                    ThemeModeRow(state.settings.themeMode, onSelect = viewModel::setThemeMode)
                    ColorSchemeRow(state.settings.colorScheme, onSelect = viewModel::setColorScheme)
                }
            }

            // ---- 管理计数器（不提供删除；长列表保持行级懒加载）----
            item(key = ID_CARDS) {
                CollapsibleSectionHeader(
                    title = "管理计数器",
                    collapsed = ID_CARDS in collapsedIds,
                    onToggle = { viewModel.toggleCollapsed(ID_CARDS) }
                ) {
                    TextButton(onClick = {
                        editingCard = null
                        cardEditorOpen = true
                    }) { Text("新增") }
                }
            }
            if (ID_CARDS !in collapsedIds) {
                if (state.cards.isEmpty()) {
                    item {
                        Text(
                            text = "暂无计数器，点击右上角「新增」创建。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(state.cards, key = { "card-${it.id}" }) { card ->
                        ManageCardRow(
                            card = card,
                            onEdit = {
                                editingCard = card
                                cardEditorOpen = true
                            }
                        )
                    }
                }
            }

            // ---- 数据（两个按钮直接放在标题行右侧）----
            item(key = "data") {
                PlainSection(
                    title = "数据",
                    trailing = {
                        DataButton("导出文件") {
                            viewModel.exportJson { result ->
                                if (result.startsWith("导出失败")) {
                                    showMessage(result)
                                } else {
                                    pendingExport = result
                                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault())
                                        .format(Date())
                                    exportLauncher.launch("pvz2tool-backup-$stamp.json")
                                }
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        DataButton("导入文件") {
                            // 一些文件管理器把 json 识别成 text/plain，这里一并放行
                            importLauncher.launch(arrayOf("application/json", "text/plain", "text/*"))
                        }
                    }
                )
            }

            // ---- 关于 ----
            item(key = ID_ABOUT) {
                SettingsSection(
                    title = "关于",
                    collapsed = ID_ABOUT in collapsedIds,
                    onToggle = { viewModel.toggleCollapsed(ID_ABOUT) }
                ) {
                    AboutContent(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
                }
            }

            // 底部占位，避免最后一个栏目贴着导航栏
            item(key = "bottom-spacer") {
                Box(Modifier.height(56.dp))
            }
        }
    }

    // ---- 各类对话框（放在 Scaffold 外，保证覆盖全屏）----
    if (cardEditorOpen) {
        CardEditorDialog(
            initial = editingCard,
            onDismiss = {
                cardEditorOpen = false
                editingCard = null
            },
            onSave = { title, url, color, explorable, status, maxTier ->
                val target = editingCard
                if (target == null) {
                    viewModel.addCard(title, url, color, explorable, status, maxTier)
                } else {
                    viewModel.updateCard(target, title, url, color, explorable, status, maxTier)
                }
                cardEditorOpen = false
                editingCard = null
            }
        )
    }

    pendingImportText?.let { text ->
        ImportDialog(
            initialText = text,
            onDismiss = { pendingImportText = null },
            onConfirm = { content, mode, overwriteConflicts ->
                pendingImportText = null
                viewModel.importJson(content, mode, overwriteConflicts) { message ->
                    showMessage(message)
                }
            }
        )
    }
}

// ---------------- 小组件 ----------------

// 设置页可折叠栏目的 id，持久化在 DataStore 的 collapsed_groups 集合里；
// 外观和「数据」直接平铺，不需要 id
private const val ID_CARDS = SETTINGS_CARDS_GROUP_ID // 首次进入默认收起
private const val ID_ABOUT = SETTINGS_ABOUT_GROUP_ID // 首次进入默认收起

/**
 * 栏目标题行：标题统一从 16dp 起，保证各栏目标题左对齐；
 * 右侧放操作按钮或折叠箭头。
 */
@Composable
private fun SectionTitleRow(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) trailing()
    }
}

/**
 * 设置页的可折叠栏目表头：整行可点，箭头在右侧随状态旋转。
 * trailing 用于「管理计数器」右侧的「新增」按钮。
 */
@Composable
private fun CollapsibleSectionHeader(
    title: String,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        animationSpec = tween(200),
        label = "settingsArrow"
    )
    SectionTitleRow(
        title = title,
        modifier = modifier,
        onClick = onToggle,
        trailing = {
            if (trailing != null) trailing()
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (collapsed) "展开" else "折叠",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .rotate(rotation)
            )
        }
    )
}

/**
 * 不折叠的栏目：标题 + 可选右侧操作 + 可选内容。
 * 「数据」把两个按钮直接放在标题行右边，所以 content 可以为空。
 */
@Composable
private fun PlainSection(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(modifier) {
        SectionTitleRow(title = title, trailing = trailing)
        if (content != null) Column(content = content)
    }
}

/** 「数据」栏目用的紧凑按钮，与标题行同高，不把表头撑高。 */
@Composable
private fun DataButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * 一个可折叠的栏目：表头 + 带展开/收起动画的内容。
 * 「管理计数器」是长列表，仍按行懒加载，不走这个组件。
 */
@Composable
private fun SettingsSection(
    title: String,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier) {
        CollapsibleSectionHeader(
            title = title,
            collapsed = collapsed,
            onToggle = onToggle,
            trailing = trailing
        )
        AnimatedVisibility(
            visible = !collapsed,
            enter = expandVertically(animationSpec = tween(240), expandFrom = Alignment.Top) +
                fadeIn(animationSpec = tween(240)),
            exit = shrinkVertically(animationSpec = tween(240), shrinkTowards = Alignment.Top) +
                fadeOut(animationSpec = tween(240))
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeModeRow(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(themeLabel(mode)) }
            )
        }
    }
}

@Composable
private fun ColorSchemeRow(selected: ColorSchemeChoice, onSelect: (ColorSchemeChoice) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ColorSchemeChoice.entries.forEach { choice ->
            FilterChip(
                selected = selected == choice,
                onClick = { onSelect(choice) },
                label = { Text(schemeLabel(choice)) }
            )
        }
    }
}

@Composable
private fun ManageCardRow(card: PlantCardEntity, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = card.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val extra = if (card.explorable) " · 探险" else ""
            Text(
                text = "${statusLabel(card.fifthTierStatus)} · 封顶 ${card.maxTier} 阶 · ${card.color.displayName}色$extra",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "编辑")
        }
    }
}

/** 导入弹窗：预览选中的文件内容，选择覆盖/合并，以及冲突时如何处理。 */
@Composable
private fun ImportDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, ImportMode, Boolean) -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var mode by remember { mutableStateOf(ImportMode.MERGE) }
    var overwriteConflicts by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入 JSON") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == ImportMode.MERGE,
                        onClick = { mode = ImportMode.MERGE },
                        label = { Text("合并") }
                    )
                    FilterChip(
                        selected = mode == ImportMode.OVERWRITE,
                        onClick = { mode = ImportMode.OVERWRITE },
                        label = { Text("覆盖") }
                    )
                }
                if (mode == ImportMode.MERGE) {
                    FieldLabel("冲突时（同 stableKey 或同名）")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = overwriteConflicts,
                            onClick = { overwriteConflicts = true },
                            label = { Text("用导入数据覆盖") }
                        )
                        FilterChip(
                            selected = !overwriteConflicts,
                            onClick = { overwriteConflicts = false },
                            label = { Text("保留本地") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("JSON 内容") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                if (mode == ImportMode.OVERWRITE) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "覆盖会先清空全部现有数据。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text, mode, overwriteConflicts) },
                enabled = text.isNotBlank()
            ) { Text("导入") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ---------------- 枚举显示名 ----------------

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

private fun schemeLabel(choice: ColorSchemeChoice): String = when (choice) {
    ColorSchemeChoice.DEFAULT -> "默认"
    ColorSchemeChoice.BLUE -> "蓝色"
    ColorSchemeChoice.GREEN -> "绿色"
    ColorSchemeChoice.ORANGE -> "橙色"
    ColorSchemeChoice.DYNAMIC -> "动态取色"
}
