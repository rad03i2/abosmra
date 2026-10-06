package com.radwan.abosmra.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radwan.abosmra.GasLedgerViewModel
import com.radwan.abosmra.data.DebtAnomalyWarning
import com.radwan.abosmra.data.DebtCreateResult
import com.radwan.abosmra.notifications.FinancialOperationFeedback
import com.radwan.abosmra.notifications.FinancialOperationKind
import com.radwan.abosmra.notifications.FinancialOperationReceipt
import com.radwan.abosmra.speech.ArabicDebtAmountParser
import com.radwan.abosmra.speech.DebtSpeechError
import com.radwan.abosmra.speech.DebtSpeechRecognizer
import com.radwan.abosmra.speech.SpeechAmountParseResult
import com.radwan.abosmra.ui.components.ScreenTopBar
import com.radwan.abosmra.ui.components.SoftDivider
import com.radwan.abosmra.ui.theme.DebtRed
import com.radwan.abosmra.ui.theme.PaidGreen
import com.radwan.abosmra.util.formatMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MICROPHONE_PERMISSION_V29 = "android.permission." + "RECORD_AUDIO"

private data class DebtDraftV29(
    val amount: Long,
    val bottles: Int?,
    val bottlePrice: Long?,
    val balanceBefore: Long
)

private enum class DebtModeV12(val label: String) {
    AMOUNT("مبلغ مباشر"),
    BOTTLES("حسب القناني")
}

@Composable
fun AddDebtScreenV12(
    vm: GasLedgerViewModel,
    customerId: String,
    onBack: () -> Unit
) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    val customer = customers.firstOrNull { it.id == customerId }
    if (customer == null) {
        V12MissingCustomer(onBack)
        return
    }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val sendFinancialFeedback = rememberFinancialFeedbackHandler(vm)
    val previousBalance = vm.balance(customer)

    var modeName by rememberSaveable { mutableStateOf(DebtModeV12.AMOUNT.name) }
    val mode = DebtModeV12.valueOf(modeName)
    var amountText by rememberSaveable { mutableStateOf("") }
    var bottles by rememberSaveable { mutableStateOf("") }
    var bottlePrice by rememberSaveable { mutableStateOf("25000") }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var savedAmount by rememberSaveable { mutableStateOf<Long?>(null) }

    val bottleCount = bottles.toIntOrNull() ?: 0
    val price = bottlePrice.toLongOrNull() ?: 0L
    val amount = if (mode == DebtModeV12.AMOUNT) {
        amountText.toLongOrNull() ?: 0L
    } else {
        if (bottleCount > 0 && price > 0) bottleCount * price else 0L
    }
    val canSubmit = amount > 0L && !isSaving

    fun submit() {
        if (isSaving) return
        if (amount <= 0L) {
            errorText = if (mode == DebtModeV12.AMOUNT) {
                "أدخل مبلغًا صحيحًا."
            } else {
                "أدخل عدد القناني وسعرها."
            }
            return
        }

        focusManager.clearFocus()
        isSaving = true
        errorText = null

        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    vm.addDebt(
                        customerId = customerId,
                        amount = amount,
                        bottles = bottleCount.takeIf { mode == DebtModeV12.BOTTLES && it > 0 },
                        bottlePrice = price.takeIf { mode == DebtModeV12.BOTTLES && it > 0 }
                    )
                }
            }
            isSaving = false
            if (result.isSuccess) {
                savedAmount = amount
                sendFinancialFeedback(
                    FinancialOperationReceipt(
                        kind = FinancialOperationKind.DEBT,
                        customerId = customer.id,
                        customerName = customer.name,
                        amount = amount,
                        balanceAfter = previousBalance + amount
                    )
                )
            } else {
                errorText = "تعذر حفظ الدين. حاول مرة أخرى."
            }
        }
    }

    savedAmount?.let { saved ->
        V12SuccessDialog(
            title = "تم تسجيل الدين",
            message = "أضيف " + formatMoney(saved) + " إلى حساب " + customer.name,
            onDone = onBack
        )
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                if (isSaving) "جاري الحفظ..." else "إضافة دين",
                if (isSaving) null else onBack
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Button(
                    onClick = ::submit,
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth().imePadding().padding(12.dp, 10.dp, 12.dp, 12.dp).height(58.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("جاري تسجيل الدين...", modifier = Modifier.padding(horizontal = 8.dp))
                    } else {
                        Text("تسجيل الدين", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp, 4.dp, 14.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                V12FinanceHero(
                    customerName = customer.name,
                    label = "الدين الحالي",
                    amount = previousBalance,
                    positive = false
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DebtModeV12.entries.forEach { item ->
                        FilterChip(
                            selected = mode == item,
                            onClick = {
                                if (!isSaving) {
                                    modeName = item.name
                                    errorText = null
                                }
                            },
                            label = { Text(item.label) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                    }
                }
            }

            if (mode == DebtModeV12.AMOUNT) {
                item {
                    V12AmountInput(
                        value = amountText,
                        onValueChange = {
                            amountText = it.filter(Char::isDigit).take(12)
                            errorText = null
                        },
                        label = "مبلغ الدين",
                        helper = errorText ?: "أدخل المبلغ ثم اضغط تم من لوحة الأرقام للحفظ.",
                        isError = errorText != null,
                        enabled = !isSaving,
                        onDone = ::submit
                    )
                }
                item {
                    V12QuickAmounts(
                        values = listOf(5_000L, 10_000L, 15_000L, 20_000L, 25_000L, 50_000L),
                        selected = amountText.toLongOrNull(),
                        enabled = !isSaving
                    ) {
                        amountText = it.toString()
                        errorText = null
                    }
                }
            } else {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("تفاصيل القناني", style = MaterialTheme.typography.titleMedium)

                            OutlinedTextField(
                                value = bottles,
                                onValueChange = {
                                    bottles = it.filter(Char::isDigit).take(3)
                                    errorText = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("عدد القناني") },
                                enabled = !isSaving,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                shape = MaterialTheme.shapes.large
                            )

                            OutlinedTextField(
                                value = bottlePrice,
                                onValueChange = {
                                    bottlePrice = it.filter(Char::isDigit).take(9)
                                    errorText = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("سعر القنينة") },
                                suffix = { Text("د.ع") },
                                enabled = !isSaving,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { submit() }),
                                shape = MaterialTheme.shapes.large
                            )

                            SoftDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الإجمالي", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatMoney(amount),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DebtRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            errorText?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                V12Equation(
                    firstLabel = "الرصيد السابق",
                    first = previousBalance,
                    operator = "+",
                    secondLabel = "الدين الجديد",
                    second = amount,
                    resultLabel = "الرصيد بعد التسجيل",
                    result = previousBalance + amount,
                    positive = false
                )
            }
        }
    }
}

