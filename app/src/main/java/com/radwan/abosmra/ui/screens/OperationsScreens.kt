package com.radwan.abosmra.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radwan.abosmra.GasLedgerViewModel
import com.radwan.abosmra.data.Customer
import com.radwan.abosmra.data.EntryType
import com.radwan.abosmra.ui.components.CustomerCard
import com.radwan.abosmra.ui.components.EmptyState
import com.radwan.abosmra.ui.components.MetricCard
import com.radwan.abosmra.ui.components.ScreenTopBar
import com.radwan.abosmra.ui.components.SectionTitle
import com.radwan.abosmra.ui.theme.DebtRed
import com.radwan.abosmra.ui.theme.PaidGreen
import com.radwan.abosmra.util.formatDate
import com.radwan.abosmra.util.formatMoney
import com.radwan.abosmra.util.formatTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun dayOf(timestamp: Long): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCollectionsScreen(
    vm: GasLedgerViewModel,
    onCustomer: (String) -> Unit
) {
    val customers by vm.customers.collectAsState()
    val entries by vm.entries.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showPicker by remember { mutableStateOf(false) }

    val payments = entries
        .filter { it.type == EntryType.PAYMENT && dayOf(it.createdAt) == selectedDate }
        .sortedByDescending { it.createdAt }
    val total = payments.sumOf { it.amount }
    val uniqueCustomers = payments.map { it.customerId }.distinct().size
    val largest = payments.maxOfOrNull { it.amount } ?: 0L

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showPicker = false
                }) { Text("اختيار") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("إلغاء") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        topBar = { ScreenTopBar("التحصيلات اليومية") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Button(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.CalendarMonth, null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(
                        if (selectedDate == LocalDate.now()) "اليوم • " + formatDate(System.currentTimeMillis())
                        else selectedDate.toString()
                    )
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("إجمالي التحصيلات", style = MaterialTheme.typography.titleMedium)
                        Text(formatMoney(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = PaidGreen)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("عدد العمليات", payments.size.toString(), Icons.Rounded.ReceiptLong, Modifier.weight(1f))
                    MetricCard("الزبائن الذين دفعوا", uniqueCustomers.toString(), Icons.Rounded.People, Modifier.weight(1f))
                }
            }
            item {
                MetricCard("أكبر عملية تحصيل", formatMoney(largest), Icons.Rounded.TrendingUp, Modifier.fillMaxWidth())
            }
            item { SectionTitle("عمليات اليوم") }

            if (payments.isEmpty()) {
                item {
                    EmptyState("لا توجد تحصيلات", "لا توجد عمليات تحصيل مسجلة في هذا التاريخ.")
                }
            } else {
                items(payments, key = { it.id }) { entry ->
                    val customer = customers.firstOrNull { it.id == entry.customerId }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { customer?.let { onCustomer(it.id) } },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer?.name ?: "زبون", fontWeight = FontWeight.Bold)
                                Text(
                                    (customer?.area?.takeIf { it.isNotBlank() } ?: "بدون منطقة") + " • " + formatTime(entry.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(formatMoney(entry.amount), fontWeight = FontWeight.ExtraBold, color = PaidGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyDebtsScreen(
    vm: GasLedgerViewModel,
    onBack: () -> Unit,
    onCustomer: (String) -> Unit
) {
    val customers by vm.customers.collectAsState()
    val entries by vm.entries.collectAsState()
    val today = LocalDate.now()
    val debts = entries.filter { it.type == EntryType.DEBT && dayOf(it.createdAt) == today }.sortedByDescending { it.createdAt }
    val total = debts.sumOf { it.amount }
    val bottles = debts.sumOf { it.bottles ?: 0 }
    val customerCount = debts.map { it.customerId }.distinct().size
    val average = if (debts.isEmpty()) 0L else total / debts.size

    Scaffold(
        topBar = { ScreenTopBar("ديون اليوم", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("إجمالي ديون اليوم", style = MaterialTheme.typography.titleMedium)
                        Text(formatMoney(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = DebtRed)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("عدد العمليات", debts.size.toString(), Icons.Rounded.ReceiptLong, Modifier.weight(1f))
                    MetricCard("عدد القناني", bottles.toString(), Icons.Rounded.LocalShipping, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("عدد الزبائن", customerCount.toString(), Icons.Rounded.People, Modifier.weight(1f))
                    MetricCard("متوسط العملية", formatMoney(average), Icons.Rounded.TrendingUp, Modifier.weight(1f))
                }
            }
            item { SectionTitle("عمليات البيع بالدين") }

            if (debts.isEmpty()) {
                item { EmptyState("لا توجد ديون اليوم", "عمليات البيع بالدين ستظهر هنا بمجرد تسجيلها.") }
            } else {
                items(debts, key = { it.id }) { entry ->
                    val customer = customers.firstOrNull { it.id == entry.customerId }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { customer?.let { onCustomer(it.id) } },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer?.name ?: "زبون", fontWeight = FontWeight.Bold)
                                Text(
                                    (entry.bottles?.toString()?.plus(" قنينة • ") ?: "") + formatTime(entry.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(formatMoney(entry.amount), fontWeight = FontWeight.ExtraBold, color = DebtRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopDebtorsScreen(
    vm: GasLedgerViewModel,
    onBack: () -> Unit,
    onCustomer: (String) -> Unit
) {
    val customers by vm.customers.collectAsState()
    val entries by vm.entries.collectAsState()
    var selectedArea by remember { mutableStateOf("الكل") }
    val areas = listOf("الكل") + customers.map { it.area }.filter { it.isNotBlank() }.distinct().sorted()
    val debtors = remember(customers, entries, selectedArea) {
        vm.topDebtors().filter { selectedArea == "الكل" || it.area == selectedArea }
    }
    val maxDebt = debtors.maxOfOrNull { vm.balance(it) }?.coerceAtLeast(1L) ?: 1L

    Scaffold(
        topBar = { ScreenTopBar("أعلى الزبائن مديونية", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    areas.take(5).forEach { area ->
                        FilterChip(
                            selected = selectedArea == area,
                            onClick = { selectedArea = area },
                            label = { Text(area) }
                        )
                    }
                }
            }
            items(debtors, key = { it.id }) { customer ->
                val index = debtors.indexOf(customer) + 1
                val balance = vm.balance(customer)
                val lastPayment = vm.lastPaymentFor(customer.id)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onCustomer(customer.id) },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(index.toString() + ".", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer.name, fontWeight = FontWeight.Bold)
                                Text(customer.area.ifBlank { "بدون منطقة" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatMoney(balance), color = DebtRed, fontWeight = FontWeight.ExtraBold)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                .then(
                                    Modifier
                                )
                        ) {
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { balance.toFloat() / maxDebt.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text(
                            "آخر تحصيل: " + (lastPayment?.let { formatDate(it.createdAt) + " • " + formatMoney(it.amount) } ?: "لا يوجد"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AreasScreen(
    vm: GasLedgerViewModel,
    onBack: () -> Unit,
    onCustomer: (String) -> Unit
) {
    val customers by vm.customers.collectAsState()
    val entries by vm.entries.collectAsState()
    var expandedArea by remember { mutableStateOf<String?>(null) }

    val grouped = customers.groupBy { it.area.ifBlank { "غير محددة" } }
        .toList()
        .sortedByDescending { (_, list) -> list.sumOf { vm.balance(it) } }

    Scaffold(
        topBar = { ScreenTopBar("المناطق / الأحياء", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "رتّب جولتك حسب المناطق والحسابات المفتوحة.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            grouped.forEach { (area, areaCustomers) ->
                val areaDebt = areaCustomers.sumOf { vm.balance(it) }
                val ids = areaCustomers.map { it.id }.toSet()
                val todayCollections = entries
                    .filter {
                        it.customerId in ids &&
                            it.type == EntryType.PAYMENT &&
                            dayOf(it.createdAt) == LocalDate.now()
                    }
                    .sumOf { it.amount }

                item(key = "area-" + area) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            expandedArea = if (expandedArea == area) null else area
                        },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(area, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(areaCustomers.size.toString() + " زبون", style = MaterialTheme.typography.bodySmall)
                                Text("الديون " + formatMoney(areaDebt), style = MaterialTheme.typography.bodySmall, color = DebtRed)
                            }
                            Text("تحصيلات اليوم: " + formatMoney(todayCollections), style = MaterialTheme.typography.labelMedium, color = PaidGreen)
                            Text(
                                if (expandedArea == area) "إخفاء زبائن المنطقة" else "عرض زبائن المنطقة",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (expandedArea == area) {
                    items(areaCustomers.sortedByDescending { vm.balance(it) }, key = { "area-customer-" + it.id }) { customer ->
                        CustomerCard(customer, vm.balance(customer), onClick = { onCustomer(customer.id) })
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
