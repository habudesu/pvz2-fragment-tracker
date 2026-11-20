package com.pvz2tool.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pvz2tool.data.entity.PlantCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantCardDao {

    /** 订阅全部卡片；数据变化自动推送。 */
    @Query("SELECT * FROM plant_card")
    fun observeAll(): Flow<List<PlantCardEntity>>

    @Query("SELECT * FROM plant_card")
    suspend fun getAll(): List<PlantCardEntity>

    @Query("SELECT COUNT(*) FROM plant_card")
    suspend fun count(): Int

    @Insert
    suspend fun insert(card: PlantCardEntity): Long

    @Insert
    suspend fun insertAll(cards: List<PlantCardEntity>)

    @Update
    suspend fun update(card: PlantCardEntity)

    @Query("SELECT * FROM plant_card WHERE stableKey = :key LIMIT 1")
    suspend fun findByStableKey(key: String): PlantCardEntity?

    @Query("SELECT * FROM plant_card WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): PlantCardEntity?

    /**
     * 防丢更新的原子 SQL：每次点击都在数据库层 +delta，并保证不小于 0。
     * 快速连点也不会丢更新。
     */
    @Query(
        "UPDATE plant_card " +
            "SET currentFragments = MAX(0, currentFragments + :delta), updatedAt = :now " +
            "WHERE id = :id"
    )
    suspend fun addFragments(id: Long, delta: Int, now: Long)

    /** 只改图片地址（打包图片迁移用），不碰进度和时间戳。 */
    @Query("UPDATE plant_card SET imageUrl = :url WHERE id = :id")
    suspend fun updateImageUrl(id: Long, url: String)

    @Query("DELETE FROM plant_card")
    suspend fun deleteAll()
}
