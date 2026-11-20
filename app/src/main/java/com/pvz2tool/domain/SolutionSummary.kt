package com.pvz2tool.domain

import com.pvz2tool.data.entity.FifthTierStatus
import com.pvz2tool.data.entity.PlantCardEntity
import com.pvz2tool.data.entity.PlantColor
import com.pvz2tool.data.entity.SolutionOwnedEntity

/** 某颜色的培养液：已有 / 需求。 */
data class SolutionSummary(
    val color: PlantColor,
    val owned: Int,
    val needed: Int
)

/**
 * 需求 = HAS_FIFTH 且 currentTier<5 的卡数 × 10（0 阶/未获得也计入）。
 * 已有按颜色手填。
 */
fun buildSolution(
    cards: List<PlantCardEntity>,
    owned: List<SolutionOwnedEntity>
): List<SolutionSummary> {
    val ownedMap = owned.associate { it.color to it.ownedCount }
    return PlantColor.entries.map { color ->
        val count = cards.count {
            it.color == color &&
                it.fifthTierStatus == FifthTierStatus.HAS_FIFTH &&
                it.currentTier < 5
        }
        SolutionSummary(
            color = color,
            owned = ownedMap[color] ?: 0,
            needed = count * 10
        )
    }
}
