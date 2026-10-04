package com.radwan.abosmra.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.radwan.abosmra.GasLedgerViewModel
import com.radwan.abosmra.data.EntryType
import com.radwan.abosmra.data.LedgerEntry
import com.radwan.abosmra.ui.components.EmptyState
import com.radwan.abosmra.ui.components.ScreenTopBar
import com.radwan.abosmra.ui.components.SectionTitle
import com.radwan.abosmra.ui.components.SoftDivider
import com.radwan.abosmra.ui.components.TransactionRow
import com.radwan.abosmra.ui.theme.DebtRed
import com.radwan.abosmra.ui.theme.PaidGreen
import com.radwan.abosmra.util.formatDate
import com.radwan.abosmra.util.formatMoney

@Composable
fun AddDebtScreen(
    vm: GasLedgerViewModel,
    customerId: String,
    onBack: () -> Unit
) {
    val customers by vm.customers.collectAsState()
    val customer = customers.firstOrNull { it.id == customerId }
    if (customer == null) {
        MissingCustomerScreen(onBack)
        return
    }

    val previousBalance = vm.balance(customer)
    var bottles by remember { mutableStateOf("") }
    var bottlePrice by remember { mutableStateOf("25000") }
    var directAmount by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    val bottleCount = bottles.toIntOrNull() ?: 0
    val onePrice = bottlePrice.toLongOrNull() ?: 0L
    val calculatedFromBottles = if (bottleCount > 0 && onePrice > 0) bottleCount * onePrice else 0L
    val debtAmount = directAmount.toLongOrNull()?.takeIf { it > 0 } ?: calculatedFromBottles

    if (saved) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Rounded.CheckCircle, null, tint = PaidGreen) },
            title = { Text("تم تسجيل الدين") },
            text = { Text("أُضيف " + formatMoney(debtAmount) + " إلى حساب " + customer.name + ".") },
            confirmButton = {
                TextButton(onClick = onBack) { Text("تم") }
            }
        )
    }

    Scaffold(
        topBar = { ScreenTopBar("إضافة دين", onBack) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CustomerBalanceHeader(customer.name, previousBalance)

            Text("تفاصيل قناني الغاز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bottles,
                    onValueChange = {
                        bottles = it.filter(Char::isDigit).take(3)
                        directAmount = ""
                        showError = false
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("عدد القناني") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = bottlePrice,
                    onValueChange = {
                        bottlePrice = it.filter(Char::isDigit).take(9)
                        directAmount = ""
                        showError = false
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("سعر القنينة") },
                    suffix = { Text("د.ع") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Text("أو أدخل مبلغ الدين مباشرة", color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = directAmount,
                onValueChange = {
                    directAmount = it.filter(Char::isDigit).take(12)
                    showError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("مبلغ الدين") },
                suffix = { Text("د.ع") },
                singleLine = true,
                isError = showError,
                supportingText = {
                    if (showError) Text("أدخل مبلغًا صحيحًا أكبر من صفر")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            SectionTitle("مبالغ سريعة")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(5_000L, 10_000L, 15_000L).forEach { amount ->
                    OutlinedButton(
                        onClick = {
                            directAmount = amount.toString()
                            bottles = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatMoney(amount))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(20_000L, 25_000L, 50_000L).forEach { amount ->
                    OutlinedButton(
                        onClick = {
                            directAmount = amount.toString()
                            bottles = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatMoney(amount))
                    }
                }
            }

            BalanceEquation(
                firstLabel = "الدين السابق",
                first = previousBalance,
                operator = "+",
                secondLabel = "الدين الجديد",
                second = debtAmount,
                resultLabel = "الدين بعد العملية",
                result = previousBalance + debtAmount,
                resultColorPositive = false
            )

            Button(
                onClick = {
                    if (debtAmount <= 0L) {
                        showError = true
                    } else {
                        vm.addDebt(
                            customerId = customerId,
                            amount = debtAmount,
                            bottles = bottleCount.takeIf { it > 0 && directAmount.isBlank() },
                            bottlePrice = onePrice.takeIf { it > 0 && directAmount.isBlank() }
                        )
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("تأكيد تسجيل الدين", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddPaymentScreen(
    vm: GasLedgerViewModel,
    customerId: String,
    onBack: () -> Unit
) {
    val customers by vm.customers.collectAsState()
    val customer = customers.firstOrNull { it.id == customerId }
    if (customer == null) {
        MissingCustomerScreen(onBack)
        return
    }

    val currentBalance = vm.balance(customer)
    var amountText by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    val amount = amountText.toLongOrNull() ?: 0L
    val tooHigh = amount > currentBalance && amount > 0
    val remaining = (currentBalance - amount).coerceAtLeast(0L)

    if (savedMessage != null) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Rounded.CheckCircle, null, tint = PaidGreen) },
            title = { Text(savedMessage.orEmpty()) },
            text = {
                Text(
                    if (amount == currentBalance) "أصبح رصيد " + customer.name + " صفرًا."
                    else "تم تسجيل " + formatMoney(amount) + " كتحصيل."
                )
            },
            confirmButton = {
                TextButton(onClick = onBack) { Text("تم") }
            }
        )
    }

    Scaffold(
        topBar = { ScreenTopBar("تسجيل تحصيل", onBack) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CustomerBalanceHeader(customer.name, currentBalance)

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter(Char::isDigit).take(12) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("مبلغ التحصيل") },
                suffix = { Text("د.ع") },
                singleLine = true,
                isError = tooHigh,
                supportingText = {
                    when {
                        tooHigh -> Text("لا يمكن تحصيل مبلغ أكبر من الدين الحالي")
                        currentBalance > 0 -> Text("الحد الأعلى: " + formatMoney(currentBalance))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = { amountText = currentBalance.toString() },
                enabled = currentBalance > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تسديد كامل")
            }

            SectionTitle("مبالغ سريعة")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(5_000L, 10_000L, 25_000L).forEach { quick ->
                    OutlinedButton(
                        onClick = { amountText = quick.coerceAtMost(currentBalance).toString() },
                        enabled = currentBalance > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatMoney(quick))
                    }
                }
            }

            BalanceEquation(
                firstLabel = "الدين الحالي",
                first = currentBalance,
                operator = "-",
                secondLabel = "المبلغ المدفوع",
                second = amount,
                resultLabel = "المتبقي",
                result = remaining,
                resultColorPositive = remaining == 0L && amount > 0
            )

            if (remaining == 0L && amount > 0 && !tooHigh) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = PaidGreen)
                        Text("تم تسديد الحساب بالكامل ✓", fontWeight = FontWeight.Bold, color = PaidGreen)
                    }
                }
            }

            Button(
                onClick = {
                    if (vm.addPayment(customerId, amount)) {
                        savedMessage = if (amount == currentBalance) "تم تسديد الحساب بالكامل ✓" else "تم تسجيل التحصيل بنجاح"
                    }
                },
                enabled = amount > 0 && amount <= currentBalance,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("تأكيد التحصيل", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private enum class MovementFilter(val label: String) {
    ALL("الكل"),
    DEBTS("ديون"),
    PAYMENTS("تحصيلات")
}

@Composable
fun CustomerTransactionsScreen(
    vm: GasLedgerViewModel,
    customerId: String,
    onBack: () -> Unit
) {
    val customers by vm.customers.collectAsState()
    val allEntries by vm.entries.collectAsState()
    val customer = customers.firstOrNull { it.id == customerId }
    if (customer == null) {
        MissingCustomerScreen(onBack)
        return
    }

    var filter by remember { mutableStateOf(MovementFilter.ALL) }
    var recentOnly by remember { mutableStateOf(false) }
    val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L

    val entries = allEntries
        .filter { it.customerId == customerId }
        .filter {
            when (filter) {
                MovementFilter.ALL -> true
                MovementFilter.DEBTS -> it.type == EntryType.DEBT
                MovementFilter.PAYMENTS -> it.type == EntryType.PAYMENT
            }
        }
        .filter { !recentOnly || it.createdAt >= thirtyDaysAgo }
        .sortedByDescending { it.createdAt }

    fun balanceAfter(entry: LedgerEntry): Long {
        val movementsUntil = allEntries
            .filter { it.customerId == customerId && it.createdAt <= entry.createdAt }
            .sumOf { if (it.type == EntryType.DEBT) it.amount else -it.amount }
        return (customer.openingDebt + movementsUntil).coerceAtLeast(0L)
    }

    Scaffold(
        topBar = { ScreenTopBar("سجل حركات " + customer.name, onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MovementFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }
            item {
                FilterChip(
                    selected = recentOnly,
                    onClick = { recentOnly = !recentOnly },
                    label = { Text("آخر 30 يومًا") }
                )
            }
            item {
                Text(
                    entries.size.toString() + " حركة",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entries.isEmpty()) {
                item {
                    EmptyState("لا توجد حركات", "غيّر الفلاتر أو ابدأ بإضافة دين جديد.")
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    Card(shape = RoundedCornerShape(18.dp)) {
                        TransactionRow(
                            entry = entry,
                            showBalance = balanceAfter(entry),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatementScreen(
    vm: GasLedgerViewModel,
    customerId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val customers by vm.customers.collectAsState()
    val allEntries by vm.entries.collectAsState()
    val customer = customers.firstOrNull { it.id == customerId }
    if (customer == null) {
        MissingCustomerScreen(onBack)
        return
    }

    val entries = allEntries.filter { it.customerId == customerId }.sortedByDescending { it.createdAt }
    val totalDebts = customer.openingDebt + entries.filter { it.type == EntryType.DEBT }.sumOf { it.amount }
    val totalPaid = entries.filter { it.type == EntryType.PAYMENT }.sumOf { it.amount }
    val balance = vm.balance(customer)

    fun shareStatement() {
        val message = buildString {
            appendLine("السلام عليكم")
            appendLine("كشف حساب الدين")
            appendLine("الزبون: " + customer.name)
            if (customer.area.isNotBlank()) appendLine("المنطقة: " + customer.area)
            appendLine("الدين الحالي: " + formatMoney(balance))
            appendLine("إجمالي الديون: " + formatMoney(totalDebts))
            appendLine("إجمالي المدفوع: " + formatMoney(totalPaid))
            appendLine("شكرًا لحسن تعاملكم 🌹")
        }
        val whatsapp = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }
        val fallback = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            },
            "مشاركة كشف الحساب"
        )
        runCatching { context.startActivity(whatsapp) }
            .onFailure { context.startActivity(fallback) }
    }

    Scaffold(
        topBar = { ScreenTopBar("كشف الحساب", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("دفتر الغاز", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("كشف حساب", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SoftDivider()
                        StatementLine("اسم الزبون", customer.name)
                        StatementLine("رقم الهاتف", customer.phone ?: "غير مضاف")
                        StatementLine("المنطقة", customer.area.ifBlank { "غير محددة" })
                        StatementLine("تاريخ الكشف", formatDate(System.currentTimeMillis()))
                        Spacer(Modifier.height(4.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("الدين الحالي", style = MaterialTheme.typography.titleMedium)
                            Text(
                                formatMoney(balance),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (balance > 0) DebtRed else PaidGreen
                            )
                        }
                        SoftDivider()
                        StatementLine("إجمالي الديون", formatMoney(totalDebts))
                        StatementLine("إجمالي المدفوع", formatMoney(totalPaid))
                        StatementLine("المبلغ المتبقي", formatMoney(balance), bold = true)
                    }
                }
            }

            item { SectionTitle("آخر العمليات") }
            if (entries.isEmpty()) {
                item { Text("لا توجد عمليات مسجلة.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(entries.take(8), key = { it.id }) { entry ->
                    Card(shape = RoundedCornerShape(18.dp)) {
                        TransactionRow(entry, modifier = Modifier.padding(horizontal = 14.dp))
                    }
                }
            }

            item {
                Button(
                    onClick = ::shareStatement,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.Share, null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("مشاركة كشف الحساب عبر WhatsApp", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CustomerBalanceHeader(name: String, balance: Long) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("الدين الحالي: " + formatMoney(balance), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BalanceEquation(
    firstLabel: String,
    first: Long,
    operator: String,
    secondLabel: String,
    second: Long,
    resultLabel: String,
    result: Long,
    resultColorPositive: Boolean
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatementLine(firstLabel, formatMoney(first))
            StatementLine(operator + " " + secondLabel, formatMoney(second))
            SoftDivider()
            StatementLine(
                resultLabel,
                formatMoney(result),
                bold = true,
                valueColor = if (resultColorPositive) PaidGreen else DebtRed
            )
        }
    }
}

@Composable
private fun StatementLine(
    label: String,
    value: String,
    bold: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun MissingCustomerScreen(onBack: () -> Unit) {
    Scaffold(topBar = { ScreenTopBar("الزبون", onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("تعذر العثور على الزبون.")
        }
    }
}
