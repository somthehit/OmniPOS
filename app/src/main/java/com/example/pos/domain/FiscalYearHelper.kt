package com.example.pos.domain

import com.example.pos.data.local.FiscalYearEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FiscalYearHelper {

    /**
     * Formats consecutive fiscal invoice number conforming to standard retail & IRD billing rules.
     * Example: INV-81/82-00001
     */
    fun formatFiscalInvoice(prefix: String, fyCode: String, sequence: Long): String {
        val cleanPrefix = prefix.ifBlank { "INV" }
        val cleanCode = fyCode.ifBlank { "81/82" }
        return String.format(Locale.US, "%s-%s-%05d", cleanPrefix, cleanCode, sequence.coerceAtLeast(1L))
    }

    /**
     * Generates a predefined set of Nepali Fiscal Years (Bikram Sambat cycle starting Shrawan 1 / mid-July).
     * Automatically sets the fiscal year matching the current date as active.
     */
    fun getDefaultFiscalYears(orgId: String, currentTime: Long = System.currentTimeMillis()): List<FiscalYearEntity> {
        val cal = Calendar.getInstance()

        fun makeTime(year: Int, month0: Int, day: Int, hour: Int, min: Int, sec: Int): Long {
            cal.set(year, month0, day, hour, min, sec)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val list = listOf(
            FiscalYearEntity(
                id = "fy_${orgId}_2080_81",
                orgId = orgId,
                name = "2080/81",
                code = "80/81",
                startDate = makeTime(2023, Calendar.JULY, 17, 0, 0, 0),
                endDate = makeTime(2024, Calendar.JULY, 15, 23, 59, 59),
                invoicePrefix = "INV",
                currentInvoiceSequence = 0L,
                isCurrent = false,
                isClosed = true
            ),
            FiscalYearEntity(
                id = "fy_${orgId}_2081_82",
                orgId = orgId,
                name = "2081/82",
                code = "81/82",
                startDate = makeTime(2024, Calendar.JULY, 16, 0, 0, 0),
                endDate = makeTime(2025, Calendar.JULY, 15, 23, 59, 59),
                invoicePrefix = "INV",
                currentInvoiceSequence = 0L,
                isCurrent = false,
                isClosed = false
            ),
            FiscalYearEntity(
                id = "fy_${orgId}_2082_83",
                orgId = orgId,
                name = "2082/83",
                code = "82/83",
                startDate = makeTime(2025, Calendar.JULY, 16, 0, 0, 0),
                endDate = makeTime(2026, Calendar.JULY, 15, 23, 59, 59),
                invoicePrefix = "INV",
                currentInvoiceSequence = 0L,
                isCurrent = false,
                isClosed = false
            ),
            FiscalYearEntity(
                id = "fy_${orgId}_2083_84",
                orgId = orgId,
                name = "2083/84",
                code = "83/84",
                startDate = makeTime(2026, Calendar.JULY, 16, 0, 0, 0),
                endDate = makeTime(2027, Calendar.JULY, 15, 23, 59, 59),
                invoicePrefix = "INV",
                currentInvoiceSequence = 0L,
                isCurrent = false,
                isClosed = false
            )
        )

        // Find which FY encompasses current time, or default to 2081/82 or 2082/83
        val activeMatch = list.firstOrNull { currentTime in it.startDate..it.endDate } ?: list[1]
        return list.map { fy ->
            fy.copy(isCurrent = fy.id == activeMatch.id)
        }
    }

    fun formatDateRange(startDate: Long, endDate: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return "${sdf.format(Date(startDate))} to ${sdf.format(Date(endDate))}"
    }

    data class FiscalQuarter(
        val quarterCode: String,
        val labelEn: String,
        val labelNe: String,
        val startDate: Long,
        val endDate: Long
    )

    fun getQuarterDateRanges(fy: FiscalYearEntity): List<FiscalQuarter> {
        val duration = (fy.endDate - fy.startDate).coerceAtLeast(4L)
        val qDuration = duration / 4
        return listOf(
            FiscalQuarter(
                quarterCode = "Q1",
                labelEn = "Shrawan-Ashwin",
                labelNe = "साउन-असोज",
                startDate = fy.startDate,
                endDate = fy.startDate + qDuration
            ),
            FiscalQuarter(
                quarterCode = "Q2",
                labelEn = "Kartik-Poush",
                labelNe = "कात्तिक-पुस",
                startDate = fy.startDate + qDuration + 1,
                endDate = fy.startDate + (2 * qDuration)
            ),
            FiscalQuarter(
                quarterCode = "Q3",
                labelEn = "Magh-Chaitra",
                labelNe = "माघ-चैत",
                startDate = fy.startDate + (2 * qDuration) + 1,
                endDate = fy.startDate + (3 * qDuration)
            ),
            FiscalQuarter(
                quarterCode = "Q4",
                labelEn = "Baisakh-Ashadh",
                labelNe = "वैशाख-असार",
                startDate = fy.startDate + (3 * qDuration) + 1,
                endDate = fy.endDate
            )
        )
    }

    /**
     * Determines which quarter of the Nepali fiscal year a timestamp belongs to.
     * Q1: Shrawan - Ashwin (approx. Jul - Oct)
     * Q2: Kartik - Poush (approx. Oct - Jan)
     * Q3: Magh - Chaitra (approx. Jan - Apr)
     * Q4: Baisakh - Ashadh (approx. Apr - Jul)
     */
    fun getQuarterLabel(timestamp: Long, fy: FiscalYearEntity): String {
        val duration = (fy.endDate - fy.startDate).coerceAtLeast(1L)
        val offset = (timestamp - fy.startDate).coerceAtLeast(0L)
        val quarterIndex = (offset * 4 / duration).toInt().coerceIn(0, 3)
        return when (quarterIndex) {
            0 -> "Q1 (Shrawan - Ashwin)"
            1 -> "Q2 (Kartik - Poush)"
            2 -> "Q3 (Magh - Chaitra)"
            else -> "Q4 (Baisakh - Ashadh)"
        }
    }
}
