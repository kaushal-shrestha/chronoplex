package com.zoneanchor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zoneanchor.ZoneAnchorApp
import com.zoneanchor.model.AppPalette
import com.zoneanchor.model.AppearanceMode
import com.zoneanchor.ui.theme.toComposeColor

@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as ZoneAnchorApp
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val palette by viewModel.palette.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Appearance", style = MaterialTheme.typography.headlineSmall)
        }
        items(AppearanceMode.entries, key = { it.name }) { mode ->
            RadioRow(
                title = mode.title,
                selected = appearance == mode,
                onClick = { viewModel.setAppearance(mode) },
            )
        }
        item {
            Text("Theme", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp))
        }
        items(AppPalette.entries, key = { it.name }) { option ->
            PaletteCard(
                palette = option,
                selected = palette == option,
                onSelect = { viewModel.setPalette(option) },
            )
        }
    }
}

@Composable
private fun RadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PaletteCard(palette: AppPalette, selected: Boolean, onSelect: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(palette.title, style = MaterialTheme.typography.titleMedium)
                    Text(palette.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Swatches(palette)
            }
            Button(onClick = onSelect, enabled = !selected, modifier = Modifier.fillMaxWidth()) {
                Text(if (selected) "Selected" else "Use this theme")
            }
        }
    }
}

@Composable
private fun Swatches(palette: AppPalette) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(palette.lightBackground, palette.lightSurface, palette.lightPrimary).forEach { color ->
            Box(
                Modifier.size(22.dp)
                    .clip(CircleShape)
                    .background(color.toComposeColor()),
            )
        }
    }
}
