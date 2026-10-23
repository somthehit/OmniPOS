package com.example.pos.domain

import com.example.pos.data.model.CartItem
import com.example.pos.data.model.CartSummary
import com.example.pos.data.model.PaymentEntry
import com.example.pos.data.model.TaxMode
import java.util.Locale

object CartCalculator {

    fun calculateSummary(
        items: List<CartItem>,
        orderDiscountPercent: Double = 0.0,
        taxMode: TaxMode = TaxMode.EXCLUSIVE,
    ): CartSummary {
        if (items.isEmpty()) {
            return CartSummary(0L, 0L, 0L, 0L)
        }

        var rawSubtotal = 0L
        var lineDiscounts = 0L
        var lineTaxes = 0L

        for (item in items) {
            val itemRaw = item.subtotalInCents
            val itemDisc = item.discountInCents
            val itemTax = item.taxInCents
            rawSubtotal += itemRaw
            lineDiscounts += itemDisc
            lineTaxes += itemTax
        }

        val netAfterLineDiscounts = rawSubtotal - lineDiscounts
        val orderDiscountInCents = ((netAfterLineDiscounts * orderDiscountPercent) / 100.0).toLong()
        val taxableSubtotal = (netAfterLineDiscounts - orderDiscountInCents).coerceAtLeast(0L)

        val finalTax = if (taxMode == TaxMode.INCLUSIVE) {
            // Tax is already embedded in selling price
            lineTaxes
        } else {
            lineTaxes
        }

        val grandTotal = if (taxMode == TaxMode.INCLUSIVE) {
            taxableSubtotal
        } else {
            taxableSubtotal + finalTax
        }

        return CartSummary(
            subtotalInCents = rawSubtotal,
            orderDiscountInCents = lineDiscounts + orderDiscountInCents,
            taxTotalInCents = finalTax,
            grandTotalInCents = grandTotal
        )
    }

    fun calculateChange(tenderedInCents: Long, totalInCents: Long): Long {
        return (tenderedInCents - totalInCents).coerceAtLeast(0L)
    }

    fun calculateRemainingDue(totalInCents: Long, payments: List<PaymentEntry>): Long {
        val paidTotal = payments.sumOf { it.amountInCents }
        return (totalInCents - paidTotal).coerceAtLeast(0L)
    }

    fun formatSouthAsianInteger(number: Long): String {
        val s = kotlin.math.abs(number).toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val rest = s.dropLast(3)
        val groupedRest = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$groupedRest,$last3"
    }

    fun formatMoney(amountInCents: Long, currencySymbol: String = "$"): String {
        val sign = if (amountInCents < 0) "-" else ""
        val absCents = kotlin.math.abs(amountInCents)
        val wholeUnits = absCents / 100
        val cents = absCents % 100
        val isSouthAsian = currencySymbol.contains("Rs", ignoreCase = true) ||
                currencySymbol.contains("NPR", ignoreCase = true) ||
                currencySymbol.contains("रु")
        val formattedUnits = if (isSouthAsian) {
            formatSouthAsianInteger(wholeUnits)
        } else {
            wholeUnits.toString()
        }
        val spacing = if (currencySymbol.length > 1) " " else ""
        return String.format(Locale.US, "%s%s%s%s.%02d", sign, currencySymbol, spacing, formattedUnits, cents)
    }
}
