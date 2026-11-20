package com.pvz2tool.domain

import com.pvz2tool.data.entity.FifthTierStatus
import com.pvz2tool.data.entity.PlantCardEntity

/** 带派生字段的界面模型；派生值实时计算，不落库。 */
data class PlantCardUi(
    val entity: PlantCardEntity,
    val remainingFragments: Int,
    val section: DisplaySection,
    val canPromote: Boolean
) {
    val isMaxed: Boolean get() = entity.currentTier == entity.maxTier
}

fun PlantCardEntity.toUi(): PlantCardUi {
    val section = when {
        currentTier >= maxTier -> when (fifthTierStatus) {
            FifthTierStatus.PENDING_FIFTH -> DisplaySection.PENDING_MAX
            else -> DisplaySection.COMPLETED
        }
        else -> DisplaySection.IN_PROGRESS
    }
    return PlantCardUi(
        entity = this,
        remainingFragments = TierCost.remaining(this),
        section = section,
        canPromote = TierCost.canPromote(this)
    )
}
