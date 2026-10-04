package com.radwan.abosmra

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.radwan.abosmra.data.AppRepository
import com.radwan.abosmra.data.Customer
import com.radwan.abosmra.data.EntryType
import com.radwan.abosmra.data.LedgerEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GasLedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

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
