package com.radwan.abosmra.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GasLedgerDaoTest {
    private lateinit var db: GasLedgerDatabase
    private lateinit var dao: GasLedgerDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<GasLedgerDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
        dao = db.dao()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    @Test
    fun roomFlowAndTargetedQueries_followLedgerChanges() = runTest {
        val customer = CustomerEntity(
            id = "c1",
            name = "أحمد",
            phone = "07700000000",
            area = "حي النور",
            address = "",
            openingDebt = 2_000L,
            notes = "",
            createdAt = 1_000L
        )
        dao.insertCustomer(customer)

        dao.insertEntry(
            LedgerEntryEntity(
                id = "d1",
                customerId = customer.id,
                type = EntryType.DEBT.name,
                amount = 5_000L,
                bottles = 2,
                bottlePrice = 2_500L,
                details = "غاز",
                createdAt = 2_000L
            )
        )
        dao.insertEntry(
            LedgerEntryEntity(
                id = "p1",
                customerId = customer.id,
                type = EntryType.PAYMENT.name,
                amount = 3_000L,
                bottles = null,
                bottlePrice = null,
                details = "",
                createdAt = 3_000L
            )
        )

        assertEquals(customer, dao.getCustomerById(customer.id))
        assertEquals(2, dao.getEntriesForCustomer(customer.id).size)
        assertEquals(1, dao.observeCustomers().first().size)
        assertEquals(2, dao.observeEntries().first().size)

        val summary = dao.currentDebtSummary()
        assertEquals(4_000L, summary.totalDebt)
        assertEquals(1, summary.openAccounts)
        assertEquals(1, summary.totalCustomers)
    }

    @Test
    fun replaceAll_behavesLikeBackupRestoreAndRemovesOldRows() = runTest {
        val oldCustomer = CustomerEntity(
            id = "old",
            name = "قديم",
            phone = null,
            area = "",
            address = "",
            openingDebt = 0L,
            notes = "",
            createdAt = 1_000L
        )
        dao.insertCustomer(oldCustomer)

        val restoredCustomer = CustomerEntity(
            id = "new",
            name = "مستعاد",
            phone = null,
            area = "الجامعة",
            address = "",
            openingDebt = 1_500L,
            notes = "",
            createdAt = 2_000L
        )
        val restoredEntry = LedgerEntryEntity(
            id = "new-debt",
            customerId = restoredCustomer.id,
            type = EntryType.DEBT.name,
            amount = 2_500L,
            bottles = null,
            bottlePrice = null,
            details = "",
            createdAt = 3_000L
        )

        dao.replaceAll(
            customers = listOf(restoredCustomer),
            entries = listOf(restoredEntry)
        )

        assertNull(dao.getCustomerById(oldCustomer.id))
        assertEquals(restoredCustomer, dao.getCustomerById(restoredCustomer.id))
        assertEquals(listOf(restoredEntry), dao.getEntriesForCustomer(restoredCustomer.id))
        assertEquals(1, dao.customerCount())
        assertEquals(1, dao.entryCount())
    }

    @Test
    fun reportQueries_aggregateDebtsCollectionsBottlesAndTopDebtors() = runTest {
        val c1 = CustomerEntity(
            id = "c1",
            name = "أحمد",
            phone = null,
            area = "حي النور",
            address = "",
            openingDebt = 1_000L,
            notes = "",
            createdAt = 1_000L
        )
        val c2 = CustomerEntity(
            id = "c2",
            name = "علي",
            phone = null,
            area = "حي النور",
            address = "",
            openingDebt = 0L,
            notes = "",
            createdAt = 1_000L
        )
        dao.insertCustomers(listOf(c1, c2))
        dao.insertEntries(
            listOf(
                LedgerEntryEntity("d1", c1.id, EntryType.DEBT.name, 5_000L, 2, 2_500L, "", 2_000L),
                LedgerEntryEntity("p1", c1.id, EntryType.PAYMENT.name, 2_000L, null, null, "", 3_000L),
                LedgerEntryEntity("d2", c2.id, EntryType.DEBT.name, 7_000L, 1, 7_000L, "", 4_000L)
            )
        )

        val metrics = dao.reportPeriodMetrics(0L, 10_000L)
        assertEquals(12_000L, metrics.debts)
        assertEquals(2_000L, metrics.collections)
        assertEquals(3L, metrics.bottles)

        val top = dao.topCustomersByDebt(2)
        assertEquals("c2", top.first().customerId)
        assertEquals(7_000L, top.first().balance)

        val areas = dao.topAreasByDebt(5)
        assertEquals(1, areas.size)
        assertEquals("حي النور", areas.single().area)
        assertEquals(11_000L, areas.single().balance)
    }
}
