package com.pvz2tool.data.io

import com.pvz2tool.data.dao.PlantCardDao
import com.pvz2tool.data.dao.SolutionDao
import com.pvz2tool.data.entity.PlantCardEntity
import com.pvz2tool.data.entity.PlantColor
import com.pvz2tool.data.entity.SolutionOwnedEntity
import com.pvz2tool.data.repo.ColorSchemeChoice
import com.pvz2tool.data.repo.SettingsRepository
import com.pvz2tool.data.repo.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

enum class ImportMode { OVERWRITE, MERGE }

/** 导入结果汇总，用于导入后提示新增/覆盖/保留的数量。 */
data class ImportResult(
    val newCards: Int = 0,
    val overwrittenCards: Int = 0,
    val keptCards: Int = 0
)

/** 导入/导出与合并（参考文档 §10）。 */
class ImportExportManager(
    private val cardDao: PlantCardDao,
    private val solutionDao: SolutionDao,
    private val settingsRepository: SettingsRepository,
    /** 把网络图片地址换成打包图片路径；由 BundledImages 提供。 */
    private val localizeImage: (String?) -> String? = { it }
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** 汇总当前全部数据为一段 JSON。 */
    suspend fun exportJson(): String {
        val s = settingsRepository.settings.first()
        val bundle = ExportBundle(
            settings = SettingsDto(
                themeMode = s.themeMode.name,
                colorScheme = s.colorScheme.name
            ),
            solutions = solutionDao.getAll().map { SolutionDto(it.color.name, it.ownedCount) },
            cards = cardDao.getAll().map { it.toDto() }
        )
        return json.encodeToString(ExportBundle.serializer(), bundle)
    }

    /**
     * 导入 JSON。
     * OVERWRITE：先清空再导入。
     * MERGE：按 stableKey（未命中再按标题）去重，冲突交由 onConflict 决定是否覆盖。
     * 返回新增/覆盖/保留的卡片数量，供导入后提示。
     */
    suspend fun importJson(
        text: String,
        mode: ImportMode,
        onConflict: suspend (local: PlantCardEntity, remote: PlantCardEntity) -> Boolean = { _, _ -> true }
    ): ImportResult {
        val bundle = json.decodeFromString(ExportBundle.serializer(), text)
        val now = System.currentTimeMillis()

        if (mode == ImportMode.OVERWRITE) {
            cardDao.deleteAll()
            solutionDao.deleteAll()
        }

        applySettings(bundle.settings)

        bundle.solutions.forEach { dto ->
            val color = runCatching { PlantColor.valueOf(dto.color) }.getOrNull() ?: return@forEach
            solutionDao.upsert(SolutionOwnedEntity(color, dto.ownedCount.coerceAtLeast(0)))
        }

        var newCards = 0
        var overwritten = 0
        var kept = 0
        bundle.cards.forEach { dto ->
            // 旧备份里是 CDN 地址，导入时就换成打包路径，避免覆盖后又要联网
            val incoming = dto.copy(imageUrl = localizeImage(dto.imageUrl)).toEntity(now)
            val byKey = cardDao.findByStableKey(incoming.stableKey)
            val byTitle = if (byKey == null) cardDao.findByTitle(incoming.title) else null
            val local = byKey ?: byTitle
            if (local != null) {
                if (onConflict(local, incoming)) {
                    cardDao.update(
                        incoming.copy(
                            id = local.id,
                            createdAt = local.createdAt,
                            updatedAt = now
                        )
                    )
                    overwritten++
                } else {
                    kept++
                }
            } else {
                cardDao.insert(incoming.copy(id = 0))
                newCards++
            }
        }
        return ImportResult(newCards = newCards, overwrittenCards = overwritten, keptCards = kept)
    }

    private suspend fun applySettings(dto: SettingsDto) {
        runCatching { ThemeMode.valueOf(dto.themeMode) }.getOrNull()
            ?.let { settingsRepository.setThemeMode(it) }
        runCatching { ColorSchemeChoice.valueOf(dto.colorScheme) }.getOrNull()
            ?.let { settingsRepository.setColorScheme(it) }
    }
}
