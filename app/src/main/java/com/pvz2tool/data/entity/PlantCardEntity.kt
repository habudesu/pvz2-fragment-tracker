package com.pvz2tool.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 植物卡片实体。
 * - stableKey：跨设备导入合并的唯一键。
 * - currentTier 必须 <= maxTier。
 * - maxTier 只升不降；HAS=5、PENDING=4、NEVER=1 或 3。
 */
@Entity(
    tableName = "plant_card",
    indices = [Index(value = ["stableKey"], unique = true)]
)
data class PlantCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableKey: String,
    val title: String,
    val imageUrl: String? = null,
    val color: PlantColor,
    val explorable: Boolean,
    val fifthTierStatus: FifthTierStatus,
    val currentTier: Int,
    val maxTier: Int,
    val currentFragments: Int,
    val createdAt: Long,
    val updatedAt: Long
)
