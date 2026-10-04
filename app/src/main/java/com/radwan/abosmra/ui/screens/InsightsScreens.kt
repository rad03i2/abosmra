package com.radwan.abosmra.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radwan.abosmra.GasLedgerViewModel
import com.radwan.abosmra.data.Customer
import com.radwan.abosmra.data.EntryType
import com.radwan.abosmra.data.LedgerEntry
import com.radwan.abosmra.ui.components.CustomerCard
import com.radwan.abosmra.ui.components.EmptyState
import com.radwan.abosmra.ui.components.MetricCard
import com.radwan.abosmra.ui.components.ScreenTopBar
import com.radwan.abosmra.ui.components.SectionTitle
import com.radwan.abosmra.ui.theme.DebtRed
import com.radwan.abosmra.ui.theme.PaidGreen
import com.radwan.abosmra.util.daysSince
import com.radwan.abosmra.util.formatDate
import com.radwan.abosmra.util.formatMoney
import com.radwan.abosmra.util.normalizeIraqPhone
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun SmartSearchScreen(
    vm: GasLedgerViewModel,
    onBack: () -> Unit,
    onCustomer: (String) -> Unit,
    onDebt: (String) -> Unit,
    onPayment: (String) -> Unit
) {
    val customers by vm.customers.collectAsState()
    val entries by vm.entries.collectAsState()
    var query by remember { mutableStateOf("") }

    val results = remember(customers, entries, query) {
        if (query.isBlank()) {
            customers.sortedByDescending { vm.lastEntryFor(it.id)?.createdAt ?: it.createdAt }.take(8)
        } else {
            customers.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.phone.orEmpty().contains(query.filter(Char::isDigit)) ||
                    it.area.contains(query, ignoreCase = true)
            }.sortedByDescending { vm.balance(it) }
        }
    }

    Scaffold(
        topBar = { ScreenTopBar("البحث السريع", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("الاسم، رقم الهاتف أو المنطقة") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
            item {
                Text(
                    if (query.isBlank()) "الوصول السريع" else "نتائج البحث",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (results.isEmpty()) {
                item {
                    EmptyState("لا توجد نتيجة", "جرّب جزءًا من الاسم أو رقم الهاتف أو اسم المنطقة.")
                }
            } else {
                items(results, key = { it.id }) { customer ->
                    SearchResultCard(
                        customer = customer,
                        balance = vm.balance(customer),
                        onOpen = { onCustomer(customer.id) },
                        onDebt = { onDebt(customer.id) },
                        onPayment = { onPayment(customer.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    customer: Customer,
    balance: Long,
    onOpen: () -> Unit,
    onDebt: () -> Unit,
    onPayment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CustomerCard(customer, balance, onClick = onOpen)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDebt, modifier = Modifier.weight(1f)) { Text("إضافة دين") }
                OutlinedButton(onClick = onPayment, enabled = balance > 0, modifier = Modifier.weight(1f)) { Text("تحصيل") }
                Button(onClick = onOpen, modifier = Modifier.weight(1f)) { Text("فتح الحساب") }
            }
        }
    }
}

private enum class ReportPeriod(val label: String, val days: Long?) {
    TODAY("اليوم", 1L),
    WEEK("الأسبوع", 7L),
    MONTH("الشهر", 30L),
    CUSTOM("مخصص", 90L)
}

@Composable
fun ReportsScreen(vm: GasLedgerViewModel) {
    val customers by vm.customers.collectAsState()
    val allEntries by vm.entries.collectAsState()
    var period by remember { mutableStateOf(ReportPeriod.MONTH) }

    val startMillis = remember(period) {
        if (period == ReportPeriod.TODAY) {
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            System.currentTimeMillis() - (period.days ?: 30L) * 24L * 60L * 60L * 1000L
        }
    }
    val entries = allEntries.filter { it.createdAt >= startMillis }
    val debts = entries.filter { it.type == EntryType.DEBT }.sumOf { it.amount }
    val collections = entries.filter { it.type == EntryType.PAYMENT }.sumOf { it.amount }
    val bottles = entries.filter { it.type == EntryType.DEBT }.sumOf { it.bottles ?: 0 }
    val newCustomers = customers.count { it.createdAt >= startMillis }
    val topCustomer = vm.topDebtors().firstOrNull()
    val areaTotals = customers
        .groupBy { it.area.ifBlank { "غير محددة" } }
        .mapValues { (_, list) -> list.sumOf { vm.balance(it) } }
    val topArea = areaTotals.maxByOrNull { it.value }
    val maxBar = maxOf(debts, collections, 1L).toFloat()

    Scaffold(
        topBar = { ScreenTopBar("التقارير والإحصائيات") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportPeriod.entries.forEach { option ->
                        FilterChip(
                            selected = period == option,
                            onClick = { period = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("المبيعات بالدين", formatMoney(debts), Icons.Rounded.ReceiptLong, Modifier.weight(1f))
                    MetricCard("التحصيلات", formatMoney(collections), Icons.Rounded.TrendingUp, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("الدين المتبقي الكلي", formatMoney(vm.totalDebt()), Icons.Rounded.AccountBalanceWallet, Modifier.weight(1f))
                    MetricCard("زبائن جدد", newCustomers.toString(), Icons.Rounded.People, Modifier.weight(1f))
                }
            }
            item {
                MetricCard("القناني المسجلة", bottles.toString(), Icons.Rounded.BarChart, Modifier.fillMaxWidth())
            }

            item { SectionTitle("الديون مقابل التحصيلات") }
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ReportBar("الديون", debts, debts.toFloat() / maxBar, DebtRed)
                        ReportBar("التحصيلات", collections, collections.toFloat() / maxBar, PaidGreen)
                    }
                }
            }

            item { SectionTitle("أبرز الحسابات") }
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("أعلى منطقة مديونية", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (topArea != null) topArea.key + " • " + formatMoney(topArea.value) else "لا توجد بيانات",
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text("أعلى زبون مديونية", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (topCustomer != null) topCustomer.name + " • " + formatMoney(vm.balance(topCustomer)) else "لا توجد حسابات مفتوحة",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportBar(
    label: String,
    value: Long,
    fraction: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(formatMoney(value))
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color
        )
    }
}

@Composable
fun FollowUpScreen(
    vm: GasLedgerViewModel,
    onBack: () -> Unit,
    onCustomer: (String) -> Unit
) {
    val context = LocalContext.current
    val customers by vm.customers.collectAsState()
    val entries by vm.entries.collectAsState()
    var filter by remember { mutableStateOf("لم يسدد منذ 30 يومًا") }

    fun daysSincePayment(customer: Customer): Long {
        val lastPayment = vm.lastPaymentFor(customer.id)
        return if (lastPayment != null) daysSince(lastPayment.createdAt) else daysSince(customer.createdAt)
    }

    val candidates = remember(customers, entries, filter) {
        val indebted = customers.filter { vm.balance(it) > 0 }
        when (filter) {
            "ديون قديمة" -> indebted.filter {
                val last = vm.lastEntryFor(it.id)
                last != null && daysSince(last.createdAt) >= 30
            }.sortedByDescending { vm.balance(it) }
            "أعلى الحسابات" -> indebted.sortedByDescending { vm.balance(it) }.take(10)
            "اليوم" -> indebted.sortedWith(compareByDescending<Customer> { daysSincePayment(it) }.thenByDescending { vm.balance(it) }).take(10)
            else -> indebted.filter { daysSincePayment(it) >= 30 }.sortedByDescending { daysSincePayment(it) }
        }
    }

    Scaffold(
        topBar = { ScreenTopBar("التنبيهات والمتابعة", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "متابعة مهنية للحسابات المفتوحة دون إزعاج الزبائن.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("لم يسدد منذ 30 يومًا", "ديون قديمة", "أعلى الحسابات", "اليوم").forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option, maxLines = 1) }
                        )
                    }
                }
            }
            if (candidates.isEmpty()) {
                item { EmptyState("لا توجد حسابات تحتاج متابعة", "الحسابات الحالية لا تطابق هذا التصنيف.") }
            } else {
                items(candidates, key = { it.id }) { customer ->
                    val lastPayment = vm.lastPaymentFor(customer.id)
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(customer.name, fontWeight = FontWeight.ExtraBold)
                                    Text(customer.area.ifBlank { "بدون منطقة" }, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(formatMoney(vm.balance(customer)), color = DebtRed, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(
                                "آخر دفعة: " + (lastPayment?.let { formatDate(it.createdAt) + " • " + formatMoney(it.amount) } ?: "لا توجد دفعة"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "منذ آخر تحصيل: " + daysSincePayment(customer).toString() + " يوم",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        customer.phone?.let {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + it)))
                                        }
                                    },
                                    enabled = !customer.phone.isNullOrBlank()
                                ) {
                                    Icon(Icons.Rounded.Call, "اتصال")
                                }
                                IconButton(
                                    onClick = {
                                        customer.phone?.let {
                                            val phone = normalizeIraqPhone(it).removePrefix("+")
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + phone)))
                                            }
                                        }
                                    },
                                    enabled = !customer.phone.isNullOrBlank()
                                ) {
                                    Icon(Icons.Rounded.Chat, "WhatsApp")
                                }
                                Button(onClick = { onCustomer(customer.id) }, modifier = Modifier.weight(1f)) {
                                    Text("فتح الحساب")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: GasLedgerViewModel) {
    val context = LocalContext.current
    var darkMode by remember { mutableStateOf(false) }
    var appLock by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("إعادة البيانات التجريبية") },
            text = { Text("سيتم حذف البيانات الحالية وإعادة بيانات العرض الأولية.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetDemoData()
                    showResetConfirm = false
                    infoMessage = "تمت إعادة البيانات التجريبية."
                }) { Text("إعادة") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    if (infoMessage != null) {
        AlertDialog(
            onDismissRequest = { infoMessage = null },
            title = { Text("دفتر الغاز") },
            text = { Text(infoMessage.orEmpty()) },
            confirmButton = { TextButton(onClick = { infoMessage = null }) { Text("حسنًا") } }
        )
    }

    fun exportBackup() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, vm.exportJson())
        }
        context.startActivity(Intent.createChooser(intent, "تصدير نسخة بيانات دفتر الغاز"))
    }

    Scaffold(
        topBar = { ScreenTopBar("الإعدادات والنسخ الاحتياطي") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SettingsSectionTitle("البيانات") }
            item { SettingsRow(Icons.Rounded.Backup, "إنشاء نسخة احتياطية", "تصدير البيانات بصيغة JSON", ::exportBackup) }
            item { SettingsRow(Icons.Rounded.Restore, "استعادة نسخة احتياطية", "اختيار ملف نسخة محفوظة") { infoMessage = "واجهة الاستعادة جاهزة للتوصيل بمنتقي الملفات في مرحلة التكامل النهائية." } }
            item { SettingsRow(Icons.Rounded.CloudDownload, "تصدير البيانات", "مشاركة نسخة كاملة من بيانات التطبيق", ::exportBackup) }
            item { SettingsRow(Icons.Rounded.Share, "تصدير كشف أو تقرير", "من داخل كشف الحساب أو التقارير") { infoMessage = "يمكن مشاركة كشف الزبون مباشرة من شاشة كشف الحساب." } }

            item { SettingsSectionTitle("إعدادات التطبيق") }
            item {
                SettingSwitchRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "الوضع الداكن",
                    subtitle = "يتبع النظام افتراضيًا",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
            }
            item { SettingsRow(Icons.Rounded.AccountBalanceWallet, "العملة", "الدينار العراقي • د.ع") { } }
            item { SettingsRow(Icons.Rounded.ReceiptLong, "تنسيق الأرقام", "الأرقام الإنجليزية 0–9") { } }

            item { SettingsSectionTitle("WhatsApp") }
            item { SettingsRow(Icons.Rounded.Chat, "مشاركة كشف الحساب", "النص والرصيد وملخص الحساب") { infoMessage = "تتم المشاركة مباشرة من شاشة كشف الحساب." } }

            item { SettingsSectionTitle("الأمان") }
            item {
                SettingSwitchRow(
                    icon = Icons.Rounded.Lock,
                    title = "قفل التطبيق",
                    subtitle = "PIN أو بصمة الهاتف عند التفعيل النهائي",
                    checked = appLock,
                    onCheckedChange = { appLock = it }
                )
            }

            item { SettingsSectionTitle("حول التطبيق") }
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("دفتر الغاز", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                Text("الإصدار 1.0.0", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(
                            "تطبيق عربي لإدارة زبائن وديون وتحصيلات موزّع قناني الغاز أثناء الجولة اليومية.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إعادة البيانات التجريبية")
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronLeft, null)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
