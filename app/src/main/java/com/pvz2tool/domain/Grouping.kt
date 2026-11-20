package com.pvz2tool.domain

import com.pvz2tool.data.entity.PlantColor

/** 三级颜色组：同品质的卡片聚在一起，橙色太多时可以单独收起。 */
data class CardColorGroup(
    val color: PlantColor,
    val cards: List<PlantCardUi>
)

/** 二级状态小节：进行中 / 已满待五阶 / 已完成，内部再按颜色分组。 */
data class CardSectionGroup(
    val section: DisplaySection,
    val colors: List<CardColorGroup>,
    /** 已完成小节整段调暗，视觉上弱化。 */
    val dimmed: Boolean
) {
    val count: Int get() = colors.sumOf { it.cards.size }
}

/** 一级大块：可探险 / 不可探险，内部按状态分小节。 */
data class CardBlock(
    val explorable: Boolean,
    val sections: List<CardSectionGroup>
) {
    val count: Int get() = sections.sumOf { it.count }
}

/** 分组与排序（参考文档 §8）：块 → 状态小节 → 颜色组。 */
object Grouping {

    // enum 顺序即展示顺序：白 → 绿 → 蓝 → 紫 → 橙
    private val COLOR_ORDER = PlantColor.entries

    // 可探险在前，不可探险在后
    private val EXPLORABLE_ORDER = listOf(true, false)

    private val SECTION_ORDER = listOf(
        DisplaySection.IN_PROGRESS,
        DisplaySection.PENDING_MAX,
        DisplaySection.COMPLETED
    )

    fun build(cards: List<PlantCardUi>): List<CardBlock> {
        val blocks = mutableListOf<CardBlock>()
        for (explorable in EXPLORABLE_ORDER) {
            val sections = mutableListOf<CardSectionGroup>()
            for (section in SECTION_ORDER) {
                val inSection = cards.filter {
                    it.entity.explorable == explorable && it.section == section
                }
                if (inSection.isEmpty()) continue // 空小节不输出

                // 颜色组内排序：阶数降序 → 剩余碎片升序
                val colors = COLOR_ORDER.mapNotNull { color ->
                    val inColor = inSection
                        .filter { it.entity.color == color }
                        .sortedWith(
                            compareByDescending<PlantCardUi> { it.entity.currentTier }
                                .thenBy { it.remainingFragments }
                        )
                    if (inColor.isEmpty()) null else CardColorGroup(color, inColor)
                }
                sections += CardSectionGroup(
                    section = section,
                    colors = colors,
                    dimmed = section == DisplaySection.COMPLETED
                )
            }
            if (sections.isNotEmpty()) {
                blocks += CardBlock(explorable = explorable, sections = sections)
            }
        }
        return blocks
    }

    /** 主页「其他道具」汇总块的折叠 id。 */
    const val SOLUTION_GROUP_ID: String = "home-solutions"

    /** 折叠状态用的稳定 id：只在本次运行内有效，但必须先于界面重绘保持不变。 */
    fun blockId(explorable: Boolean): String =
        if (explorable) "block-explorable" else "block-locked"

    fun sectionId(explorable: Boolean, section: DisplaySection): String =
        "section-${if (explorable) "explorable" else "locked"}-${section.name}"

    fun colorGroupId(explorable: Boolean, section: DisplaySection, color: PlantColor): String =
        "color-${if (explorable) "explorable" else "locked"}-${section.name}-${color.name}"

    /** 默认全部折叠：块、状态小节、颜色组和「其他道具」都收起，由用户自己逐层展开。 */
    val DEFAULT_COLLAPSED_IDS: Set<String> =
        EXPLORABLE_ORDER.flatMap { explorable ->
            listOf(blockId(explorable)) + SECTION_ORDER.flatMap { section ->
                listOf(sectionId(explorable, section)) +
                    COLOR_ORDER.map { colorGroupId(explorable, section, it) }
            }
        }.toSet() + SOLUTION_GROUP_ID
}
