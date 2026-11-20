package com.pvz2tool.data.io

import com.pvz2tool.data.entity.FifthTierStatus
import com.pvz2tool.data.entity.PlantCardEntity
import com.pvz2tool.data.entity.PlantColor
import kotlinx.serialization.Serializable
import java.util.UUID

// ---- 导入/导出 DTO（参考文档 §10）----

@Serializable
data class SettingsDto(
    val themeMode: String = "SYSTEM",
    val colorScheme: String = "DEFAULT"
)

@Serializable
data class SolutionDto(
    val color: String,
    val ownedCount: Int
)

@Serializable
data class PlantCardDto(
    val stableKey: String? = null,
    val title: String,
    val imageUrl: String? = null,
    val color: String,
    val explorable: Boolean = false,
    val fifthTierStatus: String,
    val currentTier: Int = 0,
    val maxTier: Int,
    val currentFragments: Int = 0
)

@Serializable
data class ExportBundle(
    val schemaVersion: Int = 1,
    val settings: SettingsDto = SettingsDto(),
    val solutions: List<SolutionDto> = emptyList(),
    val cards: List<PlantCardDto> = emptyList()
)

// ---- 实体 <-> DTO 映射 ----

fun PlantCardEntity.toDto(): PlantCardDto = PlantCardDto(
    stableKey = stableKey,
    title = title,
    imageUrl = imageUrl,
    color = color.name,
    explorable = explorable,
    fifthTierStatus = fifthTierStatus.name,
    currentTier = currentTier,
    maxTier = maxTier,
    currentFragments = currentFragments
)

/**
 * DTO → 实体。导入的 JSON 与预置数据都可能被手动改过，
 * 这里以 maxTier 为准反推 fifthTierStatus（5→HAS、4→PENDING、1/3→NEVER），
 * 既保证两者永远一致，也避免非法 maxTier（例如误写成 44）让 TierCost 越界崩溃。
 */
fun PlantCardDto.toEntity(now: Long): PlantCardEntity {
    // 只保留合法封顶阶数：>=5 视作 5 阶，其余非法值兜底成 1 阶
    val safeMaxTier = when {
        maxTier >= 5 -> 5
        maxTier == 4 -> 4
        maxTier == 3 -> 3
        else -> 1
    }
    val status = when (safeMaxTier) {
        5 -> FifthTierStatus.HAS_FIFTH
        4 -> FifthTierStatus.PENDING_FIFTH
        else -> FifthTierStatus.NEVER_FIFTH
    }
    return PlantCardEntity(
        stableKey = stableKey ?: UUID.randomUUID().toString(),
        title = title,
        imageUrl = imageUrl,
        color = PlantColor.valueOf(color),
        explorable = explorable,
        fifthTierStatus = status,
        currentTier = currentTier.coerceIn(0, safeMaxTier),
        maxTier = safeMaxTier,
        currentFragments = currentFragments.coerceAtLeast(0),
        createdAt = now,
        updatedAt = now
    )
}
