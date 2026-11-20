package com.pvz2tool.data.preload

import android.content.Context
import com.pvz2tool.data.dao.PlantCardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 打包进 assets 的卡片图片。
 * 图片按原文件名（各不相同的 MD5）保存，网络地址能对应上就换成本地相对路径，
 * 这样老安装里的 CDN 地址和旧备份都能落到本地，不再依赖网络。
 */
class BundledImages(context: Context) {

    private val assets = context.applicationContext.assets

    private val cardFileNames: Set<String> by lazy {
        assets.list(CARD_DIR)?.toSet().orEmpty()
    }

    /** 网络地址对应到打包图片时返回 "cards/xxx.webp"，否则原样返回。 */
    fun localize(url: String?): String? {
        if (url.isNullOrBlank()) return url
        if (!url.startsWith("http", ignoreCase = true)) return url
        val fileName = url.substringBefore('?').substringAfterLast('/')
        val stem = fileName.substringBeforeLast('.', fileName)
        // 打包时统一转成 webp，用去扩展名的文件名匹配
        val bundled = "$stem.webp"
        return if (bundled in cardFileNames) "$CARD_DIR/$bundled" else url
    }

    /** 把数据库里仍指向 CDN 的图片地址换成打包路径；可重复执行。 */
    suspend fun migrateStoredUrls(cardDao: PlantCardDao) = withContext(Dispatchers.IO) {
        cardDao.getAll().forEach { card ->
            val current = card.imageUrl ?: return@forEach
            val localized = localize(current) ?: return@forEach
            if (localized != current) {
                cardDao.updateImageUrl(card.id, localized)
            }
        }
    }

    private companion object {
        const val CARD_DIR = "cards"
    }
}
