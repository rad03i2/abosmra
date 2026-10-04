package com.radwan.abosmra.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("gas_ledger_data", Context.MODE_PRIVATE)
    private val dao = GasLedgerDatabase.get(appContext).dao()

    private var customersCache: MutableList<Customer> = mutableListOf()
    private var entriesCache: MutableList<LedgerEntry> = mutableListOf()

    init {
        loadRoomOrMigrateLegacy()
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

        dbCall { dao.insertCustomer(customer.toEntity()) }
        customersCache.add(0, customer)
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
        require(customersCache.any { it.id == customerId }) { "Customer not found" }

        val entry = LedgerEntry(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            type = EntryType.DEBT,
            amount = amount,
            bottles = bottles,
            bottlePrice = bottlePrice,
            details = details
        )

        dbCall { dao.insertEntry(entry.toEntity()) }
        entriesCache.add(0, entry)
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

        dbCall { dao.insertEntry(entry.toEntity()) }
        entriesCache.add(0, entry)
        return entry
    }

    fun resetDemoData() {
        val (customers, entries) = demoData()
        dbCall {
            dao.replaceAll(
                customers = customers.map(Customer::toEntity),
                entries = entries.map(LedgerEntry::toEntity)
            )
        }

        customersCache = customers.toMutableList()
        entriesCache = entries.toMutableList()

        prefs.edit()
            .putBoolean(ROOM_INITIALIZED_KEY, true)
            .apply()
    }

    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "دفتر الغاز")
        root.put("version", 2)
        root.put("storage", "room-sqlite")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("customers", customersToJson())
        root.put("entries", entriesToJson())
        return root.toString(2)
    }

    private fun loadRoomOrMigrateLegacy() {
        dbCall {
            val dbCustomers = dao.getCustomers().map(CustomerEntity::toModel)
            val dbEntries = dao.getEntries().map(LedgerEntryEntity::toModel)
            val alreadyInitialized = prefs.getBoolean(ROOM_INITIALIZED_KEY, false)

            if (dbCustomers.isNotEmpty() || dbEntries.isNotEmpty()) {
                customersCache = dbCustomers.toMutableList()
                entriesCache = dbEntries.toMutableList()

                if (!alreadyInitialized) {
                    prefs.edit()
                        .putBoolean(ROOM_INITIALIZED_KEY, true)
                        .commit()
                }
                return@dbCall
            }

            if (alreadyInitialized) {
                customersCache = mutableListOf()
                entriesCache = mutableListOf()
                return@dbCall
            }

            val legacyCustomers = parseCustomers(prefs.getString("customers", null))
            val knownCustomerIds = legacyCustomers.mapTo(hashSetOf()) { it.id }
            val legacyEntries = parseEntries(prefs.getString("entries", null))
                .filter { it.customerId in knownCustomerIds }

            val source = if (legacyCustomers.isNotEmpty()) {
                legacyCustomers to legacyEntries
            } else {
                demoData()
            }

            dao.replaceAll(
                customers = source.first.map(Customer::toEntity),
                entries = source.second.map(LedgerEntry::toEntity)
            )

            customersCache = source.first.toMutableList()
            entriesCache = source.second.toMutableList()

            // Keep the legacy JSON untouched as a recovery copy. The marker prevents
            // importing it again after Room becomes the authoritative data source.
            prefs.edit()
                .putBoolean(ROOM_INITIALIZED_KEY, true)
                .commit()
        }
    }

    private fun <T> dbCall(block: suspend () -> T): T =
        runBlocking(Dispatchers.IO) { block() }

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

    private fun demoData(): Pair<List<Customer>, List<LedgerEntry>> {
        val now = System.currentTimeMillis()
        val day = 24L * 60L * 60L * 1000L

        val customers = listOf(
            Customer("c1", "أحمد محمود", "07701234567", "حي النور", "قرب جامع النور", 25_000, createdAt = now - 90 * day),
            Customer("c2", "علي حسن", "07511223344", "حي الجامعة", "الشارع الرئيسي", 0, createdAt = now - 70 * day),
            Customer("c3", "محمد جاسم", null, "حي الزهور", "قرب المدرسة", 50_000, createdAt = now - 45 * day),
            Customer("c4", "مصطفى كريم", "07805556677", "حي النور", "الفرع الثاني", 0, createdAt = now - 30 * day),
            Customer("c5", "حيدر عبد الله", "07718889900", "حي الجامعة", "", 100_000, createdAt = now - 120 * day),
            Customer("c6", "عمر سالم", "07509998877", "حي السلام", "مقابل السوق", 0, createdAt = now - 20 * day)
        )

        val entries = listOf(
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

        return customers to entries
    }

    companion object {
        private const val ROOM_INITIALIZED_KEY = "room_initialized_v1"
    }
}
