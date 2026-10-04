package com.radwan.abosmra.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.radwan.abosmra.GasLedgerViewModel
import com.radwan.abosmra.data.Customer
import com.radwan.abosmra.data.EntryType
import com.radwan.abosmra.ui.components.CustomerCard
import com.radwan.abosmra.ui.components.MetricCard
import com.radwan.abosmra.ui.components.QuickActionCard
import com.radwan.abosmra.ui.components.ScreenTopBar
import com.radwan.abosmra.ui.components.SectionTitle
import com.radwan.abosmra.ui.components.StatusChip
import com.radwan.abosmra.ui.components.TransactionRow
import com.radwan.abosmra.ui.theme.DebtRed
import com.radwan.abosmra.ui.theme.PaidGreen
import com.radwan.abosmra.util.formatDate
import com.radwan.abosmra.util.formatMoney
import com.radwan.abosmra.util.normalizeIraqPhone
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    vm: GasLedgerViewModel,
    onCustomers: () -> Unit,
    onAddCustomer: () -> Unit,
    onSearch: () -> Unit,
    onCollections: () -> Unit,
    onDailyDebts: () -> Unit,
    onTopDebtors: () -> Unit,
    onAreas: () -> Unit,
    onFollowUp: () -> Unit,
    onCustomer: (String) -> Unit
) {
    val customers by vm.customers.collectAsState()
    val entries by vm.entries.collectAsState()
    val today = remember {
        val dayName = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE", Locale("ar", "IQ")))
        dayName + "، " + formatDate(System.currentTimeMillis())
    }
    val topDebtors = remember(customers, entries) { vm.topDebtors().take(4) }
    val recent = entries.sortedByDescending { it.createdAt }.take(5)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("أهلًا بك", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("دفتر الغاز", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(today, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    "إجمالي الديون",
                    formatMoney(vm.totalDebt()),
                    Icons.Rounded.Wallet,
                    modifier = Modifier.weight(1f),
                    supporting = vm.indebtedCustomersCount().toString() + " زبون عليهم دين"
                )
                MetricCard(
                    "تحصيلات اليوم",
                    formatMoney(vm.todayCollections()),
                    Icons.Rounded.TrendingUp,
                    modifier = Modifier.weight(1f),
                    supporting = vm.todayEntries(EntryType.PAYMENT).size.toString() + " عملية"
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    "ديون اليوم",
                    formatMoney(vm.todayDebts()),
                    Icons.Rounded.ReceiptLong,
                    modifier = Modifier.weight(1f),
                    supporting = vm.todayEntries(EntryType.DEBT).size.toString() + " عملية"
                )
                MetricCard(
                    "عدد الزبائن",
                    customers.size.toString(),
                    Icons.Rounded.People,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { SectionTitle("الوصول السريع") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionCard("إضافة دين", Icons.Rounded.Add, onSearch, Modifier.weight(1f))
                QuickActionCard("تسجيل تحصيل", Icons.Rounded.Wallet, onSearch, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionCard("إضافة زبون", Icons.Rounded.PersonAdd, onAddCustomer, Modifier.weight(1f))
                QuickActionCard("كشف حساب", Icons.Rounded.ReceiptLong, onSearch, Modifier.weight(1f))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSearch),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("بحث سريع عن زبون", fontWeight = FontWeight.Bold)
                        Text("ابحث بالاسم أو رقم الهاتف أو المنطقة", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            SectionTitle("أعلى الزبائن مديونية", "عرض الكل", onTopDebtors)
        }
        items(topDebtors, key = { it.id }) { customer ->
            CustomerCard(
                customer = customer,
                balance = vm.balance(customer),
                onClick = { onCustomer(customer.id) }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard("المناطق", Icons.Rounded.LocationOn, onAreas, Modifier.weight(1f))
                QuickActionCard("المتابعة", Icons.Rounded.Assessment, onFollowUp, Modifier.weight(1f))
            }
        }

        item {
            SectionTitle("آخر العمليات", "التحصيلات", onCollections)
        }
        items(recent, key = { it.id }) { entry ->
            val customer = customers.firstOrNull { it.id == entry.customerId }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { customer?.let { onCustomer(it.id) } },
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(horizontal = 14.dp)) {
                    if (customer != null) {
                        Text(
                            customer.name,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    TransactionRow(entry)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onDailyDebts, modifier = Modifier.weight(1f)) {
                    Text("ديون اليوم")
                }
                Button(onClick = onCustomers, modifier = Modifier.weight(1f)) {
                    Text("كل الزبائن")
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

private enum class CustomerSort(val label: String) {
    HIGHEST("الأعلى مديونية"),
    NAME("الاسم"),
    LATEST("الأحدث تعاملًا"),
    AREA("حسب المنطقة")
}

@Composable
fun CustomersScreen(
    vm: GasLedgerViewModel,
    onAdd: () -> Unit,
    onCustomer: (String) -> Unit
) {
    val customers by vm.customers.collectAsState()
    val entries by vm.entries.collectAsState()
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(CustomerSort.HIGHEST) }

    val visible = remember(customers, entries, query, sort) {
        val filtered = customers.filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.phone.orEmpty().contains(query) ||
                it.area.contains(query, ignoreCase = true)
        }
        when (sort) {
            CustomerSort.HIGHEST -> filtered.sortedByDescending { vm.balance(it) }
            CustomerSort.NAME -> filtered.sortedBy { it.name }
            CustomerSort.LATEST -> filtered.sortedByDescending { vm.lastEntryFor(it.id)?.createdAt ?: it.createdAt }
            CustomerSort.AREA -> filtered.sortedWith(compareBy<Customer> { it.area }.thenBy { it.name })
        }
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = "الزبائن",
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = "إضافة زبون")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("إضافة زبون") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("بحث بالاسم أو الهاتف أو المنطقة") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    shape = RoundedCornerShape(18.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomerSort.entries.forEach { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { sort = option },
                            label = { Text(option.label, maxLines = 1) }
                        )
                    }
                }
            }
            item {
                Text(
                    visible.size.toString() + " زبون",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(visible, key = { it.id }) { customer ->
                val last = vm.lastEntryFor(customer.id)
                CustomerCard(
                    customer = customer,
                    balance = vm.balance(customer),
                    lastActivity = last?.let { "آخر تعامل " + formatDate(it.createdAt) },
                    onClick = { onCustomer(customer.id) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AddCustomerScreen(
    vm: GasLedgerViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var openingDebt by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { ScreenTopBar("إضافة زبون جديد", onBack) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "بيانات الزبون",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    showError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("اسم الزبون *") },
                singleLine = true,
                isError = showError && name.isBlank(),
                supportingText = {
                    if (showError && name.isBlank()) Text("اسم الزبون مطلوب")
                }
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter(Char::isDigit).take(11) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("رقم الهاتف") },
                placeholder = { Text("07XXXXXXXXX") },
                supportingText = { Text("يدعم أرقام العراق، وسيُستخدم +964 عند المشاركة") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("المنطقة / الحي") },
                singleLine = true
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("العنوان أو وصف المكان") },
                minLines = 2
            )
            OutlinedTextField(
                value = openingDebt,
                onValueChange = { openingDebt = it.filter(Char::isDigit).take(12) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("المبلغ السابق / الدين الافتتاحي") },
                suffix = { Text("د.ع") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ملاحظات مختصرة") },
                minLines = 2
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        val customer = vm.addCustomer(
                            name = name,
                            phone = phone.takeIf { it.isNotBlank() },
                            area = area,
                            address = address,
                            openingDebt = openingDebt.toLongOrNull() ?: 0L,
                            notes = notes
                        )
                        onSaved(customer.id)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("حفظ الزبون", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomerProfileScreen(
    vm: GasLedgerViewModel,
    customerId: String,
    onBack: () -> Unit,
    onAddDebt: () -> Unit,
    onPayment: () -> Unit,
    onTransactions: () -> Unit,
    onStatement: () -> Unit
) {
    val customers by vm.customers.collectAsState()
    val entries by vm.entries.collectAsState()
    val customer = customers.firstOrNull { it.id == customerId }
    val context = LocalContext.current

    if (customer == null) {
        Scaffold(topBar = { ScreenTopBar("الزبون", onBack) }) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("تعذر العثور على هذا الزبون")
            }
        }
        return
    }

    val balance = vm.balance(customer)
    val customerEntries = entries.filter { it.customerId == customerId }.sortedByDescending { it.createdAt }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = customer.name,
                onBack = onBack,
                actions = {
                    if (!customer.phone.isNullOrBlank()) {
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customer.phone)))
                        }) {
                            Icon(Icons.Rounded.Call, contentDescription = "اتصال")
                        }
                        IconButton(onClick = {
                            val phone = normalizeIraqPhone(customer.phone)
                            val uri = Uri.parse("https://wa.me/" + phone.removePrefix("+"))
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }) {
                            Icon(Icons.Rounded.Chat, contentDescription = "WhatsApp")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(customer.area.ifBlank { "بدون منطقة" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(customer.phone ?: "بدون رقم هاتف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (customer.address.isNotBlank()) {
                        Text(customer.address, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (balance > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("الدين الحالي", style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatMoney(balance),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (balance > 0) DebtRed else PaidGreen
                        )
                        StatusChip(
                            text = if (balance == 0L) "الحساب مسدد بالكامل" else "حساب مفتوح",
                            positive = balance == 0L
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddDebt, modifier = Modifier.weight(1f)) {
                        Text("إضافة دين")
                    }
                    Button(
                        onClick = onPayment,
                        enabled = balance > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تسجيل تحصيل")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard("كشف الحساب", Icons.Rounded.ReceiptLong, onStatement, Modifier.weight(1f))
                    QuickActionCard("كل الحركات", Icons.Rounded.Assessment, onTransactions, Modifier.weight(1f))
                }
            }

            item {
                SectionTitle("آخر الحركات", "عرض السجل", onTransactions)
            }
            if (customerEntries.isEmpty()) {
                item {
                    Text("لا توجد حركات مالية مسجلة بعد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(customerEntries.take(6), key = { it.id }) { entry ->
                    Card(shape = RoundedCornerShape(18.dp)) {
                        TransactionRow(entry, modifier = Modifier.padding(horizontal = 14.dp))
                    }
                }
            }

            item {
                SectionTitle("معلومات الزبون")
            }
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileInfo("المنطقة", customer.area.ifBlank { "غير محددة" })
                        ProfileInfo("العنوان", customer.address.ifBlank { "غير مضاف" })
                        ProfileInfo("رقم الهاتف", customer.phone ?: "غير مضاف")
                        ProfileInfo("الدين الافتتاحي", formatMoney(customer.openingDebt))
                        if (customer.notes.isNotBlank()) ProfileInfo("ملاحظات", customer.notes)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}
