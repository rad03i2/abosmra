package com.radwan.abosmra.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.radwan.abosmra.GasLedgerViewModel
import com.radwan.abosmra.data.AutoBackupInterval
import com.radwan.abosmra.data.BackupPreview
import com.radwan.abosmra.ui.components.ScreenTopBar
import com.radwan.abosmra.ui.components.SectionTitle
import com.radwan.abosmra.util.formatDate
import com.radwan.abosmra.util.formatTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreenV8(vm: GasLedgerViewModel) {
    val context = LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    var pendingRestoreRaw by remember { mutableStateOf<String?>(null) }
    var autoInterval by remember { mutableStateOf(vm.autoBackupInterval()) }
    var lastBackupAt by remember { mutableStateOf(vm.lastBackupAt()) }

    val backupFileName = remember {
        val stamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.US)
        )
        "DaftarAlGas-backup-" + stamp + ".json"
    }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(vm.createBackupJson())
                } ?: error("تعذر فتح الملف للحفظ.")
                vm.markManualBackupCreated()
                lastBackupAt = vm.lastBackupAt()
            }
            message = if (result.isSuccess) {
                "تم حفظ النسخة الاحتياطية بنجاح."
            } else {
                "تعذر حفظ النسخة الاحتياطية."
            }
        }
    }

    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("تعذر قراءة الملف.")
            }
            if (result.isFailure) {
                pendingRestoreRaw = null
                preview = null
                message = "تعذر قراءة ملف النسخة الاحتياطية."
            } else {
                val raw = result.getOrThrow()
                val checked = vm.previewBackup(raw)
                preview = checked
                pendingRestoreRaw = raw.takeIf { checked.valid }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("إعادة البيانات التجريبية") },
            text = { Text("سيتم حذف البيانات الحالية وإعادة بيانات العرض الأولية.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetDemoData()
                    showResetConfirm = false
                    lastBackupAt = vm.lastBackupAt()
                    message = "تمت إعادة البيانات التجريبية."
                }) { Text("إعادة") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    preview?.let { checked ->
        AlertDialog(
            onDismissRequest = {
                preview = null
                pendingRestoreRaw = null
            },
            title = {
                Text(if (checked.valid) "معاينة النسخة" else "ملف غير صالح")
            },
            text = {
                if (checked.valid) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تم فحص الملف بنجاح قبل لمس البيانات الحالية.")
                        V8PreviewLine("الزبائن", checked.customerCount.toString())
                        V8PreviewLine("الحركات", checked.entryCount.toString())
                        if (checked.createdAt > 0L) {
                            V8PreviewLine(
                                "تاريخ النسخة",
                                formatDate(checked.createdAt) + " • " + formatTime(checked.createdAt)
                            )
                        }
                        Text(
                            "عند التأكيد سيحفظ التطبيق نسخة أمان من بياناتك الحالية أولًا، ثم يستبدلها بهذه النسخة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(checked.message)
                }
            },
            confirmButton = {
                if (checked.valid) {
                    TextButton(onClick = {
                        val raw = pendingRestoreRaw
                        val result = if (raw != null) vm.restoreBackup(raw) else null
                        preview = null
                        pendingRestoreRaw = null
                        lastBackupAt = vm.lastBackupAt()
                        message = result?.message ?: "تعذر استعادة النسخة."
                    }) {
                        Text("استعادة واستبدال")
                    }
                } else {
                    TextButton(onClick = {
                        preview = null
                        pendingRestoreRaw = null
                    }) { Text("حسنًا") }
                }
            },
            dismissButton = if (checked.valid) {
                {
                    TextButton(onClick = {
                        preview = null
                        pendingRestoreRaw = null
                    }) { Text("إلغاء") }
                }
            } else null
        )
    }

    message?.let { value ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("دفتر الغاز") },
            text = { Text(value) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text("حسنًا") }
            }
        )
    }

    Scaffold(topBar = { ScreenTopBar("المزيد") }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Info,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("دفتر الغاز", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "الإصدار 1.8.0 • نسخ احتياطي واستعادة آمنة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item { SectionTitle("حماية البيانات") }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Backup,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text("النسخة الاحتياطية", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (lastBackupAt > 0L) {
                                        "آخر نسخة: " + formatDate(lastBackupAt) + " • " + formatTime(lastBackupAt)
                                    } else {
                                        "لم يتم إنشاء نسخة احتياطية بعد"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { createBackup.launch(backupFileName) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Rounded.Backup, null, modifier = Modifier.size(18.dp))
                            Text("إنشاء نسخة الآن", modifier = Modifier.padding(horizontal = 7.dp))
                        }

                        OutlinedButton(
                            onClick = { openBackup.launch(arrayOf("application/json", "text/plain")) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(18.dp))
                            Text("استعادة من ملف", modifier = Modifier.padding(horizontal = 7.dp))
                        }
                    }
                }
            }

            item { SectionTitle("النسخ التلقائي") }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Schedule,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text("النسخ التلقائي", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "يُحفظ داخل مساحة التطبيق دون صلاحيات تخزين عامة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            V8AutoBackupChip(
                                "إيقاف",
                                autoInterval == AutoBackupInterval.OFF,
                                Modifier.weight(1f)
                            ) {
                                autoInterval = AutoBackupInterval.OFF
                                vm.setAutoBackupInterval(autoInterval)
                            }
                            V8AutoBackupChip(
                                "يومي",
                                autoInterval == AutoBackupInterval.DAILY,
                                Modifier.weight(1f)
                            ) {
                                autoInterval = AutoBackupInterval.DAILY
                                vm.setAutoBackupInterval(autoInterval)
                                lastBackupAt = vm.lastBackupAt()
                            }
                            V8AutoBackupChip(
                                "أسبوعي",
                                autoInterval == AutoBackupInterval.WEEKLY,
                                Modifier.weight(1f)
                            ) {
                                autoInterval = AutoBackupInterval.WEEKLY
                                vm.setAutoBackupInterval(autoInterval)
                                lastBackupAt = vm.lastBackupAt()
                            }
                        }
                    }
                }
            }

            item { SectionTitle("التطبيق") }
            item {
                V8SettingsRow(Icons.Rounded.Share, "كشف الحساب", "المشاركة من داخل حساب الزبون") {
                    message = "افتح حساب الزبون ثم كشف الحساب للمشاركة."
                }
            }
            item {
                V8SettingsRow(Icons.Rounded.DarkMode, "المظهر", "يتبع إعداد الهاتف تلقائيًا") {
                    message = "الوضع الفاتح والداكن يتبعان إعداد الهاتف."
                }
            }
            item {
                V8SettingsRow(Icons.Rounded.AccountBalanceWallet, "العملة", "الدينار العراقي • د.ع") { }
            }
            item {
                V8SettingsRow(Icons.Rounded.Lock, "قفل التطبيق", "غير مفعّل حاليًا") {
                    message = "PIN والبصمة غير مفعّلين في هذه النسخة."
                }
            }

            item {
                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("إعادة البيانات التجريبية")
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
        }
    }
}

@Composable
private fun V8AutoBackupChip(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier
    )
}

@Composable
private fun V8PreviewLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun V8SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(19.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronLeft,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
