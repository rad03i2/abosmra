package com.radwan.abosmra.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppRepository(context: Context) {
    private val prefs = context.getSharedPreferences("gas_ledger_data", Context.MODE_PRIVATE)

    private var customersCache: MutableList<Customer> = mutableListOf()
    private var entriesCache: MutableList<LedgerEntry> = mutableListOf()

    init {
        load()
        if (customersCache.isEmpty()) {
            seedDemoData()
        }
    }

    fun customers(): List<Customer> = customersCache.toList()

    fun entries(): List<LedgerEntry> = entriesCache.toList()

    fun addCustomer(
        name: String,
        phone: String?,
        area: String,
        address: String,
        openingDebt: Long,
        notes: String
    ): Customer {
        val customer = Customer(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phone = phone?.trim()?.takeIf { it.isNotBlank() },
            area = area.trim(),
            address = address.trim(),
            openingDebt = openingDebt.coerceAtLeast(0L),
            notes = notes.trim()
        )
        customersCache.add(0, customer)
        save()
        return customer
    }

    fun addDebt(
        customerId: String,
        amount: Long,
        bottles: Int?,
        bottlePrice: Long?,
        details: String = ""
    ): LedgerEntry {
        require(amount > 0)
        val entry = LedgerEntry(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            type = EntryType.DEBT,
            amount = amount,
            bottles = bottles,
            bottlePrice = bottlePrice,
            details = details
        )
        entriesCache.add(0, entry)
        save()
        return entry
    }

    fun addPayment(customerId: String, amount: Long): LedgerEntry {
        val customer = customersCache.first { it.id == customerId }
        val balance = customerBalance(customer, entriesCache)
        require(amount in 1..balance) { "Payment must be within current balance" }

        val entry = LedgerEntry(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            type = EntryType.PAYMENT,
            amount = amount
        )
        entriesCache.add(0, entry)
        save()
        return entry
    }

    fun resetDemoData() {
        customersCache.clear()
        entriesCache.clear()
        prefs.edit().clear().apply()
        seedDemoData()
    }

    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "دفتر الغاز")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("customers", customersToJson())
        root.put("entries", entriesToJson())
        return root.toString(2)
    }

    private fun save() {
        prefs.edit()
            .putString("customers", customersToJson().toString())
            .putString("entries", entriesToJson().toString())
            .apply()
    }

    private fun load() {
        customersCache = parseCustomers(prefs.getString("customers", null)).toMutableList()
        entriesCache = parseEntries(prefs.getString("entries", null)).toMutableList()
    }

    private fun customersToJson(): JSONArray = JSONArray().apply {
        customersCache.forEach { customer ->
            put(JSONObject().apply {
                put("id", customer.id)
                put("name", customer.name)
                put("phone", customer.phone ?: "")
                put("area", customer.area)
                put("address", customer.address)
                put("openingDebt", customer.openingDebt)
                put("notes", customer.notes)
                put("createdAt", customer.createdAt)
            })
        }
    }

    private fun entriesToJson(): JSONArray = JSONArray().apply {
        entriesCache.forEach { entry ->
            put(JSONObject().apply {
                put("id", entry.id)
                put("customerId", entry.customerId)
                put("type", entry.type.name)
                put("amount", entry.amount)
                put("bottles", entry.bottles ?: JSONObject.NULL)
                put("bottlePrice", entry.bottlePrice ?: JSONObject.NULL)
                put("details", entry.details)
                put("createdAt", entry.createdAt)
            })
        }
    }

    private fun parseCustomers(raw: String?): List<Customer> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        Customer(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            phone = item.optString("phone").takeIf { it.isNotBlank() },
                            area = item.optString("area"),
                            address = item.optString("address"),
                            openingDebt = item.optLong("openingDebt"),
                            notes = item.optString("notes"),
                            createdAt = item.optLong("createdAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun parseEntries(raw: String?): List<LedgerEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        LedgerEntry(
                            id = item.getString("id"),
                            customerId = item.getString("customerId"),
                            type = EntryType.valueOf(item.getString("type")),
                            amount = item.getLong("amount"),
                            bottles = if (item.isNull("bottles")) null else item.getInt("bottles"),
                            bottlePrice = if (item.isNull("bottlePrice")) null else item.getLong("bottlePrice"),
                            details = item.optString("details"),
                            createdAt = item.getLong("createdAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun seedDemoData() {
        val now = System.currentTimeMillis()
        val day = 24L * 60L * 60L * 1000L

        val demoCustomers = listOf(
            Customer("c1", "أحمد محمود", "07701234567", "حي النور", "قرب جامع النور", 25_000, createdAt = now - 90 * day),
            Customer("c2", "علي حسن", "07511223344", "حي الجامعة", "الشارع الرئيسي", 0, createdAt = now - 70 * day),
            Customer("c3", "محمد جاسم", null, "حي الزهور", "قرب المدرسة", 50_000, createdAt = now - 45 * day),
            Customer("c4", "مصطفى كريم", "07805556677", "حي النور", "الفرع الثاني", 0, createdAt = now - 30 * day),
            Customer("c5", "حيدر عبد الله", "07718889900", "حي الجامعة", "", 100_000, createdAt = now - 120 * day),
            Customer("c6", "عمر سالم", "07509998877", "حي السلام", "مقابل السوق", 0, createdAt = now - 20 * day)
        )
        customersCache.addAll(demoCustomers)

        entriesCache.addAll(
            listOf(
                LedgerEntry("e1", "c1", EntryType.DEBT, 50_000, 2, 25_000, "قناني غاز", now - 2 * 60 * 60 * 1000L),
                LedgerEntry("e2", "c2", EntryType.DEBT, 75_000, 3, 25_000, "قناني غاز", now - 3 * 60 * 60 * 1000L),
                LedgerEntry("e3", "c2", EntryType.PAYMENT, 25_000, createdAt = now - 90 * 60 * 1000L),
                LedgerEntry("e4", "c3", EntryType.DEBT, 25_000, 1, 25_000, "قنينة غاز", now - 5 * day),
                LedgerEntry("e5", "c4", EntryType.DEBT, 50_000, 2, 25_000, "قناني غاز", now - day),
                LedgerEntry("e6", "c4", EntryType.PAYMENT, 50_000, createdAt = now - 6 * 60 * 60 * 1000L),
                LedgerEntry("e7", "c5", EntryType.DEBT, 150_000, 6, 25_000, "قناني غاز", now - 35 * day),
                LedgerEntry("e8", "c5", EntryType.PAYMENT, 50_000, createdAt = now - 31 * day),
                LedgerEntry("e9", "c6", EntryType.DEBT, 25_000, 1, 25_000, "قنينة غاز", now - 4 * 60 * 60 * 1000L),
                LedgerEntry("e10", "c6", EntryType.PAYMENT, 10_000, createdAt = now - 30 * 60 * 1000L)
            )
        )
        save()
    }
}
