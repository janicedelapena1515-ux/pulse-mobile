package com.example.domain.model

enum class StorageCategory(val label: String) {
    ALL("All"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    DOWNLOADS("Downloads"),
    LARGE_FILES("Large"),
    OLD_DOWNLOADS("Old Downloads"),
    DUPLICATES("Duplicates")
}

enum class StorageSortOption(val label: String) {
    SIZE_DESC("Largest First"),
    SIZE_ASC("Smallest First"),
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First")
}

data class ScannedFileItem(
    val uriString: String,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val mimeType: String,
    val category: StorageCategory,
    val folderDisplayName: String,
    val isOldDownload: Boolean = false,
    val isLargeFile: Boolean = false,
    val duplicateGroupId: String? = null,
    val contentHash: String? = null,
    val isAppCacheFile: Boolean = false
)

data class StorageScanSummary(
    val isScanning: Boolean = false,
    val scanProgressMessage: String = "",
    val scannedFolderCount: Int = 0,
    val totalFiles: Int = 0,
    val totalBytes: Long = 0L,
    val largeFilesCount: Int = 0,
    val largeFilesBytes: Long = 0L,
    val oldDownloadsCount: Int = 0,
    val oldDownloadsBytes: Long = 0L,
    val duplicateGroupsCount: Int = 0,
    val duplicateExtraBytes: Long = 0L,
    val appCacheBytes: Long = 0L,
    val lastScanMillis: Long = 0L
)

data class InspectedMediaInfo(
    val uriString: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val dateAddedMillis: Long?
)
