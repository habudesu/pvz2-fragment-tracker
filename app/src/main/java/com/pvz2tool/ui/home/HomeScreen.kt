package com.pvz2tool.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pvz2tool.data.entity.FifthTierStatus
import com.pvz2tool.data.entity.PlantCardEntity
import com.pvz2tool.data.entity.PlantColor
import com.pvz2tool.domain.Grouping
import com.pvz2tool.domain.PlantCardUi
import com.pvz2tool.domain.SolutionSummary
import com.pvz2tool.ui.common.CardEditorDialog
import com.pvz2tool.ui.common.EmptyState
import com.pvz2tool.ui.common.SolutionIcon
import kotlinx.coroutines.launch

/**
 * 折叠/展开时的位移规格：短 tween，不用 spring。
 * 弹簧尾巴长，大段内容会在几百毫秒里不停重排，看起来一卡一卡。
 */
private val CollapsePlacementSpec = tween<IntOffset>(
    durationMillis = 180,
    easing = FastOutSlowInEasing
)

/** 路由层：创建/持有 ViewModel，收集状态后交给无状态的 [HomeScreen]。 */
@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    // 每次进入主页按当前进度重建排序；页面内的加减不触发重排，避免误触
    LaunchedEffect(Unit) { viewModel.refreshLayout() }

    HomeScreen(
        state = state,
        onAddFragments = viewModel::addFragments,
        onAddSolution = viewModel::addSolution,
        onSaveProgress = viewModel::saveProgress,
        onSaveCardInfo = viewModel::saveCardInfo,
        onToggleCollapsed = viewModel::toggleCollapsed,
        onQueryChange = viewModel::setQuery,
        onRefreshLayout = viewModel::refreshLayout,
        onOpenSettings = onOpenSettings,
        onDismissGuide = viewModel::dismissGuide
    )
}

