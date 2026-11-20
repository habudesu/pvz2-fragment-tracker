package com.pvz2tool.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pvz2tool.AppContainer
import com.pvz2tool.PvzApplication
import com.pvz2tool.data.entity.FifthTierStatus
import com.pvz2tool.data.entity.PlantCardEntity
import com.pvz2tool.data.entity.PlantColor
import com.pvz2tool.data.io.ImportMode
import com.pvz2tool.data.repo.AppSettings
import com.pvz2tool.data.repo.ColorSchemeChoice
import com.pvz2tool.data.repo.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 设置页状态：外观设置、全部卡片（管理用）。 */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val cards: List<PlantCardEntity> = emptyList()
)

/**
 * 设置页 ViewModel（参考文档 §14）。
 * 读写 DataStore、管理计数器（增删改）、导入导出。
 * 其他道具的已有数量在主页直接加减，设置页不再提供。
 */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val cardRepository = container.cardRepository
    private val settingsRepository = container.settingsRepository

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        cardRepository.observeAll()
    ) { settings, cards ->
        SettingsUiState(
            settings = settings,
            cards = cards.sortedBy { it.title }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setColorScheme(scheme: ColorSchemeChoice) {
        viewModelScope.launch { settingsRepository.setColorScheme(scheme) }
    }

    /** 折叠/展开设置页的某个栏目（持久化在 DataStore）。 */
    fun toggleCollapsed(id: String) {
        viewModelScope.launch { settingsRepository.toggleCollapsedGroup(id) }
    }

    fun addCard(
        title: String,
        imageUrl: String?,
        color: PlantColor,
        explorable: Boolean,
        status: FifthTierStatus,
        maxTier: Int
    ) {
        viewModelScope.launch {
            cardRepository.addCard(title, imageUrl, color, explorable, status, maxTier)
        }
    }

    fun updateCard(
        card: PlantCardEntity,
        title: String,
        imageUrl: String?,
        color: PlantColor,
        explorable: Boolean,
        status: FifthTierStatus,
        maxTier: Int
    ) {
        viewModelScope.launch {
            cardRepository.updateCardInfo(card, title, imageUrl, color, explorable, status, maxTier)
        }
    }

    /** 导出：成功返回 JSON 文本，失败返回以「导出失败」开头的提示。 */
    fun exportJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { container.importExportManager.exportJson() }
                .getOrElse { "导出失败：${it.message ?: "未知错误"}" }
            onResult(result)
        }
    }

    /**
     * 导入：结束后通过 onResult 回一条汇总提示。
     * overwriteConflicts 决定合并模式下遇到同 stableKey/同名卡片时是否覆盖本地。
     */
    fun importJson(
        text: String,
        mode: ImportMode,
        overwriteConflicts: Boolean,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val message = runCatching {
                container.importExportManager.importJson(
                    text = text,
                    mode = mode,
                    onConflict = { _, _ -> overwriteConflicts }
                )
            }.fold(
                onSuccess = { r ->
                    "导入完成：新增 ${r.newCards} 张，覆盖 ${r.overwrittenCards} 张，保留 ${r.keptCards} 张"
                },
                onFailure = { "导入失败：${it.message ?: "格式不正确"}" }
            )
            onResult(message)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PvzApplication
                SettingsViewModel(app.container)
            }
        }
    }
}
