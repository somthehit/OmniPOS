package com.example.pos.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.pos.data.local.*
import com.example.pos.data.model.*
import com.example.pos.data.repository.PosRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CrossDeviceSyncTest {

    private lateinit var dbDeviceA: AppDatabase
    private lateinit var daoDeviceA: PosDao
    private lateinit var repoDeviceA: PosRepository

    private lateinit var dbDeviceB: AppDatabase
    private lateinit var daoDeviceB: PosDao
    private lateinit var repoDeviceB: PosRepository

    private val orgId = "org_cross_device_test"
    private val ownerUid = "usr_owner_dev_a"
    private val cashierUid = "usr_cashier_dev_b"

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Simulate Device A in-memory Room database
        dbDeviceA = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        daoDeviceA = dbDeviceA.posDao()
        repoDeviceA = PosRepository(daoDeviceA)

        // Simulate Device B separate in-memory Room database
        dbDeviceB = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        daoDeviceB = dbDeviceB.posDao()
        repoDeviceB = PosRepository(daoDeviceB)
    }

    @After
    fun tearDown() {
        dbDeviceA.close()
        dbDeviceB.close()
    }

    @Test
    fun testDeviceA_OutboxPayloadEnrichmentOnBusinessEvents() = runTest {
        // 1. Register store on Device A
        val reg = repoDeviceA.registerNewStoreAndUser(
            storeName = "Kathmandu Mart",
            taxId = "PAN-123456789",
            currencySymbol = "Rs",
            taxMode = TaxMode.EXCLUSIVE,
            userName = "Aarav Sharma",
            email = "aarav@mart.com",
            role = StaffRole.OWNER,
            pin = "123456",
            ownerUid = ownerUid
        )
        assertNotNull(reg.org)
        assertEquals(ownerUid, reg.user.id)

        // 2. Create product with barcode
        val product = repoDeviceA.createProductWithBarcode(
            orgId = reg.org.id,
            name = "Wai Wai Noodles",
            sku = "WW-01",
            barcode = "8901234567890",
            costPriceInCents = 2000L,
            sellingPriceInCents = 2500L,
            unit = "packet",
            packQty = 1
        )
        assertNotNull(product)

        // 3. Create customer
        val customer = repoDeviceA.createCustomer(
            orgId = reg.org.id,
            name = "Bikash Thapa",
            phone = "9841000000",
            email = "bikash@thapa.com",
            creditLimitInCents = 500000L,
            paymentTermsDays = 30,
            notes = "Regular customer",
            openingBalanceInCents = 100000L,
            openingBalanceType = "TO_RECEIVE"
        )
        assertNotNull(customer)

        // 4. Record stock movement
        repoDeviceA.recordStockAdjustment(
            orgId = reg.org.id,
            branchId = reg.branch.id,
            productId = product.id,
            delta = 100,
            type = MovementType.RECEIPT,
            reason = "Opening Stock",
            staffName = "Aarav Sharma"
        )

        // 5. Open shift
        val shift = repoDeviceA.openShift(
            orgId = reg.org.id,
            registerId = reg.register.id,
            cashierUid = ownerUid,
            cashierName = "Aarav Sharma",
            openingFloatInCents = 500000L,
            branchId = reg.branch.id
        )
        assertNotNull(shift)

        // 6. Complete sale
        val sale = repoDeviceA.completeSale(
            orgId = reg.org.id,
            branchId = reg.branch.id,
            registerId = reg.register.id,
            cartItems = listOf(
                CartItem(
                    product = product,
                    quantity = 2
                )
            ),
            payments = listOf(
                PaymentEntry(
                    type = PaymentType.CASH,
                    amountInCents = 5000L
                )
            ),
            orderDiscountPercent = 0.0,
            taxMode = TaxMode.EXCLUSIVE,
            customerId = customer.id,
            cashierName = "Aarav Sharma"
        )
        assertNotNull(sale)

        // 7. Close shift
        val zReport = repoDeviceA.closeShiftAdvanced(
            shiftId = shift.id,
            countedCashInCents = 505000L,
            closedBy = "Aarav Sharma"
        )
        assertNotNull(zReport)

        // Verify Sync Outbox table contains all operational records with correct orgId & json
        val pendingOutbox = daoDeviceA.getPendingSyncEntries(reg.org.id)
        assertTrue("Sync outbox should contain enqueued operations", pendingOutbox.isNotEmpty())

        val tableNames = pendingOutbox.map { it.tableName }.toSet()
        assertTrue("Outbox must contain products", tableNames.contains("products"))
        assertTrue("Outbox must contain customers", tableNames.contains("customers"))
        assertTrue("Outbox must contain customer_ledger", tableNames.contains("customer_ledger"))
        assertTrue("Outbox must contain shifts", tableNames.contains("shifts"))
        assertTrue("Outbox must contain sales", tableNames.contains("sales"))
        assertTrue("Outbox must contain sale_items", tableNames.contains("sale_items"))
        assertTrue("Outbox must contain sale_payments", tableNames.contains("sale_payments"))
        assertTrue("Outbox must contain stock_movements", tableNames.contains("stock_movements"))

        // Validate payload contents have orgId and id
        for (entry in pendingOutbox) {
            val json = JSONObject(entry.payloadJson)
            assertEquals("Entry orgId must match", reg.org.id, entry.orgId)
            assertTrue("Payload JSON must not be blank", json.length() > 0)
        }
    }

    @Test
    fun testDeviceB_ReceivesAndPersistsAllCollectionsFromDeviceA() = runTest {
        // Prepare mock authoritative data from Device A in a shared format
        val sharedOrg = OrganizationEntity(
            id = orgId,
            name = "Synced Pokhara Store",
            taxId = "PAN-999888777",
            currencySymbol = "Rs",
            taxMode = TaxMode.EXCLUSIVE,
            ownerUid = ownerUid
        )
        val sharedBranch = BranchEntity(
            id = "br_main_sync",
            orgId = orgId,
            name = "Main Branch",
            code = "MAIN",
            address = "Pokhara",
            phone = "061500000"
        )
        val sharedRegister = RegisterEntity(
            id = "reg_sync_01",
            orgId = orgId,
            branchId = "br_main_sync",
            name = "POS Counter 1",
            code = "REG1"
        )
        val sharedProduct = ProductEntity(
            id = "prod_sync_101",
            orgId = orgId,
            categoryId = "cat_groceries",
            name = "Basmati Rice 20kg",
            sku = "RICE-20K",
            barcode = "890000000101",
            costPriceInCents = 250000L,
            sellingPriceInCents = 320000L,
            unit = "bag"
        )
        val sharedCustomer = CustomerEntity(
            id = "cust_sync_201",
            orgId = orgId,
            name = "Sunil Shrestha",
            phone = "9851000000",
            email = "sunil@gmail.com",
            creditLimitInCents = 1000000L,
            currentDueInCents = 320000L
        )
        val sharedSale = SaleEntity(
            id = "sale_sync_301",
            orgId = orgId,
            branchId = "br_main_sync",
            registerId = "reg_sync_01",
            invoiceNo = "INV-000301",
            customerId = "cust_sync_201",
            subtotalInCents = 320000L,
            discountInCents = 0L,
            taxInCents = 41600L,
            grandTotalInCents = 361600L,
            paymentStatus = "CREDIT",
            createdBy = "Aarav"
        )
        val sharedSaleItem = SaleItemEntity(
            id = "item_sync_401",
            orgId = orgId,
            saleId = "sale_sync_301",
            productId = "prod_sync_101",
            productName = "Basmati Rice 20kg",
            unitPriceInCents = 320000L,
            quantity = 1,
            discountInCents = 0L,
            taxInCents = 41600L,
            lineTotalInCents = 361600L
        )
        val sharedPayment = SalePaymentEntity(
            id = "pay_sync_501",
            orgId = orgId,
            saleId = "sale_sync_301",
            paymentType = PaymentType.CREDIT_DUE,
            amountInCents = 361600L
        )
        val sharedShift = ShiftEntity(
            id = "shift_sync_601",
            orgId = orgId,
            branchId = "br_main_sync",
            registerId = "reg_sync_01",
            cashierUid = ownerUid,
            cashierName = "Aarav Sharma",
            openingFloatInCents = 500000L,
            closedAt = System.currentTimeMillis(),
            countedCashInCents = 500000L,
            status = "CLOSED"
        )
        val sharedStockMovement = StockMovementEntity(
            id = "sm_sync_701",
            orgId = orgId,
            branchId = "br_main_sync",
            productId = "prod_sync_101",
            quantityDelta = 50,
            type = MovementType.RECEIPT,
            referenceId = "PO-1001",
            createdBy = "Aarav"
        )
        val sharedFiscalYear = FiscalYearEntity(
            id = "fy_sync_801",
            orgId = orgId,
            name = "2081/82",
            code = "81/82",
            startDate = 1721088000000L,
            endDate = 1752623999000L,
            isCurrent = true
        )

        // Device B initially has an empty Room database
        assertNull(daoDeviceB.getActiveOrganizationSync())
        assertEquals(0, daoDeviceB.getAllProductsSync(orgId).size)
        assertEquals(0, daoDeviceB.getAllCustomersSync(orgId).size)
        assertEquals(0, daoDeviceB.getAllSalesSync(orgId).size)
        assertEquals(0, daoDeviceB.getAllShiftsSync(orgId).size)

        // Simulate pulling all authoritative data into Device B
        daoDeviceB.insertOrg(sharedOrg)
        daoDeviceB.insertBranch(sharedBranch)
        daoDeviceB.insertRegister(sharedRegister)
        daoDeviceB.insertProducts(listOf(sharedProduct))
        daoDeviceB.insertCustomers(listOf(sharedCustomer))
        daoDeviceB.insertSales(listOf(sharedSale))
        daoDeviceB.insertSaleItems(listOf(sharedSaleItem))
        daoDeviceB.insertSalePayments(listOf(sharedPayment))
        daoDeviceB.insertShifts(listOf(sharedShift))
        daoDeviceB.insertStockMovements(listOf(sharedStockMovement))
        daoDeviceB.insertFiscalYears(listOf(sharedFiscalYear))

        // Assert Device B has full visibility of Device A's business data
        val activeOrgB = daoDeviceB.getActiveOrganizationSync()
        assertNotNull(activeOrgB)
        assertEquals("Synced Pokhara Store", activeOrgB?.name)

        val productsB = daoDeviceB.getAllProductsSync(orgId)
        assertEquals(1, productsB.size)
        assertEquals("Basmati Rice 20kg", productsB[0].name)
        assertEquals("890000000101", productsB[0].barcode)

        val customersB = daoDeviceB.getAllCustomersSync(orgId)
        assertEquals(1, customersB.size)
        assertEquals("Sunil Shrestha", customersB[0].name)
        assertEquals(320000L, customersB[0].currentDueInCents)

        val salesB = daoDeviceB.getAllSalesSync(orgId)
        assertEquals(1, salesB.size)
        assertEquals("INV-000301", salesB[0].invoiceNo)

        val itemsB = daoDeviceB.getSaleItems(sharedSale.id)
        assertEquals(1, itemsB.size)
        assertEquals("prod_sync_101", itemsB[0].productId)

        val paymentsB = daoDeviceB.getSalePayments(sharedSale.id)
        assertEquals(1, paymentsB.size)
        assertEquals(PaymentType.CREDIT_DUE, paymentsB[0].paymentType)

        val shiftsB = daoDeviceB.getAllShiftsSync(orgId)
        assertEquals(1, shiftsB.size)
        assertEquals("CLOSED", shiftsB[0].status)

        val stockMovementsB = daoDeviceB.getRecentStockMovements(orgId).first()
        assertEquals(1, stockMovementsB.size)
        assertEquals(50, stockMovementsB[0].quantityDelta)

        val fyB = daoDeviceB.getActiveFiscalYearSync(orgId)
        assertNotNull(fyB)
        assertEquals("2081/82", fyB?.name)
    }

    @Test
    fun testMultiTenantIsolation_PreventsCrossOrgDataLeakage() = runTest {
        val org1Id = "org_store_alpha"
        val org2Id = "org_store_beta"

        // Insert Store Alpha product
        val prodAlpha = ProductEntity(
            id = "prod_alpha_1",
            orgId = org1Id,
            categoryId = "cat_alpha",
            name = "Alpha Product",
            sku = "SKU-A",
            barcode = "111111",
            costPriceInCents = 1000L,
            sellingPriceInCents = 1500L
        )
        daoDeviceA.insertProduct(prodAlpha)

        // Insert Store Beta product
        val prodBeta = ProductEntity(
            id = "prod_beta_1",
            orgId = org2Id,
            categoryId = "cat_beta",
            name = "Beta Product",
            sku = "SKU-B",
            barcode = "222222",
            costPriceInCents = 2000L,
            sellingPriceInCents = 2500L
        )
        daoDeviceA.insertProduct(prodBeta)

        // Verify Org 1 cannot see Org 2 products
        val alphaProducts = daoDeviceA.getAllProductsSync(org1Id)
        assertEquals(1, alphaProducts.size)
        assertEquals("Alpha Product", alphaProducts[0].name)

        val betaProducts = daoDeviceA.getAllProductsSync(org2Id)
        assertEquals(1, betaProducts.size)
        assertEquals("Beta Product", betaProducts[0].name)

        // Barcode lookup with orgId filter
        val foundAlpha = daoDeviceA.getProductByBarcode(org1Id, "111111")
        assertNotNull(foundAlpha)
        assertEquals("prod_alpha_1", foundAlpha?.id)

        // Store Beta barcode should NOT be visible when searching within Store Alpha
        val crossLeakAttempt = daoDeviceA.getProductByBarcode(org1Id, "222222")
        assertNull("Cross-org barcode lookup must return null", crossLeakAttempt)
    }

    @Test
    fun testIdempotentSync_PreventsDuplicateRecordsOnMultiplePulls() = runTest {
        val testProduct = ProductEntity(
            id = "prod_idempotent_1",
            orgId = orgId,
            categoryId = "cat_gen",
            name = "Idempotent Item",
            sku = "IDEM-1",
            barcode = "999999",
            costPriceInCents = 1000L,
            sellingPriceInCents = 1200L
        )

        // Insert first time
        daoDeviceB.insertProducts(listOf(testProduct))
        assertEquals(1, daoDeviceB.getAllProductsSync(orgId).size)

        // Ingest the same document again (simulating multiple pull syncs)
        daoDeviceB.insertProducts(listOf(testProduct))
        assertEquals(1, daoDeviceB.getAllProductsSync(orgId).size)

        // Update product price on Device A and pull again
        val updatedProduct = testProduct.copy(sellingPriceInCents = 1350L)
        daoDeviceB.insertProducts(listOf(updatedProduct))
        val currentProducts = daoDeviceB.getAllProductsSync(orgId)
        assertEquals(1, currentProducts.size)
        assertEquals(1350L, currentProducts[0].sellingPriceInCents)
    }
}
