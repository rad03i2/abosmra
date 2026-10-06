package com.radwan.abosmra.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radwan.abosmra.GasLedgerViewModel
import com.radwan.abosmra.notifications.NotificationSoundPreset
import com.radwan.abosmra.notifications.OperationSoundPreset

@Composable
fun FinancialSoundSettingsCardV28(vm: GasLedgerViewModel) {
    val initial = remember { vm.financialFeedbackSettings() }
    var operationSound by remember { mutableStateOf(initial.operationSound) }
    var notificationSound by remember { mutableStateOf(initial.notificationSound) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SoundGroupCardV28(
            title = "صوت نجاح العملية",
            description = "صوت واحد موحّد للدين والتحصيل والتسديد الكامل، ويعمل فور نجاح الحفظ.",
            icon = {
                Icon(
                    Icons.Rounded.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            OperationSoundPreset.entries.forEach { preset ->
                SoundChoiceRowV28(
                    title = preset.title,
                    description = preset.description,
                    selected = operationSound == preset,
                    onSelect = {
                        operationSound = preset
                        vm.setOperationSound(preset)
                    },
                    onPreview = { vm.previewOperationSound(preset) }
                )
            }
        }

        SoundGroupCardV28(
            title = "صوت إشعار الهاتف",
            description = "يعمل مع إشعار تأكيد العملية بعد ثانيتين. يمكن أن يكون مختلفًا عن صوت نجاح العملية.",
            icon = {
                Icon(
                    Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            NotificationSoundPreset.entries.forEach { preset ->
                SoundChoiceRowV28(
                    title = preset.title,
                    description = preset.description,
                    selected = notificationSound == preset,
                    onSelect = {
                        notificationSound = preset
                        vm.setNotificationSound(preset)
                    },
                    onPreview = { vm.previewNotificationSound(preset) }
                )
            }
        }

        Text(
            "اضغط على أي خيار لاختياره؛ الحفظ يتم تلقائيًا. استخدم «تجربة» لسماع الصوت قبل الاعتماد.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SoundGroupCardV28(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        icon()
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun SoundChoiceRowV28(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
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
            Icon(
                imageVector = if (selected) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.VolumeUp
                },
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onPreview
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("تجربة")
            }
        }
    }
}
