package com.zoneanchor.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zoneanchor.app.R
import com.zoneanchor.app.ui.tappable
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupSection(
    status: String?,
    onExport: ((String) -> Unit) -> Unit,
    onImport: (String) -> Unit,
    onSaved: (Boolean) -> Unit,
    onOpenFailed: (String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingExport by remember { mutableStateOf<String?>(null) }
    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val content = pendingExport
        pendingExport = null
        if (uri == null || content == null) {
            onSaved(false)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                        checkNotNull(writer) { "Could not open backup file." }
                        writer.write(content)
                    }
                }.isSuccess
            }
            onSaved(saved)
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                        checkNotNull(reader) { "Could not open backup file." }
                        reader.readText()
                    }
                }
            }
            content.onSuccess(onImport).onFailure { onOpenFailed(it.message) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.backup_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        BackupActionRow(
            title = stringResource(R.string.export_backup),
            supportingText = stringResource(R.string.export_backup_description),
            icon = { Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp)) },
            onClick = {
                onExport { content ->
                    pendingExport = content
                    createBackup.launch("zoneanchor-backup-${LocalDate.now()}.json")
                }
            },
        )
        BackupActionRow(
            title = stringResource(R.string.import_backup),
            supportingText = stringResource(R.string.import_backup_description),
            icon = { Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp)) },
            onClick = { openBackup.launch(arrayOf("application/json", "text/json", "*/*")) },
        )
        status?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun BackupActionRow(
    title: String,
    supportingText: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .tappable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
            leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        leadingContent = icon,
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(supportingText, style = MaterialTheme.typography.bodySmall)
        },
    )
}
