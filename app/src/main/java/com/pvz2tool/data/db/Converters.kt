package com.pvz2tool.data.db

import androidx.room.TypeConverter
import com.pvz2tool.data.entity.FifthTierStatus
import com.pvz2tool.data.entity.PlantColor

/** 把枚举以 name 形式存入 SQLite，读时还原。 */
class Converters {
    @TypeConverter
    fun fromColor(value: PlantColor): String = value.name

    @TypeConverter
    fun toColor(value: String): PlantColor = PlantColor.valueOf(value)

    @TypeConverter
    fun fromStatus(value: FifthTierStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): FifthTierStatus = FifthTierStatus.valueOf(value)
}
