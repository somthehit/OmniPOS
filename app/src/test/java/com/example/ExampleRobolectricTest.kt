package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.pos.data.local.AppDatabase
import com.example.pos.data.local.Converters
import com.example.pos.data.local.Organization
import com.example.pos.data.local.OrganizationEntity
import com.example.pos.data.local.Product
import com.example.pos.data.local.ProductEntity
import com.example.pos.data.local.Sale
import com.example.pos.data.local.SaleEntity
import com.example.pos.data.local.User
import com.example.pos.data.local.UserEntity
import com.example.pos.data.model.StaffRole
import com.example.pos.data.model.TaxMode
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OmniPOS", appName)
    }

    @Test
    fun `cart calculator math verification`() {
        val change = com.example.pos.domain.CartCalculator.calculateChange(5000L, 3450L)
        assertEquals(1550L, change)
        val formatted = com.example.pos.domain.CartCalculator.formatMoney(3450L, "$")
        assertEquals("$34.50", formatted)

        // Nepali & South Asian number grouping (1,00,000)
        val formattedNprLakh = com.example.pos.domain.CartCalculator.formatMoney(10000000L, "Rs")
        assertEquals("Rs 1,00,000.00", formattedNprLakh)

        val formattedNprCrore = com.example.pos.domain.CartCalculator.formatMoney(1000000000L, "रु")
        assertEquals("रु 1,00,00,000.00", formattedNprCrore)
    }

    @Test
    fun `organization and multi-tenant user dao isolation`() = runBlocking {
        val orgDao = db.organizationDao()
        val userDao = db.userDao()

        val orgA = OrganizationEntity(
            id = "org_a",
            orgId = "org_a",
            name = "Store Alpha",
            taxId = "TAX-A",
            currencySymbol = "$",
            taxMode = TaxMode.EXCLUSIVE,
            ownerUid = "owner_a"
        )
        val orgB = OrganizationEntity(
            id = "org_b",
            orgId = "org_b",
            name = "Store Beta",
            taxId = "TAX-B",
            currencySymbol = "€",
            taxMode = TaxMode.INCLUSIVE,
            ownerUid = "owner_b"
        )
        orgDao.insertOrg(orgA)
        orgDao.insertOrg(orgB)

        val userA = UserEntity(
            id = "usr_1",
            orgId = "org_a",
            fullName = "Alice Smith",
            email = "alice@alpha.com",
            role = StaffRole.ADMIN
        )
        val userB = UserEntity(
            id = "usr_2",
            orgId = "org_b",
            fullName = "Bob Jones",
            email = "bob@beta.com",
            role = StaffRole.CASHIER
        )
        userDao.insertUser(userA)
        userDao.insertUser(userB)

        // Strict multi-tenancy verification: org_a cannot see userB
        val foundUserInOrgA = userDao.getUserById("org_a", "usr_1")
        assertNotNull(foundUserInOrgA)
        assertEquals("Alice Smith", foundUserInOrgA?.fullName)

        val crossOrgUserQuery = userDao.getUserById("org_a", "usr_2")
        assertNull(crossOrgUserQuery)
    }

    @Test
    fun `product and sale dao scoped by org_id`() = runBlocking {
        val productDao = db.productDao()
        val saleDao = db.saleDao()

        val prodA = ProductEntity(
            id = "prod_1",
            orgId = "org_a",
            categoryId = "cat_1",
            name = "Espresso Roast",
            sku = "COF-01",
            barcode = "111222333",
            costPriceInCents = 150L,
            sellingPriceInCents = 350L,
            taxRatePercent = 5.0
        )
        val prodB = ProductEntity(
            id = "prod_2",
            orgId = "org_b",
            categoryId = "cat_2",
            name = "Green Tea",
            sku = "TEA-01",
            barcode = "444555666",
            costPriceInCents = 100L,
            sellingPriceInCents = 250L,
            taxRatePercent = 0.0
        )
        productDao.insertProduct(prodA)
        productDao.insertProduct(prodB)

        // Check product scoped
        val prodInA = productDao.getProductById("org_a", "prod_1")
        assertNotNull(prodInA)
        assertEquals("Espresso Roast", prodInA?.name)

        val crossOrgProd = productDao.getProductById("org_a", "prod_2")
        assertNull(crossOrgProd)

        // Insert and verify Sale scoped
        val saleA = SaleEntity(
            id = "sale_1",
            orgId = "org_a",
            branchId = "br_1",
            registerId = "reg_1",
            invoiceNo = "INV-001",
            subtotalInCents = 350L,
            discountInCents = 0L,
            taxInCents = 18L,
            grandTotalInCents = 368L
        )
        saleDao.insertSale(saleA)

        val retrievedSale = saleDao.getSaleById("org_a", "sale_1")
        assertNotNull(retrievedSale)
        assertEquals("INV-001", retrievedSale?.invoiceNo)

        val crossOrgSale = saleDao.getSaleById("org_b", "sale_1")
        assertNull(crossOrgSale)
    }

    @Test
    fun `update and delete queries strictly isolate by org_id`() = runBlocking {
        val productDao = db.productDao()
        val userDao = db.userDao()

        val prod = ProductEntity(
            id = "prod_iso",
            orgId = "org_target",
            categoryId = "cat_1",
            name = "Latte",
            sku = "LAT-01",
            barcode = "777888",
            costPriceInCents = 200L,
            sellingPriceInCents = 400L
        )
        productDao.insertProduct(prod)

        // Try updating with mismatched orgId -> Should not alter record in org_target
        productDao.updateProductPrice("wrong_org", "prod_iso", "Latte Updated", 500L, 250L)
        val unchanged = productDao.getProductById("org_target", "prod_iso")
        assertEquals(400L, unchanged?.sellingPriceInCents)

        // Update with matching orgId
        productDao.updateProductPrice("org_target", "prod_iso", "Latte Updated", 500L, 250L)
        val updated = productDao.getProductById("org_target", "prod_iso")
        assertEquals(500L, updated?.sellingPriceInCents)
        assertEquals("Latte Updated", updated?.name)

        // Try deleting with wrong orgId -> Should NOT delete
        productDao.deleteProduct("wrong_org", "prod_iso")
        val stillExists = productDao.getProductById("org_target", "prod_iso")
        assertNotNull(stillExists)

        // Delete with matching orgId -> Deletes successfully
        productDao.deleteProduct("org_target", "prod_iso")
        val deleted = productDao.getProductById("org_target", "prod_iso")
        assertNull(deleted)
    }

    @Test
    fun `test barcode utils checksum and classification`() {
        // EAN-13 Checksum Calculation & Validation
        val prefix12 = "890123456789"
        val expectedCheckDigit = com.example.pos.domain.BarcodeUtils.calculateEan13Checksum(prefix12)
        val validEan13 = prefix12 + expectedCheckDigit
        val validRes = com.example.pos.domain.BarcodeUtils.validateBarcode(validEan13)
        assertEquals("EAN-13", validRes.format)
        org.junit.Assert.assertTrue(validRes.isValid)
        assertNull(validRes.warning)

        // Invalid EAN-13 Checksum Detection
        val badCheckDigit = (expectedCheckDigit + 1) % 10
        val invalidEan13 = prefix12 + badCheckDigit
        val invalidRes = com.example.pos.domain.BarcodeUtils.validateBarcode(invalidEan13)
        org.junit.Assert.assertFalse(invalidRes.isValid)
        org.junit.Assert.assertFalse(invalidRes.checksumValid)
        assertNotNull(invalidRes.warning)

        // Internal Code128 generation
        val internalBarcode = com.example.pos.domain.BarcodeUtils.generateInternalCode128("WAI-WAI-75G")
        assertEquals("INT-WAI-WAI-75G", internalBarcode)
        val code128Res = com.example.pos.domain.BarcodeUtils.validateBarcode(internalBarcode)
        assertEquals("Code128", code128Res.format)
        org.junit.Assert.assertTrue(code128Res.isValid)
    }

    @Test
    fun `test multi-barcode and carton pack size in room database`() = runBlocking {
        val posDao = db.posDao()
        val orgId = "org_nepal_mart"

        val masterItem = ProductEntity(
            id = "item_coke_330",
            orgId = orgId,
            categoryId = "cat_drinks",
            name = "Coca Cola 330ml Can",
            sku = "COKE-330",
            barcode = "5449000000996", // Single can barcode
            costPriceInCents = 4500L,
            sellingPriceInCents = 6000L
        )
        posDao.insertProduct(masterItem)

        // Map Carton of 24 Cans with secondary carton barcode
        val cartonBarcode = com.example.pos.data.local.ItemBarcodeEntity(
            id = "bc_carton_24",
            orgId = orgId,
            itemId = masterItem.id,
            barcode = "05449000000999", // Carton barcode
            packQty = 24
        )
        posDao.insertItemBarcode(cartonBarcode)

        // Lookup single can barcode
        val singleCan = posDao.getProductByBarcode(orgId, "5449000000996")
        assertNotNull(singleCan)
        assertEquals("item_coke_330", singleCan?.id)

        // Lookup carton barcode via item_barcodes link
        val cartonLink = posDao.getBarcodeLink(orgId, "05449000000999")
        assertNotNull(cartonLink)
        assertEquals(24, cartonLink?.packQty)
        assertEquals("item_coke_330", cartonLink?.itemId)

        // Verify linked parent item
        val parentItem = posDao.getProductById(cartonLink!!.itemId)
        assertNotNull(parentItem)
        assertEquals("Coca Cola 330ml Can", parentItem?.name)
    }

    @Test
    fun `data backup and restore json serialization and round trip`() {
        val org = OrganizationEntity(
            id = "org_bak_test",
            orgId = "org_bak_test",
            name = "Test Everest Grocers",
            taxId = "PAN-987654",
            currencySymbol = "Rs",
            taxMode = TaxMode.EXCLUSIVE,
            ownerUid = "owner_bak"
        )
        val settings = com.example.pos.data.local.StoreSettingsEntity(
            orgId = org.id,
            backupEmail = "shopowner@gmail.com",
            lastBackupAt = 1700000000000L,
            printerName = "Thermal POS 80"
        )
        val categories = listOf(
            com.example.pos.data.local.CategoryEntity(id = "cat_1", orgId = org.id, name = "Groceries", colorHex = "#10B981")
        )
        val products = listOf(
            ProductEntity(
                id = "prod_1",
                orgId = org.id,
                categoryId = "cat_1",
                name = "Basmati Rice 5kg",
                sku = "RICE-5KG",
                barcode = "8901234567890",
                costPriceInCents = 45000L,
                sellingPriceInCents = 55000L,
                taxRatePercent = 13.0
            )
        )
        val barcodes = listOf(
            com.example.pos.data.local.ItemBarcodeEntity(
                id = "bc_1",
                orgId = org.id,
                itemId = "prod_1",
                barcode = "8901234567890",
                packQty = 1
            )
        )
        val customers = listOf(
            com.example.pos.data.local.CustomerEntity(
                id = "cust_1",
                orgId = org.id,
                name = "Ram Sharma",
                phone = "9841000000",
                email = "ram@gmail.com",
                creditLimitInCents = 1000000L,
                currentDueInCents = 150000L
            )
        )
        val customerLedgers = listOf(
            com.example.pos.data.local.CustomerLedgerEntity(
                id = "led_1",
                orgId = org.id,
                customerId = "cust_1",
                saleId = "sale_1",
                entryType = "SALE_CREDIT",
                amountInCents = 150000L,
                balanceAfterInCents = 150000L,
                notes = "Store credit sale"
            )
        )
        val sales = listOf(
            SaleEntity(
                id = "sale_1",
                orgId = org.id,
                branchId = "branch_1",
                registerId = "reg_1",
                invoiceNo = "INV-001",
                customerId = "cust_1",
                subtotalInCents = 55000L,
                discountInCents = 0L,
                taxInCents = 7150L,
                grandTotalInCents = 62150L,
                paymentStatus = "COMPLETED"
            )
        )

        // 1. Generate Backup JSON
        val json = com.example.pos.domain.DataBackupManager.createBackupJson(
            org = org,
            storeSettings = settings,
            categories = categories,
            products = products,
            itemBarcodes = barcodes,
            customers = customers,
            customerLedger = customerLedgers,
            sales = sales,
            saleItems = emptyList(),
            salePayments = emptyList(),
            stockMovements = emptyList(),
            shifts = emptyList(),
            cashMovements = emptyList()
        )

        assertNotNull(json)
        assertTrue(json.contains("Test Everest Grocers"))
        assertTrue(json.contains("shopowner@gmail.com"))
        assertTrue(json.contains("Basmati Rice 5kg"))
        assertTrue(json.contains("Ram Sharma"))

        // 2. Parse Backup JSON back
        val parsed = com.example.pos.domain.DataBackupManager.parseBackupJson(json, org.id)
        assertEquals(1, parsed.counts.productsCount)
        assertEquals(1, parsed.counts.customersCount)
        assertEquals(1, parsed.counts.categoriesCount)
        assertEquals(1, parsed.counts.barcodesCount)
        assertEquals(1, parsed.counts.customerLedgerCount)
        assertEquals(1, parsed.counts.salesCount)
        assertEquals("Test Everest Grocers", parsed.orgName)
        assertEquals("Basmati Rice 5kg", parsed.products[0].name)
        assertEquals("Ram Sharma", parsed.customers[0].name)
        assertEquals("shopowner@gmail.com", parsed.storeSettings?.backupEmail)
    }

    @Test
    fun `database restore into room db preserves data and handles linked gmail`() = runBlocking {
        val posDao = db.posDao()
        val orgDao = db.organizationDao()
        val orgId = "org_restore_test"

        val originalOrg = OrganizationEntity(
            id = orgId,
            orgId = orgId,
            name = "Original Store",
            taxId = "PAN-111",
            currencySymbol = "Rs",
            taxMode = TaxMode.EXCLUSIVE,
            ownerUid = "owner_test"
        )
        orgDao.insertOrg(originalOrg)

        val settings = com.example.pos.data.local.StoreSettingsEntity(
            orgId = orgId,
            backupEmail = "finance@company.com",
            lastBackupAt = System.currentTimeMillis()
        )
        posDao.insertStoreSettings(settings)

        val fetchedSettings = posDao.getStoreSettings(orgId).first()
        assertNotNull(fetchedSettings)
        assertEquals("finance@company.com", fetchedSettings?.backupEmail)

        // Create backup JSON with new products and customers
        val newCategory = com.example.pos.data.local.CategoryEntity(id = "cat_imp", orgId = orgId, name = "Imported Category")
        val newProduct = ProductEntity(
            id = "prod_imp",
            orgId = orgId,
            categoryId = "cat_imp",
            name = "Imported Honey 500g",
            sku = "HONEY-500",
            barcode = "9998887776665",
            costPriceInCents = 30000L,
            sellingPriceInCents = 42000L
        )
        val newCustomer = com.example.pos.data.local.CustomerEntity(
            id = "cust_imp",
            orgId = orgId,
            name = "Sita Devi",
            phone = "9800000000",
            email = "sita@gmail.com",
            creditLimitInCents = 500000L,
            currentDueInCents = 0L
        )

        val backupJson = com.example.pos.domain.DataBackupManager.createBackupJson(
            org = originalOrg,
            storeSettings = fetchedSettings,
            categories = listOf(newCategory),
            products = listOf(newProduct),
            itemBarcodes = emptyList(),
            customers = listOf(newCustomer),
            customerLedger = emptyList(),
            sales = emptyList(),
            saleItems = emptyList(),
            salePayments = emptyList(),
            stockMovements = emptyList(),
            shifts = emptyList(),
            cashMovements = emptyList()
        )

        // Parse and restore into DB
        val parsed = com.example.pos.domain.DataBackupManager.parseBackupJson(backupJson, orgId)
        posDao.insertCategories(parsed.categories)
        posDao.insertProducts(parsed.products)
        posDao.insertCustomers(parsed.customers)

        // Verify products and customers exist in Room DB
        val restoredProduct = posDao.getProductById("prod_imp")
        assertNotNull(restoredProduct)
        assertEquals("Imported Honey 500g", restoredProduct?.name)
        assertEquals(42000L, restoredProduct?.sellingPriceInCents)

        val restoredCustomer = posDao.getCustomerById("cust_imp")
        assertNotNull(restoredCustomer)
        assertEquals("Sita Devi", restoredCustomer?.name)
    }

    @Test
    fun `test date and uuid typeconverters`() {
        val converters = Converters()

        // Date converters
        val now = Date()
        val timestamp = converters.dateToTimestamp(now)
        assertNotNull(timestamp)
        val reconstructedDate = converters.fromTimestamp(timestamp)
        assertEquals(now.time, reconstructedDate?.time)

        assertNull(converters.dateToTimestamp(null))
        assertNull(converters.fromTimestamp(null))

        // UUID converters
        val originalUuid = UUID.randomUUID()
        val uuidString = converters.fromUUID(originalUuid)
        assertNotNull(uuidString)
        assertEquals(originalUuid.toString(), uuidString)

        val reconstructedUuid = converters.toUUID(uuidString)
        assertEquals(originalUuid, reconstructedUuid)

        assertNull(converters.fromUUID(null))
        assertNull(converters.toUUID(null))
        assertNull(converters.toUUID("invalid-uuid-string"))
    }

    @Test
    fun `test appdatabase entity registration and daos for Organization, User, Product, and Sale`() = runBlocking {
        val orgDao = db.organizationDao()
        val userDao = db.userDao()
        val productDao = db.productDao()
        val saleDao = db.saleDao()

        // 1. Organization
        val org = Organization(
            id = "org_test_101",
            orgId = "org_test_101",
            name = "Test Supermarket",
            taxId = "PAN-12345",
            currencySymbol = "Rs",
            taxMode = TaxMode.INCLUSIVE,
            ownerUid = "owner_uid_101"
        )
        orgDao.insertOrg(org)
        val fetchedOrg = orgDao.getOrgById("org_test_101")
        assertNotNull(fetchedOrg)
        assertEquals("Test Supermarket", fetchedOrg?.name)

        // 2. User
        val user = User(
            id = "user_test_202",
            orgId = "org_test_101",
            fullName = "Ram Bahadur",
            email = "ram@test.com",
            role = StaffRole.CASHIER
        )
        userDao.insertUser(user)
        val fetchedUser = userDao.getUserById(orgId = "org_test_101", userId = "user_test_202")
        assertNotNull(fetchedUser)
        assertEquals("Ram Bahadur", fetchedUser?.fullName)

        // 3. Product
        val product = Product(
            id = "prod_test_303",
            orgId = "org_test_101",
            categoryId = "cat_tea",
            name = "Himalayan Green Tea",
            sku = "TEA-01",
            barcode = "TEA987654",
            costPriceInCents = 18000L,
            sellingPriceInCents = 25000L,
            taxRatePercent = 13.0,
            unit = "pcs",
            minStockAlert = 5
        )
        productDao.insertProduct(product)
        val fetchedProduct = productDao.getProductById(orgId = "org_test_101", productId = "prod_test_303")
        assertNotNull(fetchedProduct)
        assertEquals("Himalayan Green Tea", fetchedProduct?.name)
        assertEquals(25000L, fetchedProduct?.sellingPriceInCents)

        // 4. Sale
        val saleId = UUID.randomUUID().toString()
        val sale = Sale(
            id = saleId,
            orgId = "org_test_101",
            branchId = "branch_01",
            registerId = "reg_01",
            invoiceNo = "INV-00101",
            customerId = null,
            subtotalInCents = 25000L,
            discountInCents = 0L,
            taxInCents = 3250L,
            grandTotalInCents = 28250L,
            paymentStatus = "COMPLETED",
            createdAt = System.currentTimeMillis(),
            createdBy = "user_test_202"
        )
        saleDao.insertSale(sale)
        val fetchedSale = saleDao.getSaleById(orgId = "org_test_101", saleId = saleId)
        assertNotNull(fetchedSale)
        assertEquals("INV-00101", fetchedSale?.invoiceNo)
        assertEquals(28250L, fetchedSale?.grandTotalInCents)
    }
}

