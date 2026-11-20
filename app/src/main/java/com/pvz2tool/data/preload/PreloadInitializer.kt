package com.pvz2tool.data.preload

import android.content.Context
import com.pvz2tool.data.dao.PlantCardDao
import com.pvz2tool.data.io.PlantCardDto
import com.pvz2tool.data.io.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** 首次启动从 assets 灌入预置卡片数据（仅当表为空）。 */
class PreloadInitializer(
    context: Context,
    private val cardDao: PlantCardDao
) {
    private val assets = context.applicationContext.assets
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun initializeIfEmpty() = withContext(Dispatchers.IO) {
        if (cardDao.count() == 0) {
            val now = System.currentTimeMillis()
            val dtos = decodeCards(readAsset("preload_cards.json"))
            if (dtos.isNotEmpty()) {
                cardDao.insertAll(dtos.map { it.toEntity(now) })
            }
        }
    }

    private fun decodeCards(text: String): List<PlantCardDto> =
        runCatching {
            json.decodeFromString(ListSerializer(PlantCardDto.serializer()), text)
        }.getOrDefault(emptyList())

    private fun readAsset(name: String): String =
        runCatching {
            assets.open(name).bufferedReader().use { it.readText() }
        }.getOrDefault("[]")
}
