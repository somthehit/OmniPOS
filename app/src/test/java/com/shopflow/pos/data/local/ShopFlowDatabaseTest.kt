package com.shopflow.pos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shopflow.pos.data.local.dao.StockOnHand
import com.shopflow.pos.data.local.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShopFlowDatabaseTest {

    private lateinit var db: ShopFlowDatabase
    private val orgId = "org_test_123"
    private val branchId = "br_test_01"
    private val regId = "reg_test_01"
    private val userId = "usr_cashier_01"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShopFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        runBlocking {
            // Seed base organization, branch, register, user, membership
            val now = System.currentTimeMillis()
            db.organizationDao().upsert(
                OrganizationEntity(
                    id = orgId,
                    legalName = "Test Retail Inc.",
                    displayName = "Test Mart",
                    businessType = "RETAIL",
                    countryCode = "US",
                    currencyCode = "USD",
                    timezoneId = "UTC",
                    createdAt = now,
                    updatedAt = now,
                    deviceId = "dev_01"
                )
            )

            db.branchDao().upsertAll(
                listOf(
                    BranchEntity(
                        id = branchId,
                        organizationId = orgId,
                        code = "BR1",
                        name = "Main Branch",
                        createdAt = now,
                        updatedAt = now,
                        deviceId = "dev_01"
                    )
                )
            )

            db.registerDao().upsertAll(
                listOf(
                    RegisterEntity(
                        id = regId,
                        organizationId = orgId,
                        branchId = branchId,
                        name = "Register 1",
                        receiptPrefix = "BR1-REG1",
                        createdAt = now,
                        updatedAt = now,
                        deviceId = "dev_01"
                    )
                )
            )

            db.userDao().upsertUsers(
                listOf(
                    UserEntity(
                        id = userId,
                        organizationId = orgId,
                        fullName = "Jane Doe",
                        email = "jane@test.org",
                        createdAt = now,
                        updatedAt = now,
                        deviceId = "dev_01"
                    )
                )
            )

            db.userDao().upsertMemberships(
                listOf(
                    MembershipEntity(
                        id = "mem_01",
                        organizationId = orgId,
                        userId = userId,
                        roleKey = "CASHIER",
                        createdAt = now,
                        updatedAt = now,
                        deviceId = "dev_01"
                    )
                )
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCompleteSaleTransactionAndLedgerStock() = runBlocking {
        val now = System.currentTimeMillis()
        val prodId = "prod_coke_01"

        // 1. Add product & initial stock movement
        db.productDao().upsertAll(
            listOf(
                ProductEntity(
                    id = prodId,
                    organizationId = orgId,
                    name = "Coca Cola 330ml",
                    sku = "BEV-COKE-330",
                    priceMinor = 199L,
                    trackStock = true,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = "dev_01"
                )
            )
        )

        // Opening stock: 100 units (100,000 milli-units)
        db.stockDao().insertMovements(
            listOf(
                StockMovementEntity(
                    id = UUID.randomUUID().toString(),
                    organizationId = orgId,
                    branchId = branchId,
                    productId = prodId,
                    movementType = MovementType.OPENING,
                    quantityMilli = 100_000L,
                    sourceType = "opening",
                    sourceId = "init",
                    idempotencyKey = "opening:$prodId",
                    createdAt = now,
                    deviceId = "dev_01",
                    syncState = SyncState.SYNCED
                )
            )
        )

        val initialStock = db.stockDao().onHand(orgId, branchId, prodId)
        assertEquals(100_000L, initialStock.provisionalMilli)

        // 2. Execute CompleteSale via SaleRepository
        val repo = SaleRepository(
            db = db,
            activeOrganizationId = orgId,
            idGen = { UUID.randomUUID().toString() },
            nowMs = { now }
        )

        val saleId = UUID.randomUUID().toString()
        val idempotencyKey = "sale_idem_999"

        val sale = SaleEntity(
            id = saleId,
            organizationId = orgId,
            branchId = branchId,
            registerId = regId,
            cashierId = userId,
            receiptNumber = "", // allocated by repository
            idempotencyKey = idempotencyKey,
            currencyCode = "USD",
            taxMode = "EXCLUSIVE",
            subtotalMinor = 398L,
            totalMinor = 398L,
            paidMinor = 398L,
            soldAt = now,
            timezoneId = "UTC",
            createdAt = now,
            updatedAt = now,
            deviceId = "dev_01"
        )

        val items = listOf(
            SaleItemEntity(
                id = UUID.randomUUID().toString(),
                organizationId = orgId,
                saleId = saleId,
                productId = prodId,
                productName = "Coca Cola 330ml",
                sku = "BEV-COKE-330",
                quantityMilli = 2_000L, // 2 units
                unitPriceMinor = 199L,
                lineTotalMinor = 398L,
                createdAt = now,
                deviceId = "dev_01"
            )
        )

        val payments = listOf(
            PaymentEntity(
                id = UUID.randomUUID().toString(),
                organizationId = orgId,
                saleId = saleId,
                method = PaymentMethod.CASH,
                amountMinor = 398L,
                createdAt = now,
                deviceId = "dev_01"
            )
        )

        val cmd = CompleteSaleCommand(
            sale = sale,
            items = items,
            payments = payments,
            payloadJson = "{}"
        )

        val result = repo.completeSale(cmd)
        assertTrue(result is CompleteSaleResult.Recorded)
        val recorded = result as CompleteSaleResult.Recorded
        assertEquals(saleId, recorded.saleId)
        assertEquals("BR1-REG1-000001", recorded.receiptNumber)

        // 3. Verify stock decreased by 2,000 milli-units (from 100,000 to 98,000)
        val postSaleStock = db.stockDao().onHand(orgId, branchId, prodId)
        assertEquals(98_000L, postSaleStock.provisionalMilli)

        // 4. Verify outbox enqueued
        val outbox = db.outboxDao().nextBatch(orgId, now + 1000L)
        assertEquals(1, outbox.size)
        assertEquals("sale", outbox[0].entityType)
        assertEquals(saleId, outbox[0].entityId)

        // 5. Verify Idempotency: re-executing same sale returns AlreadyRecorded without duplicate
        val retryResult = repo.completeSale(cmd)
        assertTrue(retryResult is CompleteSaleResult.AlreadyRecorded)
        assertEquals(recorded.receiptNumber, (retryResult as CompleteSaleResult.AlreadyRecorded).receiptNumber)

        // Stock remains 98,000
        val finalStock = db.stockDao().onHand(orgId, branchId, prodId)
        assertEquals(98_000L, finalStock.provisionalMilli)
    }

    @Test
    fun testCustomerDaoCrudAndOrgScoping() = runBlocking {
        val now = System.currentTimeMillis()
        val custId = "cust_vip_001"

        // Create
        db.customerDao().upsert(
            CustomerEntity(
                id = custId,
                organizationId = orgId,
                name = "Alice Wonderland",
                phone = "+1 555-4321",
                email = "alice@example.com",
                createdAt = now,
                updatedAt = now,
                deviceId = "dev_01"
            )
        )

        // Read
        val customer = db.customerDao().get(orgId, custId)
        assertNotNull(customer)
        assertEquals("Alice Wonderland", customer?.name)

        // Update
        db.customerDao().upsert(customer!!.copy(name = "Alice Kingsley", updatedAt = now + 100))
        val updated = db.customerDao().get(orgId, custId)
        assertEquals("Alice Kingsley", updated?.name)

        // Delete (soft-delete)
        db.customerDao().softDelete(orgId, custId, now + 200)
        val deleted = db.customerDao().get(orgId, custId)
        assertEquals(null, deleted) // Deleted items are filtered out

        // Multi-tenant check: Different orgId returns null
        val crossTenantCust = db.customerDao().get("org_attacker_999", custId)
        assertEquals(null, crossTenantCust)
    }

    @Test
    fun testCategoryAndTaxRateDao() = runBlocking {
        val now = System.currentTimeMillis()
        val catId = "cat_apparel_01"
        val taxId = "tax_vat_13"

        // Category CRUD
        db.categoryDao().upsert(
            CategoryEntity(
                id = catId,
                organizationId = orgId,
                name = "Summer Apparel",
                sortOrder = 1,
                createdAt = now,
                updatedAt = now,
                deviceId = "dev_01"
            )
        )
        val cat = db.categoryDao().get(orgId, catId)
        assertNotNull(cat)
        assertEquals("Summer Apparel", cat?.name)

        // TaxRate CRUD
        db.taxRateDao().upsert(
            TaxRateEntity(
                id = taxId,
                organizationId = orgId,
                name = "Standard VAT",
                rateBps = 1300,
                isDefault = true,
                createdAt = now,
                updatedAt = now,
                deviceId = "dev_01"
            )
        )
        val defaultTax = db.taxRateDao().getDefault(orgId)
        assertNotNull(defaultTax)
        assertEquals(1300, defaultTax?.rateBps)

        // Cross-tenant verification:
        val crossTenantTax = db.taxRateDao().getDefault("org_attacker_999")
        assertEquals(null, crossTenantTax)
    }
}

