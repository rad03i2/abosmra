package com.radwan.abosmra

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.radwan.abosmra.data.AppRepository
import com.radwan.abosmra.data.AutoBackupInterval
import com.radwan.abosmra.data.BackupPreview
import com.radwan.abosmra.data.BackupRestoreResult
import com.radwan.abosmra.data.Customer
import com.radwan.abosmra.data.EntryType
import com.radwan.abosmra.data.LedgerEntry
import com.radwan.abosmra.data.MutationResult
import com.radwan.abosmra.security.AppSecurityStore
import com.radwan.abosmra.security.SecurityMutationResult
import com.radwan.abosmra.security.SecurityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GasLedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val security = AppSecurityStore(application)

    private val _securityState = MutableStateFlow(security.state())
    val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()

    private val _isUnlocked = MutableStateFlow(!security.isPinEnabled())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _customers = MutableStateFlow(repository.customers())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _entries = MutableStateFlow(repository.entries())
    val entries: StateFlow<List<LedgerEntry>> = _entries.asStateFlow()

    // Expensive ledger calculations are indexed once after every write instead of
    // re-filtering the entire history on every card recomposition.
    private var balanceByCustomer: Map<String, Long> = emptyMap()
    private var entriesByCustomer: Map<String, List<LedgerEntry>> = emptyMap()
    private var lastEntryByCustomer: Map<String, LedgerEntry> = emptyMap()
    private var lastPaymentByCustomer: Map<String, LedgerEntry> = emptyMap()

    init {
        rebuildIndexes()
    }

    fun customer(id: String): Customer? = _customers.value.firstOrNull { it.id == id }

    fun balance(customer: Customer): Long =
        balanceByCustomer[customer.id] ?: customer.openingDebt.coerceAtLeast(0L)

    fun entriesFor(customerId: String): List<LedgerEntry> =
        entriesByCustomer[customerId].orEmpty()

    fun addCustomer(
        name: String,
        phone: String?,
        area: String,
        address: String,
        openingDebt: Long,
        notes: String
    ): Customer {
        val customer = repository.addCustomer(name, phone, area, address, openingDebt, notes)
        refresh()
        return customer
    }

    fun updateCustomer(
        customerId: String,
        name: String,
        phone: String?,
        area: String,
        address: String,
        openingDebt: Long,
        notes: String
    ): MutationResult {
        val result = repository.updateCustomer(
            customerId, name, phone, area, address, openingDebt, notes
        )
        if (result.success) refresh()
        return result
    }

    fun deleteCustomer(customerId: String): MutationResult {
        val result = repository.deleteCustomer(customerId)
        if (result.success) refresh()
        return result
    }

    fun addDebt(
        customerId: String,
        amount: Long,
        bottles: Int?,
        bottlePrice: Long?
    ) {
        repository.addDebt(customerId, amount, bottles, bottlePrice, "قناني غاز")
        refresh()
    }

    fun addPayment(customerId: String, amount: Long): Boolean {
        val customer = customer(customerId) ?: return false
        if (amount <= 0 || amount > balance(customer)) return false
        repository.addPayment(customerId, amount)
        refresh()
        return true
    }

    fun updateEntry(
        entryId: String,
        amount: Long,
        bottles: Int?,
        bottlePrice: Long?,
        details: String
    ): MutationResult {
        val result = repository.updateEntry(entryId, amount, bottles, bottlePrice, details)
        if (result.success) refresh()
        return result
    }

    fun deleteEntry(entryId: String): MutationResult {
        val result = repository.deleteEntry(entryId)
        if (result.success) refresh()
        return result
    }

    fun totalDebt(): Long = balanceByCustomer.values.sum()

    fun indebtedCustomersCount(): Int = balanceByCustomer.values.count { it > 0L }

    fun todayEntries(type: EntryType? = null): List<LedgerEntry> {
        val today = LocalDate.now()
        return _entries.value.asSequence()
            .filter { entry ->
                val date = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                date == today && (type == null || entry.type == type)
            }
            .sortedByDescending { it.createdAt }
            .toList()
    }

    fun todayCollections(): Long = todayEntries(EntryType.PAYMENT).sumOf { it.amount }

    fun todayDebts(): Long = todayEntries(EntryType.DEBT).sumOf { it.amount }

    fun topDebtors(): List<Customer> =
        _customers.value.asSequence()
            .filter { balanceByCustomer[it.id].orZero() > 0L }
            .sortedByDescending { balanceByCustomer[it.id].orZero() }
            .toList()

    fun lastEntryFor(customerId: String): LedgerEntry? = lastEntryByCustomer[customerId]

    fun lastPaymentFor(customerId: String): LedgerEntry? = lastPaymentByCustomer[customerId]

    fun exportJson(): String = repository.exportJson()

    fun createBackupJson(): String = repository.createBackupJson()

    fun previewBackup(raw: String): BackupPreview =
        repository.previewBackup(raw)

    fun restoreBackup(raw: String): BackupRestoreResult {
        val result = repository.restoreBackup(raw)
        if (result.success) refresh()
        return result
    }

    fun markManualBackupCreated(timestamp: Long = System.currentTimeMillis()) {
        repository.markManualBackupCreated(timestamp)
    }

    fun lastBackupAt(): Long = repository.lastBackupAt()

    fun autoBackupInterval(): AutoBackupInterval =
        repository.autoBackupInterval()

    fun setAutoBackupInterval(interval: AutoBackupInterval) {
        repository.setAutoBackupInterval(interval)
    }

    fun unlockWithPin(pin: String): SecurityMutationResult {
        val success = security.verifyPin(pin)
        if (success) {
            _isUnlocked.value = true
            return SecurityMutationResult(true, "تم فتح التطبيق.")
        }
        return SecurityMutationResult(false, "PIN غير صحيح.")
    }

    fun unlockWithBiometric() {
        if (_securityState.value.pinEnabled && _securityState.value.biometricEnabled) {
            _isUnlocked.value = true
        }
    }

    fun lockNow() {
        if (_securityState.value.pinEnabled) _isUnlocked.value = false
    }

    fun onAppBackgrounded() {
        security.markBackgrounded()
    }

    fun onAppForegrounded() {
        refreshSecurityState()
        if (security.shouldLockOnForeground()) {
            _isUnlocked.value = false
        }
    }

    fun setPin(pin: String): SecurityMutationResult {
        val result = security.setPin(pin)
        if (result.success) {
            _isUnlocked.value = true
            refreshSecurityState()
        }
        return result
    }

    fun changePin(currentPin: String, newPin: String): SecurityMutationResult {
        val result = security.changePin(currentPin, newPin)
        if (result.success) refreshSecurityState()
        return result
    }

    fun disablePin(currentPin: String): SecurityMutationResult {
        val result = security.disablePin(currentPin)
        if (result.success) {
            _isUnlocked.value = true
            refreshSecurityState()
        }
        return result
    }

    fun setBiometricEnabled(enabled: Boolean) {
        security.setBiometricEnabled(enabled)
        refreshSecurityState()
    }

    fun setHideAmounts(enabled: Boolean) {
        security.setHideAmounts(enabled)
        refreshSecurityState()
    }

    fun setSecureScreen(enabled: Boolean) {
        security.setSecureScreen(enabled)
        refreshSecurityState()
    }

    fun setLockTimeoutSeconds(seconds: Int) {
        security.setLockTimeoutSeconds(seconds)
        refreshSecurityState()
    }

    private fun refreshSecurityState() {
        _securityState.value = security.state()
    }

    fun resetDemoData() {
        repository.resetDemoData()
        refresh()
    }

    private fun refresh() {
        _customers.value = repository.customers()
        _entries.value = repository.entries()
        rebuildIndexes()
    }

    private fun rebuildIndexes() {
        val sortedGroups = _entries.value
            .groupBy { it.customerId }
            .mapValues { (_, list) -> list.sortedByDescending { it.createdAt } }

        entriesByCustomer = sortedGroups
        lastEntryByCustomer = sortedGroups.mapNotNull { (id, list) ->
            list.firstOrNull()?.let { id to it }
        }.toMap()
        lastPaymentByCustomer = sortedGroups.mapNotNull { (id, list) ->
            list.firstOrNull { it.type == EntryType.PAYMENT }?.let { id to it }
        }.toMap()

        balanceByCustomer = _customers.value.associate { customer ->
            val movement = sortedGroups[customer.id].orEmpty().sumOf { entry ->
                if (entry.type == EntryType.DEBT) entry.amount else -entry.amount
            }
            customer.id to (customer.openingDebt + movement).coerceAtLeast(0L)
        }
    }

    private fun Long?.orZero(): Long = this ?: 0L
}
