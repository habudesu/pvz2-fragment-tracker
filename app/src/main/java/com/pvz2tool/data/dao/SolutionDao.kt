package com.pvz2tool.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pvz2tool.data.entity.SolutionOwnedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SolutionDao {

    @Query("SELECT * FROM solution_owned")
    fun observeAll(): Flow<List<SolutionOwnedEntity>>

    @Query("SELECT * FROM solution_owned")
    suspend fun getAll(): List<SolutionOwnedEntity>

    /** 颜色为主键，重复则覆盖。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SolutionOwnedEntity)

    @Query("DELETE FROM solution_owned")
    suspend fun deleteAll()
}
