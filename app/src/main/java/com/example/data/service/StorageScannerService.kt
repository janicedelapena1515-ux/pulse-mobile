package com.example.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.SafFolderEntity
import com.example.domain.model.InspectedMediaInfo
import com.example.domain.model.ScannedFileItem
import com.example.domain.model.StorageCategory
import com.example.domain.model.StorageScanSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class StorageScannerService(private val context: Context) {

    private val _scannedFiles = MutableStateFlow<List<ScannedFileItem>>(emptyList())
    val scannedFiles: StateFlow<List<ScannedFileItem>> = _scannedFiles.asStateFlow()

    private val _summary = MutableStateFlow(StorageScanSummary())
    val summary: StateFlow<StorageScanSummary> = _summary.asStateFlow()

    fun persistTreePermission(treeUri: Uri): SafFolderEntity? {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        }
        val doc = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val displayName = doc.name?.takeIf { it.isNotBlank() } ?: treeUri.lastPathSegment ?: "Selected Folder"
        return SafFolderEntity(
            uriString = treeUri.toString(),
            displayName = displayName
        )
    }

    fun releaseTreePermission(uriString: String) {
        runCatching {
            val uri = Uri.parse(uriString)
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(uri, flags)
        }
    }

    suspend fun scanAllSources(
        folders: List<SafFolderEntity>,
        largeFileThresholdMb: Int,
        oldDownloadDays: Int,
        onFolderStatsUpdated: suspend (SafFolderEntity) -> Unit
    ): StorageScanSummary = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val largeBytesThreshold = largeFileThresholdMb.toLong() * 1024L * 1024L
        val oldAgeMillis = oldDownloadDays.toLong() * 24L * 60L * 60L * 1000L

        _summary.value = _summary.value.copy(
            isScanning = true,
            scanProgressMessage = "Inspecting app cache directories..."
        )

        val rawItems = mutableListOf<ScannedFileItem>()

        // 1. Scan Pulse Mobile's own safe cache directories
        val cacheDirs = listOfNotNull(context.cacheDir, context.codeCacheDir, context.externalCacheDir)
        var appCacheTotal = 0L
        for (dir in cacheDirs) {
            if (!dir.exists()) continue
            dir.walkTopDown().forEach { file ->
                if (file.isFile && file.length() > 0L) {
                    appCacheTotal += file.length()
                    rawItems += ScannedFileItem(
                        uriString = file.absolutePath,
                        name = file.name,
                        sizeBytes = file.length(),
                        lastModifiedMillis = file.lastModified(),
                        mimeType = "application/octet-stream",
                        category = StorageCategory.DOCUMENTS,
                        folderDisplayName = "Pulse App Cache",
                        isLargeFile = file.length() >= largeBytesThreshold,
                        isAppCacheFile = true
                    )
                }
            }
        }

        // 2. Scan user-authorized Storage Access Framework (SAF) folders
        for (folder in folders) {
            _summary.value = _summary.value.copy(
                scanProgressMessage = "Scanning ${folder.displayName}..."
            )
            val uri = Uri.parse(folder.uriString)
            val rootDoc = DocumentFile.fromTreeUri(context, uri)
            if (rootDoc == null || !rootDoc.exists() || !rootDoc.canRead()) continue

            val folderItems = mutableListOf<ScannedFileItem>()
            traverseDocumentTree(
                doc = rootDoc,
                folderLabel = folder.displayName,
                isDownloadFolder = folder.displayName.contains("download", ignoreCase = true),
                largeThreshold = largeBytesThreshold,
                oldAgeMillis = oldAgeMillis,
                nowMillis = now,
                out = folderItems,
                depth = 0
            )
            rawItems.addAll(folderItems)

            onFolderStatsUpdated(
                folder.copy(
                    lastScannedBytes = folderItems.sumOf { it.sizeBytes },
                    lastScannedFileCount = folderItems.size
                )
            )
        }

        // 3. Real duplicate detection via size-grouping + SHA-256 content hash
        _summary.value = _summary.value.copy(
            scanProgressMessage = "Analyzing content hashes for duplicates..."
        )
        val sizeGroups = rawItems.filter { it.sizeBytes > 1024L && !it.isAppCacheFile }
            .groupBy { it.sizeBytes }
            .filter { it.value.size > 1 }

        val hashByUri = mutableMapOf<String, String>()
        for ((_, candidates) in sizeGroups) {
            for (item in candidates) {
                val hash = computeSha256(item)
                if (hash != null) {
                    hashByUri[item.uriString] = hash
                }
            }
        }

        val duplicateHashGroups = hashByUri.entries
            .groupBy({ it.value }, { it.key })
            .filter { it.value.size > 1 }

        val finalItems = rawItems.map { item ->
            val hash = hashByUri[item.uriString]
            val isDup = hash != null && duplicateHashGroups.containsKey(hash)
            item.copy(
                contentHash = hash,
                duplicateGroupId = if (isDup) hash?.take(12) else null
            )
        }

        var duplicateExtraBytes = 0L
        for ((_, uris) in duplicateHashGroups) {
            val sizes = uris.mapNotNull { u -> finalItems.firstOrNull { it.uriString == u }?.sizeBytes }
            if (sizes.size > 1) {
                duplicateExtraBytes += sizes.sum() - sizes.first()
            }
        }

        val largeItems = finalItems.filter { it.isLargeFile }
        val oldDownloads = finalItems.filter { it.isOldDownload }

        val resultSummary = StorageScanSummary(
            isScanning = false,
            scanProgressMessage = if (folders.isEmpty()) {
                "App cache scanned. Add folders via SAF to scan Downloads, Media & Docs."
            } else {
                "Scan complete across ${folders.size} folder(s)"
            },
            scannedFolderCount = folders.size,
            totalFiles = finalItems.size,
            totalBytes = finalItems.sumOf { it.sizeBytes },
            largeFilesCount = largeItems.size,
            largeFilesBytes = largeItems.sumOf { it.sizeBytes },
            oldDownloadsCount = oldDownloads.size,
            oldDownloadsBytes = oldDownloads.sumOf { it.sizeBytes },
            duplicateGroupsCount = duplicateHashGroups.size,
            duplicateExtraBytes = duplicateExtraBytes,
            appCacheBytes = appCacheTotal,
            lastScanMillis = System.currentTimeMillis()
        )

        _scannedFiles.value = finalItems
        _summary.value = resultSummary
        resultSummary
    }

    suspend fun deleteFiles(items: List<ScannedFileItem>): Pair<Int, Long> =
        withContext(Dispatchers.IO) {
            var deletedCount = 0
            var bytesRecovered = 0L
            val deletedUris = mutableSetOf<String>()

            for (item in items) {
                val success = if (item.isAppCacheFile) {
                    runCatching { File(item.uriString).delete() }.getOrDefault(false)
                } else {
                    deleteSafUri(Uri.parse(item.uriString))
                }
                if (success) {
                    deletedCount++
                    bytesRecovered += item.sizeBytes
                    deletedUris += item.uriString
                }
            }

            if (deletedUris.isNotEmpty()) {
                val remaining = _scannedFiles.value.filterNot { it.uriString in deletedUris }
                _scannedFiles.value = remaining
                val current = _summary.value
                _summary.value = current.copy(
                    totalFiles = remaining.size,
                    totalBytes = remaining.sumOf { it.sizeBytes },
                    largeFilesCount = remaining.count { it.isLargeFile },
                    largeFilesBytes = remaining.filter { it.isLargeFile }.sumOf { it.sizeBytes },
                    oldDownloadsCount = remaining.count { it.isOldDownload },
                    oldDownloadsBytes = remaining.filter { it.isOldDownload }.sumOf { it.sizeBytes },
                    appCacheBytes = remaining.filter { it.isAppCacheFile }.sumOf { it.sizeBytes }
                )
            }
            deletedCount to bytesRecovered
        }

    suspend fun clearAppOwnCache(): Long = withContext(Dispatchers.IO) {
        var recovered = 0L
        val cacheDirs = listOfNotNull(context.cacheDir, context.codeCacheDir, context.externalCacheDir)
        for (dir in cacheDirs) {
            if (!dir.exists()) continue
            dir.walkBottomUp().forEach { file ->
                if (file.isFile) {
                    val size = file.length()
                    if (file.delete()) {
                        recovered += size
                    }
                }
            }
        }
        val remaining = _scannedFiles.value.filterNot { it.isAppCacheFile }
        _scannedFiles.value = remaining
        _summary.value = _summary.value.copy(
            appCacheBytes = 0L,
            totalFiles = remaining.size,
            totalBytes = remaining.sumOf { it.sizeBytes }
        )
        recovered
    }

    suspend fun inspectPickedMediaUri(uri: Uri): InspectedMediaInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val mime = context.contentResolver.getType(uri) ?: "media/*"
            var displayName = uri.lastPathSegment ?: "Selected Media"
            var sizeBytes = 0L
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
            InspectedMediaInfo(
                uriString = uri.toString(),
                displayName = displayName,
                sizeBytes = sizeBytes,
                mimeType = mime,
                dateAddedMillis = System.currentTimeMillis()
            )
        }.getOrNull()
    }

    private fun deleteSafUri(uri: Uri): Boolean {
        return runCatching {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        }.getOrElse {
            runCatching {
                DocumentFile.fromSingleUri(context, uri)?.delete() == true
            }.getOrDefault(false)
        }
    }

    private fun traverseDocumentTree(
        doc: DocumentFile,
        folderLabel: String,
        isDownloadFolder: Boolean,
        largeThreshold: Long,
        oldAgeMillis: Long,
        nowMillis: Long,
        out: MutableList<ScannedFileItem>,
        depth: Int
    ) {
        if (depth > 6 || out.size >= 2500) return
        val children = runCatching { doc.listFiles() }.getOrNull() ?: return
        for (child in children) {
            if (out.size >= 2500) break
            if (child.isDirectory) {
                val subName = child.name ?: folderLabel
                traverseDocumentTree(
                    doc = child,
                    folderLabel = subName,
                    isDownloadFolder = isDownloadFolder || subName.contains("download", ignoreCase = true),
                    largeThreshold = largeThreshold,
                    oldAgeMillis = oldAgeMillis,
                    nowMillis = nowMillis,
                    out = out,
                    depth = depth + 1
                )
            } else if (child.isFile) {
                val name = child.name ?: continue
                val size = child.length()
                val modified = child.lastModified()
                val mime = child.type ?: guessMimeType(name)
                val category = classifyCategory(mime, name, isDownloadFolder)
                val isOldDl = (isDownloadFolder || category == StorageCategory.DOWNLOADS) &&
                    modified > 0L && (nowMillis - modified) >= oldAgeMillis
                out += ScannedFileItem(
                    uriString = child.uri.toString(),
                    name = name,
                    sizeBytes = size,
                    lastModifiedMillis = modified,
                    mimeType = mime,
                    category = category,
                    folderDisplayName = folderLabel,
                    isOldDownload = isOldDl,
                    isLargeFile = size >= largeThreshold
                )
            }
        }
    }

    private fun classifyCategory(mime: String, name: String, inDownloads: Boolean): StorageCategory {
        val lowerMime = mime.lowercase()
        val lowerName = name.lowercase()
        return when {
            lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> StorageCategory.IMAGES
            lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                lowerName.endsWith(".mov") -> StorageCategory.VIDEOS
            lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") ||
                lowerName.endsWith(".m4a") || lowerName.endsWith(".flac") -> StorageCategory.AUDIO
            inDownloads -> StorageCategory.DOWNLOADS
            lowerMime.startsWith("text/") || lowerMime.contains("pdf") || lowerMime.contains("document") ||
                lowerName.endsWith(".pdf") || lowerName.endsWith(".docx") || lowerName.endsWith(".txt") ||
                lowerName.endsWith(".zip") || lowerName.endsWith(".apk") -> StorageCategory.DOCUMENTS
            else -> StorageCategory.DOCUMENTS
        }
    }

    private fun guessMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }

    private fun computeSha256(item: ScannedFileItem): String? {
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            val input = if (item.isAppCacheFile) {
                File(item.uriString).inputStream()
            } else {
                context.contentResolver.openInputStream(Uri.parse(item.uriString))
            } ?: return null

            input.use { stream ->
                val buffer = ByteArray(16 * 1024)
                // Hash up to first 2 MB + file size marker for responsive mobile performance
                var totalRead = 0L
                val maxBytes = 2L * 1024L * 1024L
                while (totalRead < maxBytes) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                    totalRead += read
                }
                digest.update(item.sizeBytes.toString().toByteArray())
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }
}
