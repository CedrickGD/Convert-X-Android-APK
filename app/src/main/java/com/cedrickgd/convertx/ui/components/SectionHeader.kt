package com.cedrickgd.convertx.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cedrickgd.convertx.ui.theme.ConvertXTheme
import com.cedrickgd.convertx.ui.theme.ThemeMode

/**
 * Row with an uppercase section label on the left and a horizontal divider that fills
 * the remaining space on the right.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(end = 12.dp)
        )
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0512)
@Composable
private fun PreviewSectionHeader() {
    ConvertXTheme(themeMode = ThemeMode.DARK) {
        SectionHeader(text = "Recent files", modifier = Modifier.padding(16.dp))
    }
}
