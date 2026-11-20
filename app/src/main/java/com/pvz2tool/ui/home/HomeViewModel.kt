package com.pvz2tool.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pvz2tool.AppContainer
import com.pvz2tool.PvzApplication
import com.pvz2tool.data.entity.FifthTierStatus
import com.pvz2tool.data.entity.PlantCardEntity
import com.pvz2tool.data.entity.PlantColor
import com.pvz2tool.data.entity.SolutionOwnedEntity
import com.pvz2tool.domain.CardBlock
import com.pvz2tool.domain.CardColorGroup
import com.pvz2tool.domain.CardSectionGroup
import com.pvz2tool.domain.DisplaySection
import com.pvz2tool.domain.Grouping
import com.pvz2tool.domain.PinyinInitials
import com.pvz2tool.domain.PlantCardUi
import com.pvz2tool.domain.SolutionSummary
import com.pvz2tool.domain.buildSolution
import com.pvz2tool.domain.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 主页界面状态。blocks 已经是分好块、排好序的，UI 直接照着渲染。 */
data class HomeUiState(
    val loading: Boolean = true,
    val blocks: List<CardBlock> = emptyList(),
    val solutions: List<SolutionSummary> = emptyList(),
    /** 被折叠的块/小节/颜色组 id；只在内存里，每次打开应用恢复默认。 */
    val collapsedIds: Set<String> = emptySet(),
    /** 是否显示首次启动引导。 */
    val showGuide: Boolean = false,
    /** 搜索关键字，空串表示不筛选。 */
    val query: String = ""
)

/**
 * 冻结的展示顺序：只记卡片 id、小节与颜色组归属。
 * 加减碎片、改颜色或状态时卡片都不会跳位，只有「刷新排序」或重新进入主页时才重建。
 */
private data class FrozenColor(val color: PlantColor, val cardIds: List<Long>)
private data class FrozenSection(val section: DisplaySection, val colors: List<FrozenColor>)
private data class FrozenBlock(val explorable: Boolean, val sections: List<FrozenSection>)

/**
 * 主页 ViewModel（参考文档 §7.2）。
 *
 * 数据流：卡片表 + 设置 + 其他道具表 → combine → 派生分组 → StateFlow。
 * 加减碎片走 DAO 原子 SQL；保存进度做边界校验。
 */
class HomeViewModel(container: AppContainer) : ViewModel() {

    private val cardRepository = container.cardRepository
    private val settingsRepository = container.settingsRepository
    private val solutionDao = container.solutionDao
    private val pinyin = container.pinyinInitials

    /** 道具数量的读改写需要串行，避免连点时后一次读到旧值。 */
    private val solutionMutex = Mutex()

    /** 冻结的展示顺序；null/空表示还没排过，按实时结果渲染（首启数据到达前）。 */
    private val layout = MutableStateFlow<List<FrozenBlock>?>(null)

    /** 搜索关键字。只在内存里筛选，不改动冻结顺序，也不写 DataStore。 */
    private val query = MutableStateFlow("")

    /**
     * 折叠状态只存在内存里：每次打开应用都回到全折叠，由用户自己逐层展开，
     * 本次使用期间的展开/收起在退出前一直有效。
     */
    private val collapsed = MutableStateFlow(Grouping.DEFAULT_COLLAPSED_IDS)

