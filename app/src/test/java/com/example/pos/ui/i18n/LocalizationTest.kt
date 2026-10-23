package com.example.pos.ui.i18n

import com.example.pos.data.local.StoreSettingsEntity
import com.example.pos.domain.DataBackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalizationTest {

    @Test
    fun appLanguage_fromCode_mapsCorrectly() {
        assertEquals(AppLanguage.NEPALI, AppLanguage.fromCode("ne"))
        assertEquals(AppLanguage.NEPALI, AppLanguage.fromCode("NE"))
        assertEquals(AppLanguage.NEPALI, AppLanguage.fromCode("Ne"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("EN"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("unknown"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(""))
    }

    @Test
    fun nepaliStrings_providesAccurateTranslations() {
        val s = NepaliStrings
        assertEquals("बिलिङ", s.navRegister)
        assertEquals("मौज्दात", s.navInventory)
        assertEquals("ग्राहकहरू", s.navCustomers)
        assertEquals("रिपोर्टहरू", s.navReports)
        assertEquals("सेटिङ्स", s.navSettings)

        assertEquals("शिफ्ट चालू", s.shiftOpen)
        assertEquals("शिफ्ट बन्द", s.shiftClosed)
        assertEquals("काउन्टर लक", s.lockRegister)
        assertEquals("कर्मचारी परिवर्तन", s.switchOperator)

        assertEquals("भाषा र स्थानियकरण", s.language)
        assertEquals("भाषा छनौट गर्नुहोस्", s.selectLanguage)
        assertEquals("नेपाली (Nepali)", s.nepali)

        assertEquals("बिल कार्ट", s.cartTitle)
        assertEquals("उप-जम्मा", s.subtotal)
        assertEquals("छुट", s.discount)
        assertEquals("कुल जम्मा", s.grandTotal)
        assertEquals("भुक्तानी गर्नुहोस्", s.payNow)

        assertEquals("शाखा मौज्दात", s.branchInventory)
        assertEquals("+ स्टक दाखिला", s.receiveStock)

        assertEquals("ग्राहक खाता", s.customersTitle)
        assertEquals("+ नयाँ ग्राहक", s.addCustomer)

        assertEquals("कारोबार रिपोर्ट", s.reportsTitle)
        assertEquals("कुल आम्दानी", s.totalRevenue)
        assertEquals("सम्पन्न बिक्री", s.totalOrders)
        assertEquals("नयाँ कर्मचारी थप्नुहोस्", s.addStaffMember)
        assertEquals("कर्मचारी सुरक्षित गर्नुहोस्", s.saveStaffButton)
    }

    @Test
    fun englishStrings_providesCompleteStrings() {
        val s = EnglishStrings
        assertEquals("Register", s.navRegister)
        assertEquals("Inventory", s.navInventory)
        assertEquals("Customers", s.navCustomers)
        assertEquals("Reports", s.navReports)
        assertEquals("Settings", s.navSettings)

        assertEquals("Shift OPEN", s.shiftOpen)
        assertEquals("Shift CLOSED", s.shiftClosed)
        assertEquals("Lock Terminal", s.lockRegister)

        assertEquals("Language & Localization", s.language)
        assertEquals("Choose Language", s.selectLanguage)
        assertEquals("Add Staff Member", s.addStaffMember)
        assertEquals("Add Staff Member", s.saveStaffButton)
    }

    @Test
    fun storeSettings_languageBackupRoundTrip() {
        val org = com.example.pos.data.local.OrganizationEntity(
            id = "org_nepali_test",
            name = "Nepal Store",
            taxId = "123456789",
            currencySymbol = "Rs",
            ownerUid = "owner_test_123"
        )
        val settings = StoreSettingsEntity(
            orgId = "org_nepali_test",
            language = "ne",
            backupEmail = "test@nepalipos.np"
        )

        val json = DataBackupManager.createBackupJson(
            org = org,
            storeSettings = settings,
            categories = emptyList(),
            products = emptyList(),
            itemBarcodes = emptyList(),
            customers = emptyList(),
            customerLedger = emptyList(),
            sales = emptyList(),
            saleItems = emptyList(),
            salePayments = emptyList(),
            stockMovements = emptyList(),
            shifts = emptyList(),
            cashMovements = emptyList()
        )

        assertTrue(json.contains("\"language\": \"ne\""))

        val parsed = DataBackupManager.parseBackupJson(json, "org_nepali_test")
        assertNotNull(parsed.storeSettings)
        assertEquals("ne", parsed.storeSettings?.language)
    }
}
