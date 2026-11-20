package com.pvz2tool.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 某颜色「已有培养液」数量；颜色作主键，每种颜色一行。 */
@Entity(tableName = "solution_owned")
data class SolutionOwnedEntity(
    @PrimaryKey val color: PlantColor,
    val ownedCount: Int
)
