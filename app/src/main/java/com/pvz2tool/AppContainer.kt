package com.pvz2tool

import android.content.Context
import com.pvz2tool.data.db.AppDatabase
import com.pvz2tool.data.io.ImportExportManager
import com.pvz2tool.data.preload.BundledImages
import com.pvz2tool.data.preload.PreloadInitializer
import com.pvz2tool.data.repo.CardRepository
import com.pvz2tool.data.repo.SettingsRepository
import com.pvz2tool.domain.PinyinInitials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 极简依赖容器（无 DI 框架）。 */
class AppContainer(context: Context) {

    val database: AppDatabase = AppDatabase.get(context)
    val cardRepository = CardRepository(database.plantCardDao())
    val settingsRepository = SettingsRepository(context)
    val solutionDao = database.solutionDao()
    val bundledImages = BundledImages(context)

    /** 首字母检索表（21KB），启动时读一次。 */
    val pinyinInitials = PinyinInitials(
        context.applicationContext.assets.open("pinyin_initials.bin").use { it.readBytes() }
    )
    val importExportManager = ImportExportManager(
        cardDao = database.plantCardDao(),
        solutionDao = database.solutionDao(),
        settingsRepository = settingsRepository,
        localizeImage = bundledImages::localize
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        val initializer = PreloadInitializer(
            context = context,
            cardDao = database.plantCardDao()
        )
        scope.launch {
            runCatching { initializer.initializeIfEmpty() }
        }
        // 老安装已有折叠集合，补上新版默认折叠的栏目（只执行一次）
        scope.launch {
            runCatching { settingsRepository.applyNewDefaultCollapsedGroups() }
        }
        // 把库里仍指向 CDN 的图片地址换成打包路径（幂等）
        scope.launch {
            runCatching { bundledImages.migrateStoredUrls(database.plantCardDao()) }
        }
    }
}