@Composable
fun AddPaymentScreenV12(
    vm: GasLedgerViewModel,
    customerId: String,
    onBack: () -> Unit
) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    val customer = customers.firstOrNull { it.id == customerId }
    if (customer == null) {
        V12MissingCustomer(onBack)
        return
    }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val sendFinancialFeedback = rememberFinancialFeedbackHandler(vm)
    val currentBalance = vm.balance(customer)
    var amountText by rememberSaveable { mutableStateOf("") }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var savedAmount by rememberSaveable { mutableStateOf<Long?>(null) }

    val amount = amountText.toLongOrNull() ?: 0L
    val tooHigh = amount > currentBalance && amount > 0
    val remaining = (currentBalance - amount).coerceAtLeast(0L)
    val canSubmit = amount > 0L && !tooHigh && !isSaving && currentBalance > 0L

    fun submit() {
        if (!canSubmit) {
            if (tooHigh) errorText = "المبلغ أكبر من الدين الحالي."
            return
        }

        focusManager.clearFocus()
        isSaving = true
        errorText = null

        scope.launch {
            val success = runCatching {
                withContext(Dispatchers.IO) {
                    vm.addPayment(customerId, amount)
                }
            }.getOrDefault(false)

            isSaving = false
            if (success) {
                savedAmount = amount
                sendFinancialFeedback(
                    FinancialOperationReceipt(
                        kind = if (amount == currentBalance) {
                            FinancialOperationKind.FULL_SETTLEMENT
                        } else {
                            FinancialOperationKind.PAYMENT
                        },
                        customerId = customer.id,
                        customerName = customer.name,
                        amount = amount,
                        balanceAfter = remaining
                    )
                )
            } else {
                errorText = "تعذر تسجيل التحصيل. تحقق من الرصيد وحاول مرة أخرى."
            }
        }
    }

    savedAmount?.let { saved ->
        V12SuccessDialog(
            title = if (saved == currentBalance) "تم تسديد الحساب" else "تم تسجيل التحصيل",
            message = if (saved == currentBalance) {
                "أصبح رصيد " + customer.name + " صفرًا."
            } else {
                "تم تحصيل " + formatMoney(saved) + " من " + customer.name
            },
            onDone = onBack
        )
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                if (isSaving) "جاري الحفظ..." else "تسجيل تحصيل",
                if (isSaving) null else onBack
            )
        },
        bottomBar = {
            if (currentBalance > 0L) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Button(
                        onClick = ::submit,
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth().padding(12.dp, 10.dp, 12.dp, 12.dp).height(58.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text("جاري تسجيل التحصيل...", modifier = Modifier.padding(horizontal = 8.dp))
                        } else {
                            Text(
                                if (remaining == 0L && amount > 0L) "تحصيل وتسديد كامل"
                                else "تأكيد التحصيل",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp, 4.dp, 14.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                V12FinanceHero(
                    customerName = customer.name,
                    label = "الدين الحالي",
                    amount = currentBalance,
                    positive = currentBalance == 0L
                )
            }

            if (currentBalance == 0L) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Check, null, tint = PaidGreen)
                            Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                                Text("الحساب مسدد بالكامل", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "لا يوجد مبلغ مطلوب من هذا الزبون.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    V12AmountInput(
                        value = amountText,
                        onValueChange = {
                            amountText = it.filter(Char::isDigit).take(12)
                            errorText = null
                        },
                        label = "المبلغ المستلم",
                        helper = when {
                            tooHigh -> "لا يمكن أن يتجاوز " + formatMoney(currentBalance)
                            errorText != null -> errorText.orEmpty()
                            else -> "أدخل المبلغ واضغط تم للحفظ مباشرة."
                        },
                        isError = tooHigh || errorText != null,
                        enabled = !isSaving,
                        onDone = ::submit
                    )
                }

                item {
                    V12QuickAmounts(
                        values = listOf(
                            5_000L,
                            10_000L,
                            25_000L,
                            currentBalance
                        ).filter { it <= currentBalance }.distinct(),
                        selected = amountText.toLongOrNull(),
                        fullAmount = currentBalance,
                        enabled = !isSaving
                    ) {
                        amountText = it.toString()
                        errorText = null
                    }
                }

                item {
                    V12Equation(
                        firstLabel = "الدين الحالي",
                        first = currentBalance,
                        operator = "-",
                        secondLabel = "المبلغ المستلم",
                        second = amount,
                        resultLabel = "المتبقي",
                        result = remaining,
                        positive = remaining == 0L && amount > 0L && !tooHigh
                    )
                }

                if (remaining == 0L && amount > 0L && !tooHigh) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Check, null, tint = PaidGreen)
                                Text(
                                    "هذه الدفعة ستغلق الحساب بالكامل.",
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = PaidGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V12FinanceHero(
    customerName: String,
    label: String,
    amount: Long,
    positive: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (positive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(customerName, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatMoney(amount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (positive) PaidGreen else DebtRed
            )
        }
    }
}

@Composable
private fun V12AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helper: String,
    isError: Boolean,
    enabled: Boolean,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text("د.ع") },
        enabled = enabled,
        singleLine = true,
        isError = isError,
        supportingText = {
            Text(
                helper,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Start),
        shape = MaterialTheme.shapes.large
    )
}

