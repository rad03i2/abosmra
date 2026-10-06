package com.radwan.abosmra.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radwan.abosmra.GasLedgerViewModel
import com.radwan.abosmra.ui.theme.ArabicFontPreset
import com.radwan.abosmra.ui.theme.TypographySettingsStore
import com.radwan.abosmra.ui.theme.fontFamilyFor
import kotlin.math.roundToInt

@Composable
internal fun TypographySettingsCardV12(vm: GasLedgerViewModel) {
    val settings by vm.typographySettings.collectAsStateWithLifecycle()
    var sliderScale by remember { mutableFloatStateOf(settings.scale) }

    LaunchedEffect(settings.scale) {
        sliderScale = settings.scale
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "الخط وحجم النص",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "اختر الخط العربي المناسب واضبط الحجم بدون التأثير على بيانات التطبيق.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ArabicFontPreset.entries.forEach { preset ->
                val selected = settings.font == preset
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.setArabicFont(preset) },
                    shape = MaterialTheme.shapes.large,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        1.dp,
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { vm.setArabicFont(preset) }
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                preset.title,
                                style = TextStyle(
                                    fontFamily = fontFamilyFor(preset),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                "دفتر دين الغاز - ابو سمرة",
                                style = TextStyle(
                                    fontFamily = fontFamilyFor(preset),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "حجم الخط",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            (sliderScale * 100f).roundToInt().toString() + "%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = sliderScale,
                        onValueChange = {
                            sliderScale = TypographySettingsStore.sanitizeScale(it)
                        },
                        onValueChangeFinished = {
                            vm.setFontScale(sliderScale)
                        },
                        valueRange = TypographySettingsStore.MIN_SCALE..
                            TypographySettingsStore.MAX_SCALE,
                        steps = 4
                    )

                    Text(
                        "نموذج للمعاينة — الزبون أحمد محمد • الدين الحالي 5,000 د.ع",
                        style = TextStyle(
                            fontFamily = fontFamilyFor(settings.font),
                            fontWeight = FontWeight.Medium,
                            fontSize = (16f * sliderScale).sp,
                            lineHeight = (25f * sliderScale).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(
                onClick = { vm.resetTypography() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("إعادة الخط والحجم الافتراضي")
            }
        }
    }
}
