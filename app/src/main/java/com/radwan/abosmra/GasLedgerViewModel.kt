package com.radwan.abosmra

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.radwan.abosmra.data.AppRepository
import com.radwan.abosmra.data.Customer
import com.radwan.abosmra.data.EntryType
import com.radwan.abosmra.data.LedgerEntry
import com.radwan.abosmra.data.customerBalance
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

    fun customer(id: String): Customer? = _customers.value.firstOrNull { it.id == id }

    fun balance(customer: Customer): Long = customerBalance(customer, _entries.value)

    fun entriesFor(customerId: String): List<LedgerEntry> =
        _entries.value.filter { it.customerId == customerId }.sortedByDescending { it.createdAt }

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

    fun totalDebt(): Long = _customers.value.sumOf(::balance)

    fun indebtedCustomersCount(): Int = _customers.value.count { balance(it) > 0 }

    fun todayEntries(type: EntryType? = null): List<LedgerEntry> {
        val today = LocalDate.now()
        return _entries.value.filter { entry ->
            val date = Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            date == today && (type == null || entry.type == type)
        }.sortedByDescending { it.createdAt }
    }

    fun todayCollections(): Long = todayEntries(EntryType.PAYMENT).sumOf { it.amount }

    fun todayDebts(): Long = todayEntries(EntryType.DEBT).sumOf { it.amount }

    fun topDebtors(): List<Customer> =
        _customers.value.filter { balance(it) > 0 }.sortedByDescending(::balance)

    fun lastEntryFor(customerId: String): LedgerEntry? =
        _entries.value.filter { it.customerId == customerId }.maxByOrNull { it.createdAt }

    fun lastPaymentFor(customerId: String): LedgerEntry? =
        _entries.value.filter { it.customerId == customerId && it.type == EntryType.PAYMENT }
            .maxByOrNull { it.createdAt }

    fun exportJson(): String = repository.exportJson()

    fun resetDemoData() {
        repository.resetDemoData()
        refresh()
    }

    private fun refresh() {
        _customers.value = repository.customers()
        _entries.value = repository.entries()
    }
}
