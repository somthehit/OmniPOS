package com.example.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftFinancialEngineTest {

    @Test
    fun testCalculateExpectedCash() {
        val openingFloat = 500000L // Rs 5,000.00
        val cashSales = 1200000L  // Rs 12,000.00
        val creditCollections = 150000L // Rs 1,500.00
        val paidIn = 50000L      // Rs 500.00
        val cashRefunds = 40000L  // Rs 400.00
        val paidOut = 30000L     // Rs 300.00
        val safeDrops = 500000L   // Rs 5,000.00

        val expected = ShiftFinancialEngine.calculateExpectedCash(
            openingFloatInCents = openingFloat,
            cashSalesInCents = cashSales,
            cashCreditCollectionsInCents = creditCollections,
            paidInInCents = paidIn,
            cashRefundsInCents = cashRefunds,
            paidOutInCents = paidOut,
            safeDropsInCents = safeDrops
        )

        // 5000 + 12000 + 1500 + 500 - 400 - 300 - 5000 = 13,300.00 (1330000 cents)
        assertEquals(1330000L, expected)
    }

    @Test
    fun testCreditNoteOnlyCashRefundAffectsDrawer() {
        // Total credit note issued = Rs 2,000 (Rs 1,500 store credit + Rs 500 cash refund)
        // Only the Rs 500 cash refund affects drawer cash!
        val opening = 500000L
        val expected = ShiftFinancialEngine.calculateExpectedCash(
            openingFloatInCents = opening,
            cashSalesInCents = 1000000L,
            cashCreditCollectionsInCents = 0L,
            paidInInCents = 0L,
            cashRefundsInCents = 50000L, // only cash refund
            paidOutInCents = 0L,
            safeDropsInCents = 0L
        )

        // 5000 + 10000 - 500 = 14,500.00
        assertEquals(1450000L, expected)
    }

    @Test
    fun testCashVariance() {
        val expected = 1000000L // Rs 10,000.00

        // Balanced
        assertEquals(0L, ShiftFinancialEngine.calculateCashVariance(1000000L, expected))

        // Surplus / Overage: counted 10,200
        assertEquals(20000L, ShiftFinancialEngine.calculateCashVariance(1020000L, expected))

        // Shortage: counted 9,800
        assertEquals(-20000L, ShiftFinancialEngine.calculateCashVariance(980000L, expected))
    }

    @Test
    fun testVarianceThresholdLevels() {
        // Under tolerance (<= Rs 50.00 / 5000 cents)
        assertEquals(
            ShiftFinancialEngine.VarianceStatus.UNDER_TOLERANCE,
            ShiftFinancialEngine.checkVarianceThreshold(3000L)
        )
        assertEquals(
            ShiftFinancialEngine.VarianceStatus.UNDER_TOLERANCE,
            ShiftFinancialEngine.checkVarianceThreshold(-5000L)
        )

        // Warning reason required (> 50 && <= 500)
        assertEquals(
            ShiftFinancialEngine.VarianceStatus.WARN_REASON_REQUIRED,
            ShiftFinancialEngine.checkVarianceThreshold(25000L) // Rs 250
        )
        assertEquals(
            ShiftFinancialEngine.VarianceStatus.WARN_REASON_REQUIRED,
            ShiftFinancialEngine.checkVarianceThreshold(-50000L) // Rs -500
        )

        // Hard block supervisor override required (> 500)
        assertEquals(
            ShiftFinancialEngine.VarianceStatus.HARD_BLOCK_SUPERVISOR_REQUIRED,
            ShiftFinancialEngine.checkVarianceThreshold(60000L) // Rs 600
        )
        assertEquals(
            ShiftFinancialEngine.VarianceStatus.HARD_BLOCK_SUPERVISOR_REQUIRED,
            ShiftFinancialEngine.checkVarianceThreshold(-150000L) // Rs -1,500
        )
    }

    @Test
    fun testStaleShiftDetection() {
        val now = 1700000000000L
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(now))

        // Active 3 hours ago, today -> not stale
        val threeHoursAgo = now - (3 * 3600 * 1000L)
        assertFalse(ShiftFinancialEngine.isShiftStale(threeHoursAgo, todayStr, now))

        // Active 14 hours ago -> STALE (>12h)
        val fourteenHoursAgo = now - (14 * 3600 * 1000L)
        assertTrue(ShiftFinancialEngine.isShiftStale(fourteenHoursAgo, todayStr, now))

        // Prior business date -> STALE
        assertTrue(ShiftFinancialEngine.isShiftStale(threeHoursAgo, "2023-11-10", now))
    }

    @Test
    fun testCalculateTotalCashFromDenominations() {
        val counts = listOf(
            1000 to 5, // 5000
            500 to 4,  // 2000
            100 to 10, // 1000
            50 to 4,   // 200
            20 to 5,   // 100
            10 to 10   // 100
        )
        val total = ShiftFinancialEngine.calculateTotalCash(counts)
        // 5000 + 2000 + 1000 + 200 + 100 + 100 = 8400.00 (840000 cents)
        assertEquals(840000L, total)
    }

    @Test
    fun testCalculateDepositAmount() {
        val countedCash = 1500000L // Rs 15,000.00
        val closingFloat = 500000L  // Rs 5,000.00 kept in drawer
        val deposit = ShiftFinancialEngine.calculateDepositAmount(countedCash, closingFloat)
        assertEquals(1000000L, deposit) // Rs 10,000.00 deposited
    }

    @Test
    fun testNepaliCurrencyFormatting() {
        val formatted = ShiftFinancialEngine.formatNepaliCurrency(10000000L, "Rs")
        assertEquals("Rs 1,00,000.00", formatted)

        val smallFormatted = ShiftFinancialEngine.formatNepaliCurrency(50000L, "Rs")
        assertEquals("Rs 500.00", smallFormatted)
    }
}