/**
 * 主页（参考文档 §11）：顶栏（刷新排序 + 设置）+ 分组网格 + 其他道具汇总 + 编辑进度弹窗。
 * 「已完成」小节可以折叠，不再提供整段隐藏开关。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAddFragments: (Long, Int) -> Unit,
    onAddSolution: (PlantColor, Int) -> Unit,
    onSaveProgress: (PlantCardEntity, Int, Int) -> Unit,
    onSaveCardInfo: (PlantCardEntity, String, String?, PlantColor, Boolean, FifthTierStatus, Int) -> Unit,
    onToggleCollapsed: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onRefreshLayout: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissGuide: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    // 搜索时忽略折叠状态：默认折叠的二级小节会把搜索结果全部藏起来；
    // 「其他道具」块不跟随搜索，仍按自己的折叠状态显示
    val effectiveCollapsedIds =
        if (state.query.isNotBlank()) emptySet() else state.collapsedIds
    // 只在有需求或已有数量时显示汇总块，条件只算一次
    val relevantSolutions = state.solutions.filter { it.needed > 0 || it.owned > 0 }

    // 正在编辑的卡片；非空时显示编辑进度弹窗
    var editing by remember { mutableStateOf<PlantCardUi?>(null) }
    // 正在编辑卡片基础信息
    var editingInfo by remember { mutableStateOf<PlantCardUi?>(null) }
    // 搜索栏是否展开；关键字本身存在 ViewModel 里
    var searchActive by remember { mutableStateOf(state.query.isNotEmpty()) }
    val searchFocus = remember { FocusRequester() }

    fun closeSearch() {
        searchActive = false
        onQueryChange("")
        keyboard?.hide()
    }

    BackHandler(enabled = searchActive) { closeSearch() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        SearchTitleField(
                            query = state.query,
                            onQueryChange = onQueryChange,
                            focusRequester = searchFocus
                        )
                    } else {
                        Text("植物碎片计数器")
                    }
                },
                navigationIcon = {
                    if (searchActive) {
                        IconButton(onClick = ::closeSearch) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出搜索")
                        }
                    }
                },
                actions = {
                    if (searchActive) {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "清空搜索")
                            }
                        }
                    } else {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        // 手动重排：加减碎片时卡片保持原位，想归位时点这里
                        IconButton(onClick = {
                            onRefreshLayout()
                            scope.launch { snackbarHostState.showSnackbar("已按当前进度重新排序") }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新排序")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.blocks.isEmpty() -> if (state.query.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "没有找到「${state.query.trim()}」",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            else -> LazyVerticalGrid(
                // 360dp 手机约 3 列；平板/横屏按宽度自动加列
                columns = GridCells.Adaptive(minSize = 110.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.blocks.forEach { block ->
                    val blockId = Grouping.blockId(block.explorable)
                    val blockCollapsed = blockId in effectiveCollapsedIds
                    item(
                        key = blockId,
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "header"
                    ) {
                        BlockHeader(
                            explorable = block.explorable,
                            count = block.count,
                            collapsed = blockCollapsed,
                            onToggle = { onToggleCollapsed(blockId) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = CollapsePlacementSpec
                            )
                        )
                    }
                    if (!blockCollapsed) {
                        block.sections.forEach { sectionGroup ->
                            val sectionId = Grouping.sectionId(block.explorable, sectionGroup.section)
                            val sectionCollapsed = sectionId in effectiveCollapsedIds
                            item(
                                key = sectionId,
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = "header"
                            ) {
                                SectionHeader(
                                    section = sectionGroup.section,
                                    count = sectionGroup.count,
                                    collapsed = sectionCollapsed,
                                    onToggle = { onToggleCollapsed(sectionId) },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = null,
                                        placementSpec = CollapsePlacementSpec
                                    )
                                )
                            }
                            if (!sectionCollapsed) {
                                sectionGroup.colors.forEach { colorGroup ->
                                    val colorId = Grouping.colorGroupId(
                                        explorable = block.explorable,
                                        section = sectionGroup.section,
                                        color = colorGroup.color
                                    )
                                    val colorCollapsed = colorId in effectiveCollapsedIds
                                    item(
                                        key = colorId,
                                        span = { GridItemSpan(maxLineSpan) },
                                        contentType = "header"
                                    ) {
                                        ColorHeader(
                                            color = colorGroup.color,
                                            count = colorGroup.cards.size,
                                            collapsed = colorCollapsed,
                                            onToggle = { onToggleCollapsed(colorId) },
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = null,
                                                fadeOutSpec = null,
                                                placementSpec = CollapsePlacementSpec
                                            )
                                        )
                                    }
                                    if (!colorCollapsed) {
                                        items(
                                            items = colorGroup.cards,
                                            key = { "card-${it.entity.id}" },
                                            contentType = { "card" }
                                        ) { card ->
                                            // 已完成的整段调暗，视觉上弱化。
                                            // 只做位移不做淡入淡出：一次折叠可能牵动两三百张卡，
                                            // 淡入淡出会让每一帧多算一遍，位移动画的开销固定且可控。
                                            Box(
                                                Modifier
                                                    .animateItem(
                                                        fadeInSpec = null,
                                                        fadeOutSpec = null,
                                                        placementSpec = CollapsePlacementSpec
                                                    )
                                                    .alpha(if (sectionGroup.dimmed) 0.55f else 1f)
                                            ) {
                                                PlantCardTile(
                                                    card = card,
                                                    onAdd = { delta -> onAddFragments(card.entity.id, delta) },
                                                    onEdit = { editing = card },
                                                    onEditInfo = { editingInfo = card }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (relevantSolutions.isNotEmpty()) {
                    val solutionCollapsed = Grouping.SOLUTION_GROUP_ID in state.collapsedIds
                    item(
                        key = Grouping.SOLUTION_GROUP_ID,
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "header"
                    ) {
                        PanelHeader(
                            title = "其他道具",
                            countText = null,
                            collapsed = solutionCollapsed,
                            onToggle = { onToggleCollapsed(Grouping.SOLUTION_GROUP_ID) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = CollapsePlacementSpec
                            )
                        )
                    }
                    if (!solutionCollapsed) {
                        item(
                            key = "solution-footer",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "solution"
                        ) {
                            SolutionFooter(
                                solutions = relevantSolutions,
                                onAddOwned = onAddSolution,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = CollapsePlacementSpec
                                )
                            )
                        }
                    }
                }
                // 底部占位：最后一行内容可以滚上来，不贴着导航栏
                item(
                    key = "bottom-spacer",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "spacer"
                ) {
                    Box(Modifier.height(88.dp))
                }
            }
        }

        editing?.let { card ->
            EditProgressDialog(
                card = card,
                onDismiss = { editing = null },
                onConfirm = { tier, fragments ->
                    onSaveProgress(card.entity, tier, fragments)
                    editing = null
                }
            )
        }

        editingInfo?.let { card ->
            CardEditorDialog(
                initial = card.entity,
                onDismiss = { editingInfo = null },
                onSave = { title, url, color, explorable, status, maxTier ->
                    onSaveCardInfo(card.entity, title, url, color, explorable, status, maxTier)
                    editingInfo = null
                }
            )
        }

        if (state.showGuide) {
            AlertDialog(
                onDismissRequest = onDismissGuide,
                title = { Text("欢迎使用") },
                text = {
                    Text("填写当前阶数和已有碎片即可开始记录。首次启动会显示全部植物卡片。")
                },
                confirmButton = {
                    TextButton(onClick = onDismissGuide) { Text("开始记录") }
                },
                dismissButton = {
                    TextButton(onClick = onDismissGuide) { Text("跳过") }
                }
            )
        }
    }
}

/** 顶栏里的搜索输入：展开后自动聚焦并弹出键盘，空串时显示占位文案。 */
@Composable
private fun SearchTitleField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = "搜索名称或拼音首字母",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        }
    )
}

