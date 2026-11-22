package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PulseAppContainer
import com.example.data.local.OptimizationLogEntity
import com.example.data.local.SafFolderEntity
import com.example.domain.model.InspectedMediaInfo
import com.example.domain.model.ScannedFileItem
import com.example.domain.model.StorageCategory
import com.example.domain.model.StorageScanSummary
import com.example.domain.model.StorageSortOption
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StorageViewModel(application: Application) : AndroidViewModel(application) {

    private val container = PulseAppContainer.get(application)
    private val scannerService = container.storageScannerService
    private val localRepo = container.localRepository
    private val prefsRepo = container.preferencesRepository
    private val telemetryService = container.telemetryService

    val safFolders: StateFlow<List<SafFolderEntity>> = localRepo.safFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val scanSummary: StateFlow<StorageScanSummary> = scannerService.summary
    val totalBytesRecovered: StateFlow<Long> = localRepo.totalBytesRecovered.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    private val _selectedCategory = MutableStateFlow(StorageCategory.ALL)
    val selectedCategory: StateFlow<StorageCategory> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(StorageSortOption.SIZE_DESC)
    val sortOption: StateFlow<StorageSortOption> = _sortOption.asStateFlow()

    private val _selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedUris: StateFlow<Set<String>> = _selectedUris.asStateFlow()

    private val _inspectedMedia = MutableStateFlow<InspectedMediaInfo?>(null)
    val inspectedMedia: StateFlow<InspectedMediaInfo?> = _inspectedMedia.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val visibleFiles: StateFlow<List<ScannedFileItem>> = combine(
        scannerService.scannedFiles,
        _selectedCategory,
        _sortOption
    ) { files, category, sort ->
        val filtered = when (category) {
            StorageCategory.ALL -> files
            StorageCategory.LARGE_FILES -> files.filter { it.isLargeFile }
            StorageCategory.OLD_DOWNLOADS -> files.filter { it.isOldDownload }
            StorageCategory.DUPLICATES -> files.filter { it.duplicateGroupId != null }
            else -> files.filter { it.category == category }
        }
        when (sort) {
            StorageSortOption.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            StorageSortOption.SIZE_ASC -> filtered.sortedBy { it.sizeBytes }
            StorageSortOption.DATE_DESC -> filtered.sortedByDescending { it.lastModifiedMillis }
            StorageSortOption.DATE_ASC -> filtered.sortedBy { it.lastModifiedMillis }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectCategory(category: StorageCategory) {
        _selectedCategory.value = category
        _selectedUris.value = emptySet()
    }

    fun selectSortOption(option: StorageSortOption) {
        _sortOption.value = option
    }

    fun toggleFileSelection(uriString: String) {
        val current = _selectedUris.value
        _selectedUris.value = if (uriString in current) current - uriString else current + uriString
    }

    fun selectAllVisible() {
        val all = visibleFiles.value.map { it.uriString }.toSet()
        _selectedUris.value = if (_selectedUris.value.size == all.size && all.isNotEmpty()) {
            emptySet()
        } else {
            all
        }
    }

    fun clearSelection() {
        _selectedUris.value = emptySet()
    }

    fun addSafFolder(treeUri: Uri) {
        viewModelScope.launch {
            val entity = scannerService.persistTreePermission(treeUri)
            if (entity == null) {
                _feedbackMessage.value = "Could not read selected folder."
                return@launch
            }
            localRepo.upsertSafFolder(entity)
            rescanAll()
            _feedbackMessage.value = "Added ${entity.displayName} via Storage Access Framework."
        }
    }

    fun removeSafFolder(uriString: String) {
        viewModelScope.launch {
            scannerService.releaseTreePermission(uriString)
            localRepo.removeSafFolder(uriString)
            rescanAll()
        }
    }

    fun rescanAll() {
        viewModelScope.launch {
            val folders = localRepo.safFolders.first()
            val prefs = prefsRepo.preferences.first()
            val summary = scannerService.scanAllSources(
                folders = folders,
                largeFileThresholdMb = prefs.largeFileThresholdMb,
                oldDownloadDays = prefs.oldDownloadDays,
                onFolderStatsUpdated = { localRepo.upsertSafFolder(it) }
            )
            telemetryService.refreshSnapshot(
                scannedClutterBytes = summary.duplicateExtraBytes + summary.oldDownloadsBytes
            )
        }
    }

    fun deleteSelectedFiles() {
        val selected = _selectedUris.value
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val targets = scannerService.scannedFiles.value.filter { it.uriString in selected }
            val beforeScore = telemetryService.telemetry.value.pulseScore.totalScore
            val (deletedCount, bytesRecovered) = scannerService.deleteFiles(targets)
            _selectedUris.value = emptySet()

            val after = telemetryService.refreshSnapshot(
                scannedClutterBytes = scannerService.summary.value.let {
                    it.duplicateExtraBytes + it.oldDownloadsBytes
                }
            )
            if (deletedCount > 0) {
                localRepo.logOptimization(
                    OptimizationLogEntity(
                        timestampMillis = System.currentTimeMillis(),
                        scoreBefore = beforeScore,
                        scoreAfter = after.pulseScore.totalScore,
                        bytesRecovered = bytesRecovered,
                        actionsSummary = "Deleted $deletedCount selected file(s) via Smart Cleaner",
                        honestNotes = "Recovered ${FormatUtils.formatBytes(bytesRecovered)} of storage."
                    )
                )
                _feedbackMessage.value =
                    "Deleted $deletedCount file(s), recovered ${FormatUtils.formatBytes(bytesRecovered)}."
            } else {
                _feedbackMessage.value = "Selected files could not be deleted (permission or OS lock)."
            }
        }
    }

    fun clearAppCache() {
        viewModelScope.launch {
            val beforeScore = telemetryService.telemetry.value.pulseScore.totalScore
            val bytes = scannerService.clearAppOwnCache()
            val after = telemetryService.refreshSnapshot()
            if (bytes > 0L) {
                localRepo.logOptimization(
                    OptimizationLogEntity(
                        timestampMillis = System.currentTimeMillis(),
                        scoreBefore = beforeScore,
                        scoreAfter = after.pulseScore.totalScore,
                        bytesRecovered = bytes,
                        actionsSummary = "Cleared Pulse app cache",
                        honestNotes = "Recovered ${FormatUtils.formatBytes(bytes)}."
                    )
                )
                _feedbackMessage.value = "Recovered ${FormatUtils.formatBytes(bytes)} from app cache."
            } else {
                _feedbackMessage.value = "App cache is already empty."
            }
        }
    }

    fun inspectPhotoPickerUri(uri: Uri) {
        viewModelScope.launch {
            _inspectedMedia.value = scannerService.inspectPickedMediaUri(uri)
        }
    }

    fun dismissInspectedMedia() {
        _inspectedMedia.value = null
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
