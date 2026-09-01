package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppTheme
import com.example.data.model.EqualizerSettings

@Composable
fun EqualizerDialog(
    settings: EqualizerSettings,
    theme: AppTheme,
    onSave: (EqualizerSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var bass by remember { mutableFloatStateOf(settings.bassBoost) }
    var treble by remember { mutableFloatStateOf(settings.treble) }
    var vocal by remember { mutableFloatStateOf(settings.vocalClarity) }
    var surround by remember { mutableFloatStateOf(settings.surround3D) }
    var vinylCrackle by remember { mutableStateOf(settings.vinylCrackle) }
    var currentPreset by remember { mutableStateOf(settings.presetName) }

    val primaryColor = Color(theme.primaryHex)

    val presets = listOf(
        "Balanced" to EqualizerSettings(0.5f, 0.5f, 0.5f, 0.4f, false, "Balanced"),
        "Club Bass" to EqualizerSettings(0.9f, 0.6f, 0.4f, 0.7f, false, "Club Bass"),
        "Vocal Pro" to EqualizerSettings(0.35f, 0.75f, 0.9f, 0.3f, false, "Vocal Pro"),
        "Lo-Fi Vinyl" to EqualizerSettings(0.7f, 0.3f, 0.5f, 0.5f, true, "Lo-Fi Vinyl"),
        "Acoustic Live" to EqualizerSettings(0.45f, 0.8f, 0.7f, 0.6f, false, "Acoustic Live")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Equalizer",
                    tint = primaryColor
                )
                Text(
                    text = "Sound Studio & Equalizer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("equalizer_dialog_content"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Presets",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { (name, preset) ->
                        val isSelected = currentPreset == name
                        Card(
                            onClick = {
                                currentPreset = name
                                bass = preset.bassBoost
                                treble = preset.treble
                                vocal = preset.vocalClarity
                                surround = preset.surround3D
                                vinylCrackle = preset.vinylCrackle
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("preset_$name")
                        ) {
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Sliders
                EqSliderRow("Bass Boost", bass, primaryColor) { bass = it; currentPreset = "Custom" }
                EqSliderRow("Treble Sparkle", treble, primaryColor) { treble = it; currentPreset = "Custom" }
                EqSliderRow("Vocal Clarity", vocal, primaryColor) { vocal = it; currentPreset = "Custom" }
                EqSliderRow("3D Spatial Surround", surround, primaryColor) { surround = it; currentPreset = "Custom" }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Vinyl Needle Crackle FX",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Simulates authentic turntable noise",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vinylCrackle,
                        onCheckedChange = { vinylCrackle = it; currentPreset = "Custom" },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor),
                        modifier = Modifier.testTag("vinyl_crackle_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        EqualizerSettings(
                            bassBoost = bass,
                            treble = treble,
                            vocalClarity = vocal,
                            surround3D = surround,
                            vinylCrackle = vinylCrackle,
                            presetName = currentPreset
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.testTag("eq_apply_button")
            ) {
                Text("Apply FX", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun EqSliderRow(
    label: String,
    value: Float,
    primaryColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = "${(value * 100).toInt()}%", fontSize = 12.sp, color = primaryColor)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = primaryColor.copy(alpha = 0.2f)
            ),
            modifier = Modifier.height(30.dp)
        )
    }
}
