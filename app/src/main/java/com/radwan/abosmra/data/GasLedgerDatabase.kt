package com.radwan.abosmra.data

import android.content.Context
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.sqlite.driver.AndroidSQLiteDriver

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["area"]),
        Index(value = ["created_at"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String?,
    val area: String,
    val address: String,
    @ColumnInfo(name = "opening_debt")
    val openingDebt: Long,
    val notes: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["created_at"]),
        Index(value = ["type"])
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "customer_id")
    val customerId: String,
    val type: String,
    val amount: Long,
    val bottles: Int?,
    @ColumnInfo(name = "bottle_price")
    val bottlePrice: Long?,
    val details: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Dao
interface GasLedgerDao {
    @Query("SELECT * FROM customers ORDER BY created_at DESC")
    suspend fun getCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM ledger_entries ORDER BY created_at DESC")
    suspend fun getEntries(): List<LedgerEntryEntity>

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun customerCount(): Int

    @Query("SELECT COUNT(*) FROM ledger_entries")
    suspend fun entryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LedgerEntryEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateEntry(entry: LedgerEntryEntity)

    @Query("DELETE FROM ledger_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: String)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomerById(customerId: String)

    @Query("DELETE FROM ledger_entries")
    suspend fun clearEntries()

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Transaction
    suspend fun replaceAll(
        customers: List<CustomerEntity>,
        entries: List<LedgerEntryEntity>
    ) {
        clearEntries()
        clearCustomers()
        if (customers.isNotEmpty()) insertCustomers(customers)
        if (entries.isNotEmpty()) insertEntries(entries)
    }
}

@Database(
    entities = [CustomerEntity::class, LedgerEntryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class GasLedgerDatabase : RoomDatabase() {
    abstract fun dao(): GasLedgerDao

    companion object {
        @Volatile
        private var instance: GasLedgerDatabase? = null

        fun get(context: Context): GasLedgerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder<GasLedgerDatabase>(
                    context.applicationContext,
                    "gas_ledger.db"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                    .also { instance = it }
            }
    }
}

fun Customer.toEntity(): CustomerEntity = CustomerEntity(
    id = id,
    name = name,
    phone = phone,
    area = area,
    address = address,
    openingDebt = openingDebt,
    notes = notes,
    createdAt = createdAt
)

fun CustomerEntity.toModel(): Customer = Customer(
    id = id,
    name = name,
    phone = phone,
    area = area,
    address = address,
    openingDebt = openingDebt,
    notes = notes,
    createdAt = createdAt
)

fun LedgerEntry.toEntity(): LedgerEntryEntity = LedgerEntryEntity(
    id = id,
    customerId = customerId,
    type = type.name,
    amount = amount,
    bottles = bottles,
    bottlePrice = bottlePrice,
    details = details,
    createdAt = createdAt
)

fun LedgerEntryEntity.toModel(): LedgerEntry = LedgerEntry(
    id = id,
    customerId = customerId,
    type = EntryType.valueOf(type),
    amount = amount,
    bottles = bottles,
    bottlePrice = bottlePrice,
    details = details,
    createdAt = createdAt
)
