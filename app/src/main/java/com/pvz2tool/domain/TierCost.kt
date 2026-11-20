package com.pvz2tool.domain

import com.pvz2tool.data.entity.FifthTierStatus
import com.pvz2tool.data.entity.PlantCardEntity

/** 碎片算法（参考文档 §5）。 */
object TierCost {

    /** COST[i] = 从第 i 阶升到第 i+1 阶所需碎片；索引 0..4。 */
    val COST = intArrayOf(10, 30, 50, 50, 80)

    /**
     * 剩余碎片：
     * - 未到顶阶：Σ COST[currentTier..maxTier-1] − currentFragments。
     * - 已到顶阶：PENDING 还能为未来 5 阶囤料（80 − 已有）；HAS/NEVER 为 0。
     * - 负数一律钳 0（允许溢出）。
     */
    fun remaining(card: PlantCardEntity): Int {
        if (card.currentTier < card.maxTier) {
            var need = 0
            for (t in card.currentTier until card.maxTier) {
                need += COST[t]
            }
            return (need - card.currentFragments).coerceAtLeast(0)
        }
        return when (card.fifthTierStatus) {
            FifthTierStatus.PENDING_FIFTH ->
                (COST[4] - card.currentFragments).coerceAtLeast(0)
            else -> 0
        }
    }

    /** 碎片是否已够升下一阶（仅未到顶阶时有意义）。 */
    fun canPromote(card: PlantCardEntity): Boolean =
        card.currentTier < card.maxTier &&
            card.currentTier in COST.indices &&
            card.currentFragments >= COST[card.currentTier]
}