    val uiState: StateFlow<HomeUiState> = combine(
        cardRepository.observeAll(),
        settingsRepository.settings,
        solutionDao.observeAll(),
        layout,
        query
    ) { cards, settings, owned, frozen, keyword ->
        // 派生字段（剩余碎片、分组状态、是否可升阶）都在内存里算，不落库
        val uiCards = cards.map { it.toUi() }
        val baseBlocks = if (frozen.isNullOrEmpty()) {
            Grouping.build(uiCards)
        } else {
            resolveBlocks(frozen, uiCards)
        }

        HomeUiState(
            loading = false,
            blocks = filterBlocks(baseBlocks, keyword, pinyin),
            solutions = buildSolution(cards, owned),
            showGuide = !settings.guideShown,
            query = keyword
        )
    }.combine(collapsed) { state, ids ->
        state.copy(collapsedIds = ids)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    /**
     * 按当前阶数/碎片重新分组排序。
     * 进入主页时和点「刷新排序」时调用；平时加减碎片不改位置，避免手指下的卡片跳走。
     */
    fun refreshLayout() {
        viewModelScope.launch {
            val cards = cardRepository.observeAll().first { it.isNotEmpty() }
            val sorted = Grouping.build(cards.map { it.toUi() })
            layout.value = sorted.map { block ->
                FrozenBlock(
                    explorable = block.explorable,
                    sections = block.sections.map { section ->
                        FrozenSection(
                            section = section.section,
                            colors = section.colors.map { colorGroup ->
                                FrozenColor(
                                    color = colorGroup.color,
                                    cardIds = colorGroup.cards.map { it.entity.id }
                                )
                            }
                        )
                    }
                )
            }
        }
    }

    /** 加/减碎片：delta 可正可负，数据库层保证不小于 0。 */
    fun addFragments(cardId: Long, delta: Int) {
        viewModelScope.launch { cardRepository.addFragments(cardId, delta) }
    }

    /** 加/减某颜色道具的已有数量：无记录时按 0 起算，减到 0 为止。 */
    fun addSolution(color: PlantColor, delta: Int) {
        viewModelScope.launch {
            // 连点会并发读写同一行，串行化避免互相覆盖
            solutionMutex.withLock {
                val current = solutionDao.getAll().firstOrNull { it.color == color }?.ownedCount ?: 0
                solutionDao.upsert(SolutionOwnedEntity(color, (current + delta).coerceAtLeast(0)))
            }
        }
    }

    /** 手动编辑阶数与碎片（保存时钳到合法范围，不自动升阶）。 */
    fun saveProgress(card: PlantCardEntity, tier: Int, fragments: Int) {
        viewModelScope.launch { cardRepository.updateProgress(card, tier, fragments) }
    }

    /** 编辑卡片基础信息（状态、品质、是否探险等）。 */
    fun saveCardInfo(
        card: PlantCardEntity,
        title: String,
        imageUrl: String?,
        color: PlantColor,
        explorable: Boolean,
        status: FifthTierStatus,
        maxTier: Int
    ) {
        viewModelScope.launch {
            cardRepository.updateCardInfo(
                card, title, imageUrl, color, explorable, status, maxTier
            )
        }
    }

    /** 折叠/展开主页上的某个块、小节或颜色组：只改内存，重启应用后恢复默认。 */
    fun toggleCollapsed(id: String) {
        val current = collapsed.value
        collapsed.value = if (id in current) current - id else current + id
    }

    /** 更新搜索关键字；只影响渲染，不重排也不持久化。 */
    fun setQuery(value: String) {
        query.value = value
    }

    /** 关闭首次引导（无论「开始」还是「跳过」都只展示一次）。 */
    fun dismissGuide() {
        viewModelScope.launch { settingsRepository.markGuideShown() }
    }

    companion object {
        /** 从 Application 的依赖容器创建 ViewModel（无 DI 框架时的标准写法）。 */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PvzApplication
                HomeViewModel(app.container)
            }
        }
    }
}

/**
 * 标题包含关键字或拼音首字母命中即匹配；空小节/空块不再输出。
 * 关键字里带标点会被忽略，所以「阿开木木」「akmm」都能搜到「阿.开.木.木.」。
 */
private fun filterBlocks(
    blocks: List<CardBlock>,
    keyword: String,
    pinyin: PinyinInitials
): List<CardBlock> {
    val q = pinyin.normalize(keyword)
    if (q.isEmpty()) return blocks
    // 只有两个字符以上的纯字母数字才走首字母匹配：
    // 单字中文（如「向」）和不带拼音意图的单字母不会误伤一片卡片
    val matchInitials = q.length >= 2 && q.none { it.code in 0x4E00..0x9FFF }
    return blocks.mapNotNull { block ->
        val sections = block.sections.mapNotNull { sectionGroup ->
            val colors = sectionGroup.colors.mapNotNull { colorGroup ->
                val matched = colorGroup.cards.filter { card ->
                    val title = card.entity.title
                    pinyin.normalize(title).contains(q) ||
                        (matchInitials && pinyin.of(title).contains(q))
                }
                if (matched.isEmpty()) null else colorGroup.copy(cards = matched)
            }
            if (colors.isEmpty()) null else sectionGroup.copy(colors = colors)
        }
        if (sections.isEmpty()) null else block.copy(sections = sections)
    }
}

/** 用冻结的顺序渲染，卡片内容（碎片、阶数、颜色）仍取实时值。 */
private fun resolveBlocks(
    frozen: List<FrozenBlock>,
    cards: List<PlantCardUi>
): List<CardBlock> {
    val byId = cards.associateBy { it.entity.id }
    return frozen.mapNotNull { block ->
        val sections = block.sections.mapNotNull { frozenSection ->
            val colors = frozenSection.colors.mapNotNull { frozenColor ->
                val inColor = frozenColor.cardIds.mapNotNull { byId[it] }
                if (inColor.isEmpty()) null else CardColorGroup(frozenColor.color, inColor)
            }
            if (colors.isEmpty()) {
                null
            } else {
                CardSectionGroup(
                    section = frozenSection.section,
                    colors = colors,
                    dimmed = frozenSection.section == DisplaySection.COMPLETED
                )
            }
        }
        if (sections.isEmpty()) null else CardBlock(block.explorable, sections)
    }
}
