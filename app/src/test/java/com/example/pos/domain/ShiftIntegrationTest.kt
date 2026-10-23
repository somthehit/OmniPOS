package com.example.pos.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.pos.data.local.AppDatabase
import com.example.pos.data.local.PosDao
import com.example.pos.data.local.RegisterEntity
import com.example.pos.data.model.CreditNoteRecord
import com.example.pos.data.model.DenominationItem
import com.example.pos.data.model.OnlineProviderDeclaration
import com.example.pos.data.model.ShiftCloseDraft
import com.example.pos.data.model.ShiftGateDecision
import com.example.pos.data.repository.PosRepository
import kotlinx.coroutines.test.runTest
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
class ShiftIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PosDao
    private lateinit var repository: PosRepository

    private val orgId = "org_test"
    private val branchId = "br_test"
    private val registerId = "reg_01"
    private val cashierUid = "usr_cashier_01"
    private val cashierName = "Ram Sharma"

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.posDao()
        repository = PosRepository(dao)

        // Seed Register
        dao.insertRegister(
            RegisterEntity(
                id = registerId,
                orgId = orgId,
                branchId = branchId,
                name = "Counter 1",
                code = "REG1"
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCompleteShiftLifecycle_Open_Movements_Close_Audit() = runTest {
        // 1. Initially gate check returns NoActiveShift
        val initialGate = repository.checkShiftGate(registerId, cashierUid)
        assertTrue(initialGate is ShiftGateDecision.NoActiveShift)

        // 2. Open Shift with Rs 5,000 float (Counted 5,000, Expected 5,000, Gap = 0)
        val openingDenominations = listOf(
            DenominationItem(denomination = 1000, count = 3),
            DenominationItem(denomination = 500, count = 4)
        )
        val openedShift = repository.openShift(
            orgId = orgId,
            branchId = branchId,
            registerId = registerId,
            cashierUid = cashierUid,
            cashierName = cashierName,
            openingFloatInCents = 500000L,
            expectedFloatInCents = 500000L,
            openingGapReason = "",
            denominationCounts = openingDenominations
        )

        assertNotNull(openedShift)
        assertEquals("OPEN", openedShift.status)
        assertEquals(500000L, openedShift.openingFloatInCents)
        assertEquals(0L, openedShift.openingGapInCents)

        // Verify register active shift bound
        val activeShiftInReg = dao.getActiveShiftSync(registerId)
        assertEquals(openedShift.id, activeShiftInReg?.id)

        // 3. Gate check now returns ResumeShift
        val gateWithActive = repository.checkShiftGate(registerId, cashierUid)
        assertTrue(gateWithActive is ShiftGateDecision.ResumeShift)
        assertEquals(openedShift.id, (gateWithActive as ShiftGateDecision.ResumeShift).shift.id)

        // TerminalInUse check if another cashier checks the same register
        val gateOtherCashier = repository.checkShiftGate(registerId, "usr_other_cashier")
        assertTrue(gateOtherCashier is ShiftGateDecision.TerminalInUse)

        // 4. Cash movements: Paid In (Rs 500), Paid Out (Rs 200), Safe Drop (Rs 2,000)
        repository.addCashMovement(
            shiftId = openedShift.id,
            movementType = "PAY_IN",
            amountInCents = 50000L,
            reason = "Added small change",
            createdBy = cashierName
        )
        repository.addCashMovement(
            shiftId = openedShift.id,
            movementType = "PAY_OUT",
            amountInCents = 20000L,
            reason = "Bought cleaning supplies",
            createdBy = cashierName
        )
        repository.addCashMovement(
            shiftId = openedShift.id,
            movementType = "SAFE_DROP",
            amountInCents = 200000L,
            reason = "Mid-day safe drop",
            createdBy = cashierName
        )

        // Verify Live Metrics: 5000 + 500 - 200 - 2000 = Rs 3,300 (330000 cents)
        val metrics = repository.getLiveShiftMetrics(openedShift.id)
        assertNotNull(metrics)
        assertEquals(330000L, metrics!!.expectedCashInCents)
        assertEquals(50000L, metrics.payInInCents)
        assertEquals(20000L, metrics.payOutInCents)
        assertEquals(200000L, metrics.safeDropInCents)

        // 5. Save Draft Closing
        val draftCounts = listOf(
            DenominationItem(denomination = 1000, count = 3),
            DenominationItem(denomination = 100, count = 3)
        )
        repository.saveCloseDraft(
            ShiftCloseDraft(
                shiftId = openedShift.id,
                countedCashInCents = 330000L,
                denominationCounts = draftCounts,
                closingFloatInCents = 100000L,
                depositAmountInCents = 230000L,
                varianceReason = "Balanced count"
            )
        )

        val shiftInDraft = dao.getShiftById(openedShift.id)
        assertEquals("CLOSING", shiftInDraft?.status)

        // 6. Close Shift Advanced (With Declarations and Credit Note)
        val closingDenominations = listOf(
            DenominationItem(denomination = 1000, count = 3),
            DenominationItem(denomination = 100, count = 3)
        )
        val onlineDeclarations = listOf(
            OnlineProviderDeclaration(provider = "eSewa", declaredAmountInCents = 150000L, slipCount = 3),
            OnlineProviderDeclaration(provider = "Fonepay", declaredAmountInCents = 50000L, slipCount = 1)
        )
        val creditNotes = listOf(
            CreditNoteRecord(
                creditNoteNo = "CN-001",
                totalAmountInCents = 200000L, // Rs 2,000 total credit note
                customerCreditInCents = 150000L, // Rs 1,500 store credit (does NOT affect drawer)
                bankRefundInCents = 0L,
                cashRefundInCents = 50000L, // Rs 500 cash refund (REDUCES drawer cash)
                reason = "Damaged return"
            )
        )

        // Note: With Rs 500 cash refund, expected cash becomes 3300 - 500 = Rs 2,800.
        // If counted is Rs 2,800, variance is 0 (APPROVED).
        val finalCounted = 280000L
        val closingFloat = 100000L // Rs 1,000 kept in drawer
        val depositAmount = finalCounted - closingFloat // Rs 1,800 deposited

        val zReport = repository.closeShiftAdvanced(
            shiftId = openedShift.id,
            countedCashInCents = finalCounted,
            denominationCounts = closingDenominations,
            onlineDeclarations = onlineDeclarations,
            creditNotes = creditNotes,
            closingFloatInCents = closingFloat,
            depositAmountInCents = depositAmount,
            varianceReason = "Balanced with return refund",
            closedBy = cashierName
        )

        assertNotNull(zReport)
        assertEquals(0L, zReport!!.cashVarianceInCents)
        assertEquals("APPROVED", zReport.status)
        assertEquals(280000L, zReport.expectedCashInCents)
        assertEquals(180000L, zReport.depositAmountInCents)

        // Register is now free
        val registerAfterClose = dao.getActiveShiftSync(registerId)
        assertNull(registerAfterClose)

        // 7. Verify Shift Details and Audit Events
        val details = repository.getShiftDetails(openedShift.id)
        assertNotNull(details)
        assertEquals("CLOSED", details!!.shift.status)
        assertEquals("APPROVED", details.shift.reviewStatus)
        assertTrue(details.cashCounts.isNotEmpty())
        assertEquals(2, details.declarations.size)
        assertEquals(1, details.creditNotes.size)
        assertTrue(details.events.any { it.eventType == "OPEN_SHIFT" })
        assertTrue(details.events.any { it.eventType == "CLOSE_SHIFT" })
    }

    @Test
    fun testAdminReopenAndSupervisorAdjustment() = runTest {
        // Open and close a shift with a small shortage (-Rs 300 / -30000 cents)
        val shift = repository.openShift(
            orgId = orgId,
            branchId = branchId,
            registerId = registerId,
            cashierUid = cashierUid,
            cashierName = cashierName,
            openingFloatInCents = 500000L
        )

        // Close with counted Rs 4,700 (Expected Rs 5,000, Shortage Rs 300)
        repository.closeShiftAdvanced(
            shiftId = shift.id,
            countedCashInCents = 470000L,
            closingFloatInCents = 100000L,
            depositAmountInCents = 370000L,
            varianceReason = "Till shortage under review",
            closedBy = cashierName
        )

        val closed = dao.getShiftById(shift.id)
        assertNotNull(closed)
        assertEquals("PENDING_REVIEW", closed!!.reviewStatus)
        assertEquals(-30000L, closed.varianceInCents)
        assertEquals(1, closed.version)

        // 1. Supervisor Adjustment: adjust +Rs 300 found in drawer bottom
        val adjusted = repository.supervisorAdjustment(
            shiftId = shift.id,
            supervisorUid = "sup_01",
            supervisorName = "Supervisor Sita",
            amountInCents = 30000L,
            reason = "Found dropped notes under cash drawer tray"
        )
        assertTrue(adjusted)

        val shiftAfterAdj = dao.getShiftById(shift.id)
        assertEquals(0L, shiftAfterAdj?.varianceInCents)

        // Verify Adjustment record exists
        val adjustments = dao.getShiftAdjustmentsSync(shift.id)
        assertEquals(1, adjustments.size)
        assertEquals(30000L, adjustments[0].amountInCents)
        assertEquals("Supervisor Sita", adjustments[0].approvedBy)

        // 2. Admin Reopen Shift: creates snapshot version and bumps version to 2
        val reopened = repository.adminReopenShift(
            shiftId = shift.id,
            adminUid = "admin_01",
            adminName = "Store Manager",
            reason = "Cashier omitted end-of-day return transaction"
        )
        assertTrue(reopened)

        val shiftAfterReopen = dao.getShiftById(shift.id)
        assertEquals("OPEN", shiftAfterReopen?.status)
        assertEquals(2, shiftAfterReopen?.version)
        assertNull(shiftAfterReopen?.closedAt)

        // Verify snapshot in shift_versions
        val versions = dao.getShiftVersionsSync(shift.id)
        assertEquals(1, versions.size)
        assertEquals(1, versions[0].version)
        assertEquals("admin_01", versions[0].changedBy)

        // 3. Admin Review Shift (after re-closing)
        repository.closeShiftAdvanced(
            shiftId = shift.id,
            countedCashInCents = 500000L,
            closedBy = cashierName
        )
        val reviewed = repository.adminReviewShift(
            shiftId = shift.id,
            adminUid = "admin_01",
            adminName = "Store Manager",
            reviewStatus = "APPROVED",
            reviewNote = "Verified and reconciled"
        )
        assertTrue(reviewed)

        val finalShift = dao.getShiftById(shift.id)
        assertEquals("APPROVED", finalShift?.reviewStatus)
        assertEquals("Store Manager", finalShift?.reviewedBy)
    }
}
