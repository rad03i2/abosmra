package com.radwan.abosmra.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.radwan.abosmra.GasLedgerViewModel
import com.radwan.abosmra.ui.components.ScreenTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddCustomerScreenV12(
    vm: GasLedgerViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var openingDebt by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        if (isSaving) return
        if (name.isBlank()) {
            errorText = "اسم الزبون مطلوب."
            return
        }

        focusManager.clearFocus()
        errorText = null
        isSaving = true

        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    vm.addCustomer(
                        name = name.trim(),
                        phone = phone.takeIf { it.isNotBlank() },
                        area = area.trim(),
                        address = address.trim(),
                        openingDebt = openingDebt.toLongOrNull() ?: 0L,
                        notes = notes.trim()
                    )
                }
            }
            isSaving = false
            result.onSuccess { onSaved(it.id) }
                .onFailure { errorText = "تعذر حفظ الزبون. حاول مرة أخرى." }
        }
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                if (isSaving) "جاري الحفظ..." else "زبون جديد",
                if (isSaving) null else onBack
            )
        },
        bottomBar = {
            Button(
                onClick = ::submit,
                enabled = !isSaving && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(12.dp).height(58.dp),
                shape = MaterialTheme.shapes.large
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text("جاري حفظ الزبون...")
                } else {
                    Text("حفظ الزبون")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(14.dp, 6.dp, 14.dp, 18.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "أدخل الأساسيات أولًا. الحقول الأخرى اختيارية ويمكن تعديلها لاحقًا.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("اسم الزبون *") },
                        singleLine = true,
                        enabled = !isSaving,
                        isError = errorText != null && name.isBlank(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter(Char::isDigit).take(11) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رقم الهاتف") },
                        placeholder = { Text("07XXXXXXXXX") },
                        singleLine = true,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("المنطقة / الحي") },
                        singleLine = true,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("وصف المكان") },
                        minLines = 2,
                        enabled = !isSaving
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = openingDebt,
                        onValueChange = { openingDebt = it.filter(Char::isDigit).take(12) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("الدين السابق") },
                        suffix = { Text("د.ع") },
                        singleLine = true,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("ملاحظات") },
                        minLines = 2,
                        enabled = !isSaving
                    )
                }
            }

            errorText?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
