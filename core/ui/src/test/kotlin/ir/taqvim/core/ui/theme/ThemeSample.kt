/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Synthetic screen for the theme screenshot matrix: type scale, containers, buttons and role swatches. */
@Composable
fun ThemeSample() {
    val colors = MaterialTheme.colorScheme
    TaqvimBackground(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Taqvim 1405", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
            Text("Body text 0123456789", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            Text("Supporting text", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Card {
                Text("Card", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("Primary") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
            }
            SwatchRow(listOf(colors.primary, colors.secondary, colors.tertiary, colors.error))
            SwatchRow(listOf(colors.primaryContainer, colors.secondaryContainer, colors.tertiaryContainer))
            SwatchRow(listOf(colors.surfaceContainerLowest, colors.surfaceContainer, colors.surfaceContainerHighest))
        }
    }
}

@Composable
private fun SwatchRow(swatches: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        swatches.forEach { Box(Modifier.size(48.dp).background(it)) }
    }
}
