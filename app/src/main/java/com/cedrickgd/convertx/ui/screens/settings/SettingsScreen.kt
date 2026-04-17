package com.cedrickgd.convertx.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedrickgd.convertx.BuildConfig
import com.cedrickgd.convertx.data.AppContainer
import com.cedrickgd.convertx.data.SettingsRepository
import com.cedrickgd.convertx.ui.components.GlowCard
import com.cedrickgd.convertx.ui.components.SectionHeader
import com.cedrickgd.convertx.ui.theme.BrandTokens
import com.cedrickgd.convertx.ui.theme.ConvertXTheme
import com.cedrickgd.convertx.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val repo = appContainer.settingsRepository
    val theme by repo.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val outputTree by repo.outputTreeUri.collectAsStateWithLifecycle(initialValue = null)
    val glow by repo.useAccentGlow.collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    SettingsContent(
        themeMode = theme,
        onThemeChange = { mode -> scope.launchSetTheme(repo, mode) },
        outputTreeUri = outputTree,
        onOutputTreeChange = { uri -> scope.launchSetTree(repo, uri) },
        useAccentGlow = glow,
        onAccentGlowChange = { enabled -> scope.launchSetGlow(repo, enabled) },
        modifier = modifier
    )
}

private fun CoroutineScope.launchSetTheme(repo: SettingsRepository, mode: ThemeMode) {
    launch { repo.setThemeMode(mode) }
}

private fun CoroutineScope.launchSetTree(repo: SettingsRepository, uri: Uri?) {
    launch { repo.setOutputTreeUri(uri) }
}

private fun CoroutineScope.launchSetGlow(repo: SettingsRepository, enabled: Boolean) {
    launch { repo.setAccentGlow(enabled) }
}

@Composable
internal fun SettingsContent(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    outputTreeUri: Uri?,
    onOutputTreeChange: (Uri?) -> Unit,
    useAccentGlow: Boolean,
    onAccentGlowChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val treeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            onOutputTreeChange(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(text = "Theme")
        ThemeSegmented(selected = themeMode, onChange = onThemeChange)

        SectionHeader(text = "Output folder")
        val treeLabel = outputTreeUri?.let { uri ->
            runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull()
        } ?: "Not set"
        GlowCard(modifier = Modifier.fillMaxWidth(), glowing = outputTreeUri != null) {
            Text(
                text = "Current",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = treeLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { treeLauncher.launch(outputTreeUri) }
            ) {
                Text(
                    text = "Pick a folder",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        SectionHeader(text = "Appearance")
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Accent glow",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Soft glow around highlighted cards",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useAccentGlow,
                    onCheckedChange = onAccentGlowChange
                )
            }
        }

        SectionHeader(text = "About")
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Convert-X",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Convert-X — Fast, offline file converter",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ThemeSegmented(
    selected: ThemeMode,
    onChange: (ThemeMode) -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            ThemePill(
                label = when (mode) {
                    ThemeMode.SYSTEM -> "System"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                },
                selected = mode == selected,
                onClick = { onChange(mode) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(
                if (selected) BrandTokens.brandGradient
                else androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0512)
@Composable
private fun SettingsScreenPreview() {
    ConvertXTheme(themeMode = ThemeMode.DARK) {
        SettingsContent(
            themeMode = ThemeMode.DARK,
            onThemeChange = {},
            outputTreeUri = null,
            onOutputTreeChange = {},
            useAccentGlow = true,
            onAccentGlowChange = {}
        )
    }
}