/** 其他道具汇总内容：已有数量可加减（重启后保留），需求用固定宽度对齐。 */
@Composable
private fun SolutionFooter(
    solutions: List<SolutionSummary>,
    onAddOwned: (PlantColor, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 8.dp)
    ) {
        solutions.forEach { summary ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SolutionIcon(color = summary.color, modifier = Modifier.size(26.dp))
                Text(
                    text = "${summary.color.displayName}色",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "已有",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                CounterButton(
                    text = "−",
                    enabled = summary.owned > 0,
                    label = "${summary.color.displayName}色道具减一",
                    onClick = { onAddOwned(summary.color, -1) },
                    size = 24.dp
                )
                Text(
                    text = summary.owned.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(32.dp)
                )
                CounterButton(
                    text = "+",
                    enabled = true,
                    label = "${summary.color.displayName}色道具加一",
                    onClick = { onAddOwned(summary.color, 1) },
                    size = 24.dp
                )
                Text(
                    text = "需求",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )
                Text(
                    text = summary.needed.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(36.dp)
                )
            }
        }
    }
}

/** 编辑进度弹窗：阶数用 Chip 选择（只有几个取值），碎片手动填写；不自动升阶、不扣碎片。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProgressDialog(
    card: PlantCardUi,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val entity = card.entity
    var tier by remember(card) { mutableStateOf(entity.currentTier) }
    var fragmentText by remember(card) { mutableStateOf(entity.currentFragments.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entity.title) },
        text = {
            Column {
                Text(
                    text = "当前 ${entity.currentTier}/${entity.maxTier} 阶，已有 ${entity.currentFragments} 碎片",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "当前阶数",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (0..entity.maxTier).forEach { option ->
                        FilterChip(
                            selected = tier == option,
                            onClick = { tier = option },
                            label = { Text("$option 阶") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = fragmentText,
                    onValueChange = { fragmentText = it.filter { c -> c.isDigit() } },
                    label = { Text("当前碎片") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (card.canPromote && tier < entity.maxTier) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "碎片已足够，请手动升阶。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    tier,
                    fragmentText.toIntOrNull() ?: entity.currentFragments
                )
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