@Composable
private fun V12QuickAmounts(
    values: List<Long>,
    selected: Long?,
    fullAmount: Long? = null,
    enabled: Boolean,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            "اختيار سريع",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            values.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    enabled = enabled,
                    modifier = Modifier.height(48.dp),
                    label = {
                        Text(
                            if (fullAmount != null && value == fullAmount) {
                                "كامل • " + formatMoney(value)
                            } else {
                                formatMoney(value)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun V12Equation(
    firstLabel: String,
    first: Long,
    operator: String,
    secondLabel: String,
    second: Long,
    resultLabel: String,
    result: Long,
    positive: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            V12EquationLine(firstLabel, formatMoney(first))
            V12EquationLine(operator + " " + secondLabel, formatMoney(second))
            SoftDivider()
            V12EquationLine(
                resultLabel,
                formatMoney(result),
                true,
                if (positive) PaidGreen else DebtRed
            )
        }
    }
}

@Composable
private fun V12EquationLine(
    label: String,
    value: String,
    bold: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = color,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun V12SuccessDialog(
    title: String,
    message: String,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                V28AnimatedSuccessMark()

                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Text(
                    "تم حفظ العملية بنجاح",
                    style = MaterialTheme.typography.labelLarge,
                    color = PaidGreen,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        "موافق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun V28AnimatedSuccessMark() {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1_000)
        )
    }

    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        val green = PaidGreen
        val track = MaterialTheme.colorScheme.primaryContainer

        Canvas(modifier = Modifier.size(88.dp)) {
            drawCircle(
                color = track,
                radius = size.minDimension / 2f
            )
            drawArc(
                color = green,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                style = Stroke(
                    width = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        if (progress.value >= 0.98f) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = PaidGreen,
                modifier = Modifier.size(46.dp)
            )
        }
    }
}

@Composable
private fun rememberFinancialFeedbackHandler(
    vm: GasLedgerViewModel
): (FinancialOperationReceipt) -> Unit {
    val context = LocalContext.current
    var pendingNotification by remember {
        mutableStateOf<FinancialOperationReceipt?>(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val receipt = pendingNotification
        pendingNotification = null
        if (granted && receipt != null) {
            vm.scheduleFinancialOperationNotification(receipt)
        }
    }

    return { receipt ->
        vm.playFinancialSuccessSound()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotification = receipt
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (FinancialOperationFeedback.canPostNotifications(context)) {
            vm.scheduleFinancialOperationNotification(receipt)
        }
    }
}

@Composable
private fun V12MissingCustomer(onBack: () -> Unit) {
    Scaffold(topBar = { ScreenTopBar("الزبون", onBack) }) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("تعذر العثور على الزبون.")
        }
    }
}
