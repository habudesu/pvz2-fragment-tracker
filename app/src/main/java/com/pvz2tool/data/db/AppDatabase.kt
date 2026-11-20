package com.pvz2tool.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pvz2tool.data.dao.PlantCardDao
import com.pvz2tool.data.dao.SolutionDao
import com.pvz2tool.data.entity.PlantCardEntity
import com.pvz2tool.data.entity.SolutionOwnedEntity

@Database(
    entities = [
        PlantCardEntity::class,
        SolutionOwnedEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun plantCardDao(): PlantCardDao
    abstract fun solutionDao(): SolutionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pvz2tool.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
