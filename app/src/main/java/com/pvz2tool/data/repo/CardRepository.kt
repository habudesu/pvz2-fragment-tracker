package com.pvz2tool.data.repo

import com.pvz2tool.data.dao.PlantCardDao
import com.pvz2tool.data.entity.FifthTierStatus
import com.pvz2tool.data.entity.PlantCardEntity
import com.pvz2tool.data.entity.PlantColor
import com.pvz2tool.data.entity.orderRank
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 卡片仓库：封装 DAO，集中做校验与业务规则。 */
class CardRepository(private val dao: PlantCardDao) {

    fun observeAll(): Flow<List<PlantCardEntity>> = dao.observeAll()

    /** 加减碎片：走原子 SQL，防丢更新。 */
    suspend fun addFragments(id: Long, delta: Int) {
        dao.addFragments(id, delta, System.currentTimeMillis())
    }

    /**
     * 保存进度：currentTier 钳到 [0, maxTier]，碎片下限 0。
     * 不自动升阶、不扣碎片。
     */
    suspend fun updateProgress(card: PlantCardEntity, newTier: Int, newFragments: Int) {
        val safeTier = newTier.coerceIn(0, card.maxTier)
        val safeFragments = newFragments.coerceAtLeast(0)
        dao.update(
            card.copy(
                currentTier = safeTier,
                currentFragments = safeFragments,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * 编辑卡片基础信息。
     * maxTier 规则：HAS=5、PENDING=4、NEVER=1或3。
     * 状态只能单向提升（NEVER → PENDING → HAS）；同状态内 maxTier 只升不降。
     */
    suspend fun updateCardInfo(
        card: PlantCardEntity,
        title: String,
        imageUrl: String?,
        color: PlantColor,
        explorable: Boolean,
        status: FifthTierStatus,
        requestedMaxTier: Int
    ) {
        // 防御：不允许把状态改回去（UI 已限制，这里再兜一层）
        val effectiveStatus = if (status.orderRank < card.fifthTierStatus.orderRank) {
            card.fifthTierStatus
        } else {
            status
        }
        val resolved = resolveMaxForStatus(effectiveStatus, requestedMaxTier)
        val newMax = if (effectiveStatus == card.fifthTierStatus) {
            maxOf(card.maxTier, resolved) // 同状态只升不降
        } else {
            resolved // 正向升级（如 PENDING→HAS 得 5）
        }
        val safeTier = card.currentTier.coerceIn(0, newMax)
        dao.update(
            card.copy(
                title = title,
                imageUrl = imageUrl,
                color = color,
                explorable = explorable,
                fifthTierStatus = effectiveStatus,
                maxTier = newMax,
                currentTier = safeTier,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** 新增计数器（用于新出植物）。 */
    suspend fun addCard(
        title: String,
        imageUrl: String?,
        color: PlantColor,
        explorable: Boolean,
        status: FifthTierStatus,
        requestedMaxTier: Int
    ) {
        val now = System.currentTimeMillis()
        val maxTier = resolveMaxForStatus(status, requestedMaxTier)
        dao.insert(
            PlantCardEntity(
                stableKey = UUID.randomUUID().toString(),
                title = title,
                imageUrl = imageUrl,
                color = color,
                explorable = explorable,
                fifthTierStatus = status,
                currentTier = 0,
                maxTier = maxTier,
                currentFragments = 0,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    /** 按状态解析合法的 maxTier。 */
    private fun resolveMaxForStatus(status: FifthTierStatus, requested: Int): Int =
        when (status) {
            FifthTierStatus.HAS_FIFTH -> 5
            FifthTierStatus.PENDING_FIFTH -> 4
            FifthTierStatus.NEVER_FIFTH -> if (requested <= 1) 1 else 3
        }
}
