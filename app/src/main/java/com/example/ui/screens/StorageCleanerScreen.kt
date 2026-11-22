package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.SafFolderEntity
import com.example.data.service.SystemIntentsHelper
import com.example.domain.model.ScannedFileItem
import com.example.domain.model.StorageCategory
import com.example.domain.model.StorageScanSummary
import com.example.domain.model.StorageSortOption
import com.example.ui.components.GlassMetricCard
import com.example.ui.components.GlossyRedButton
import com.example.ui.components.ShimmerBox
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CherryBorder
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepCherry
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GlossyCrimson
import com.example.ui.theme.MainText
import com.example.ui.theme.RubyRed
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftPinkHighlight
import com.example.util.FormatUtils

@Composable
fun StorageCleanerScreen(
    summary: StorageScanSummary,
    safFolders: List<SafFolderEntity>,
    visibleFiles: List<ScannedFileItem>,
    selectedCategory: StorageCategory,
    sortOption: StorageSortOption,
    selectedUris: Set<String>,
    totalBytesRecovered: Long,
    onBack: () -> Unit,
    onAddSafFolder: (android.net.Uri) -> Unit,
    onRemoveSafFolder: (String) -> Unit,
    onRescan: () -> Unit,
    onSelectCategory: (StorageCategory) -> Unit,
    onSelectSortOption: (StorageSortOption) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAllVisible: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearAppCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<ScannedFileItem?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onAddSafFolder(uri)
        }
    }

    val selectedTotalBytes = remember(visibleFiles, selectedUris) {
        visibleFiles.filter { it.uriString in selectedUris }.sumOf { it.sizeBytes }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (selectedUris.isNotEmpty()) 140.dp else 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DeepCherry.copy(alpha = 0.45f))
                                .border(1.dp, CherryBorder, CircleShape)
                                .testTag("storage_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MainText
                            )
                        }
                        Column {
                            Text(
                                text = "SMART STORAGE CLEANER",
                                style = MaterialTheme.typography.titleLarge,
                                color = MainText
                            )
                            Text(
                                text = "${summary.totalFiles} item(s) • ${FormatUtils.formatBytes(summary.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                    }

                    IconButton(
                        onClick = onRescan,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DeepCherry.copy(alpha = 0.45f))
                            .border(1.dp, CherryBorder, CircleShape)
                            .testTag("storage_rescan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Rescan Folders",
                            tint = SoftPinkHighlight
                        )
                    }
                }
            }

            // Summary & SAF Folder Controls
            item {
                GlassMetricCard(
                    highlighted = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL RECOVERED",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText
                            )
                            Text(
                                text = FormatUtils.formatBytes(totalBytesRecovered),
                                style = MaterialTheme.typography.headlineMedium,
                                color = EmeraldOptimal
                            )
                        }
                        if (summary.appCacheBytes > 0L) {
                            FilledTonalButton(
                                onClick = onClearAppCache,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = GlossyCrimson.copy(alpha = 0.3f),
                                    contentColor = MainText
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(17.dp)
                                )
                                Text("Clear Cache (${FormatUtils.formatBytes(summary.appCacheBytes)})")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = summary.scanProgressMessage.ifBlank {
                            "Grant access to Downloads, DCIM, or Documents using Android Storage Access Framework."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = RubyRed.copy(alpha = 0.65f),
                                contentColor = MainText
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_saf_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(18.dp)
                            )
                            Text("Add Folder (SAF)")
                        }
                    }

                    if (safFolders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            safFolders.forEach { folder ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(DeepCherry.copy(alpha = 0.55f))
                                        .border(1.dp, CherryBorder, RoundedCornerShape(50))
                                        .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        tint = SoftPinkHighlight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${folder.displayName} (${folder.lastScannedFileCount})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MainText
                                    )
                                    IconButton(
                                        onClick = { onRemoveSafFolder(folder.uriString) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove ${folder.displayName}",
                                            tint = SecondaryText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StorageCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { onSelectCategory(category) },
                            label = { Text(category.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RubyRed,
                                selectedLabelColor = MainText,
                                containerColor = DeepCherry.copy(alpha = 0.35f),
                                labelColor = SecondaryText
                            )
                        )
                    }
                }
            }

            // Sort & Select All Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Sort",
                                tint = SoftPinkHighlight,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(18.dp)
                            )
                            Text(sortOption.label, color = MainText)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = CardBackground
                        ) {
                            StorageSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label, color = MainText) },
                                    onClick = {
                                        onSelectSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = onSelectAllVisible,
                        enabled = visibleFiles.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SelectAll,
                            contentDescription = "Select All",
                            tint = SoftPinkHighlight,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(18.dp)
                        )
                        Text(
                            text = if (selectedUris.size == visibleFiles.size && visibleFiles.isNotEmpty()) {
                                "Deselect All"
                            } else {
                                "Select All (${visibleFiles.size})"
                            },
                            color = SoftPinkHighlight
                        )
                    }
                }
            }

            if (summary.isScanning) {
                items(3) {
                    ShimmerBox(
                        height = 74.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                    )
                }
            } else if (visibleFiles.isEmpty()) {
                item {
                    GlassMetricCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = SoftPinkHighlight,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = "No files in ${selectedCategory.label}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MainText
                            )
                            Text(
                                text = "Tap \"Add Folder (SAF)\" above to select Downloads, Pictures, Documents, or any folder you want to audit and clean.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                    }
                }
            } else {
                items(visibleFiles, key = { it.uriString }) { file ->
                    val isSelected = file.uriString in selectedUris
                    StorageFileRow(
                        file = file,
                        isSelected = isSelected,
                        onToggleSelect = { onToggleSelect(file.uriString) },
                        onPreview = { previewFile = file },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                    )
                }
            }
        }

        // Bottom Multi-Select Delete Bar
        if (selectedUris.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(CardBackground.copy(alpha = 0.96f))
                    .border(1.dp, CherryBorder)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                GlossyRedButton(
                    text = "DELETE ${selectedUris.size} FILE(S) • ${FormatUtils.formatBytes(selectedTotalBytes)}",
                    icon = Icons.Rounded.DeleteForever,
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("delete_selected_files_button")
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CardBackground,
            title = { Text("Confirm Permanent Deletion", color = MainText) },
            text = {
                Text(
                    text = "Permanently delete ${selectedUris.size} selected item(s) totaling ${FormatUtils.formatBytes(selectedTotalBytes)}? This cannot be undone.",
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteSelected()
                    }
                ) {
                    Text("Delete", color = CrimsonAlert)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MainText)
                }
            }
        )
    }

    if (previewFile != null) {
        val file = previewFile!!
        AlertDialog(
            onDismissRequest = { previewFile = null },
            containerColor = CardBackground,
            title = {
                Text(
                    text = file.name,
                    color = MainText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (file.category == StorageCategory.IMAGES && !file.isAppCacheFile) {
                        AsyncImage(
                            model = file.uriString,
                            contentDescription = file.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                    Text("Folder: ${file.folderDisplayName}", color = SecondaryText)
                    Text("Size: ${FormatUtils.formatBytes(file.sizeBytes)}", color = SoftPinkHighlight)
                    Text("Modified: ${FormatUtils.formatShortDate(file.lastModifiedMillis)}", color = SecondaryText)
                    Text("MIME: ${file.mimeType}", color = SecondaryText)
                    if (file.duplicateGroupId != null) {
                        Text(
                            text = "Duplicate hash group: #${file.duplicateGroupId}",
                            color = AmberWarm
                        )
                    }
                }
            },
            confirmButton = {
                if (!file.isAppCacheFile) {
                    TextButton(
                        onClick = {
                            SystemIntentsHelper.openUriWithExternalViewer(
                                context,
                                file.uriString,
                                file.mimeType
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = SoftPinkHighlight,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(16.dp)
                        )
                        Text("Open", color = SoftPinkHighlight)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { previewFile = null }) {
                    Text("Close", color = MainText)
                }
            }
        )
    }
}

@Composable
private fun StorageFileRow(
    file: ScannedFileItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (file.category) {
        StorageCategory.IMAGES -> Icons.Rounded.Image
        StorageCategory.VIDEOS -> Icons.Rounded.VideoFile
        StorageCategory.AUDIO -> Icons.Rounded.AudioFile
        else -> Icons.Rounded.Description
    }

    GlassMetricCard(
        modifier = modifier,
        highlighted = isSelected,
        onClick = onPreview
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = GlossyCrimson,
                    uncheckedColor = SecondaryText,
                    checkmarkColor = MainText
                )
            )

            if (file.category == StorageCategory.IMAGES && !file.isAppCacheFile) {
                AsyncImage(
                    model = file.uriString,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DeepCherry.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = file.category.label,
                        tint = SoftPinkHighlight
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MainText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.folderDisplayName} • ${FormatUtils.formatShortDate(file.lastModifiedMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (file.duplicateGroupId != null) {
                        Text(
                            text = "DUPLICATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarm
                        )
                    }
                    if (file.isLargeFile) {
                        Text(
                            text = "LARGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftPinkHighlight
                        )
                    }
                    if (file.isOldDownload) {
                        Text(
                            text = "OLD DL",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                    }
                }
            }

            Text(
                text = FormatUtils.formatBytes(file.sizeBytes),
                style = MaterialTheme.typography.labelMedium,
                color = SoftPinkHighlight
            )
        }
    }
}
