package com.example.pos.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.pos.data.local.SalePaymentEntity
import com.example.pos.data.local.StoreSettingsEntity
import com.example.pos.data.model.PaymentEntry
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.StaffRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CashDrawerManagerTest {

    private lateinit var context: Context
    private lateinit var printerManager: ThermalPrinterManager
    private lateinit var drawerManager: CashDrawerManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        printerManager = ThermalPrinterManager(context)
        drawerManager = CashDrawerManager(printerManager)
    }

    @Test
    fun `test cash tender identification detects cash correctly and ignores card or qr`() {
        val cashOnly = listOf(
            PaymentEntry(PaymentType.CASH, 250000L)
        )
        val cardOnly = listOf(
            PaymentEntry(PaymentType.CARD, 150000L)
        )
        val qrOnly = listOf(
            PaymentEntry(PaymentType.QR_WALLET, 100000L)
        )
        val splitCashAndCard = listOf(
            PaymentEntry(PaymentType.CARD, 100000L),
            PaymentEntry(PaymentType.CASH, 50000L)
        )
        val zeroCash = listOf(
            PaymentEntry(PaymentType.CASH, 0L)
        )

        assertTrue("Cash tender should be detected", drawerManager.hasCashTender(cashOnly))
        assertFalse("Card tender alone should not trigger cash drawer", drawerManager.hasCashTender(cardOnly))
        assertFalse("QR tender alone should not trigger cash drawer", drawerManager.hasCashTender(qrOnly))
        assertTrue("Split payment containing cash should trigger cash drawer", drawerManager.hasCashTender(splitCashAndCard))
        assertFalse("Zero cash amount should not trigger drawer", drawerManager.hasCashTender(zeroCash))

        // Also test SalePaymentEntity overload
        val entityCash = listOf(
            SalePaymentEntity(
                id = "p1",
                saleId = "s1",
                paymentType = PaymentType.CASH,
                amountInCents = 10000L,
                changeGivenInCents = 0L
            )
        )
        val entityCard = listOf(
            SalePaymentEntity(
                id = "p2",
                saleId = "s1",
                paymentType = PaymentType.CARD,
                amountInCents = 10000L,
                changeGivenInCents = 0L
            )
        )
        assertTrue("SalePaymentEntity cash should be detected", drawerManager.hasCashTenderEntities(entityCash))
        assertFalse("SalePaymentEntity card should not be detected as cash", drawerManager.hasCashTenderEntities(entityCard))
    }

    @Test
    fun `test role authorization permits cash sale but restricts manual test to managers and admins`() {
        // Operational Cash Sale is allowed for all roles during till operation
        assertTrue(drawerManager.isAuthorized(StaffRole.CASHIER, DrawerTriggerReason.CASH_SALE))
        assertTrue(drawerManager.isAuthorized(StaffRole.MANAGER, DrawerTriggerReason.CASH_SALE))
        assertTrue(drawerManager.isAuthorized(StaffRole.ADMIN, DrawerTriggerReason.CASH_SALE))
        assertTrue(drawerManager.isAuthorized(StaffRole.OWNER, DrawerTriggerReason.CASH_SALE))

        // Manual admin test or override requires elevated privileges
        assertFalse("Cashier cannot execute manual admin test kick", drawerManager.isAuthorized(StaffRole.CASHIER, DrawerTriggerReason.MANUAL_ADMIN_TEST))
        assertFalse("Storekeeper cannot execute manual admin test kick", drawerManager.isAuthorized(StaffRole.STOREKEEPER, DrawerTriggerReason.MANUAL_ADMIN_TEST))
        assertTrue("Manager can execute manual admin test kick", drawerManager.isAuthorized(StaffRole.MANAGER, DrawerTriggerReason.MANUAL_ADMIN_TEST))
        assertTrue("Admin can execute manual admin test kick", drawerManager.isAuthorized(StaffRole.ADMIN, DrawerTriggerReason.MANUAL_ADMIN_TEST))
        assertTrue("Owner can execute manual admin test kick", drawerManager.isAuthorized(StaffRole.OWNER, DrawerTriggerReason.MANUAL_ADMIN_TEST))

        // Supervisor override
        assertFalse(drawerManager.isAuthorized(StaffRole.CASHIER, DrawerTriggerReason.SUPERVISOR_OVERRIDE))
        assertTrue(drawerManager.isAuthorized(StaffRole.MANAGER, DrawerTriggerReason.SUPERVISOR_OVERRIDE))
        assertTrue(drawerManager.isAuthorized(StaffRole.ADMIN, DrawerTriggerReason.SUPERVISOR_OVERRIDE))
    }

    @Test
    fun `test drawer kick is blocked when disabled in store settings`() = runBlocking {
        val disabledSettings = StoreSettingsEntity(
            orgId = "org_1",
            cashDrawerTrigger = false,
            printerName = "BT_PRINTER_01"
        )

        var auditAction: String? = null
        val result = drawerManager.requestDrawerKick(
            reason = DrawerTriggerReason.CASH_SALE,
            role = StaffRole.CASHIER,
            settings = disabledSettings,
            onAuditLog = { act, _ -> auditAction = act }
        )

        assertTrue(result is DrawerKickResult.Blocked)
        val blocked = result as DrawerKickResult.Blocked
        assertTrue(blocked.reason.contains("disabled in store settings"))
    }

    @Test
    fun `test unauthorized cashier manual kick emits audit log and is blocked`() = runBlocking {
        val enabledSettings = StoreSettingsEntity(
            orgId = "org_1",
            cashDrawerTrigger = true,
            printerName = "BT_PRINTER_01"
        )

        var auditAction: String? = null
        var auditDetail: String? = null

        val result = drawerManager.requestDrawerKick(
            reason = DrawerTriggerReason.MANUAL_ADMIN_TEST,
            role = StaffRole.CASHIER,
            settings = enabledSettings,
            onAuditLog = { act, det ->
                auditAction = act
                auditDetail = det
            }
        )

        assertTrue(result is DrawerKickResult.Blocked)
        assertEquals("DRAWER_KICK_DENIED", auditAction)
        assertTrue(auditDetail?.contains("not authorized") == true)
    }

    @Test
    fun `test missing printer configuration returns error`() = runBlocking {
        val settingsNoPrinter = StoreSettingsEntity(
            orgId = "org_1",
            cashDrawerTrigger = true,
            printerName = "" // no printer configured
        )

        val result = drawerManager.requestDrawerKick(
            reason = DrawerTriggerReason.MANUAL_ADMIN_TEST,
            role = StaffRole.ADMIN,
            settings = settingsNoPrinter
        )

        assertTrue(result is DrawerKickResult.Error)
        val err = result as DrawerKickResult.Error
        assertTrue(err.reason.contains("No configured thermal printer"))
    }
}
