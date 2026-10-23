package com.example.pos.data.model

import java.util.UUID

enum class StaffRole {
    OWNER,
    ADMIN,
    MANAGER,
    STOREKEEPER,
    CASHIER
}

enum class PaymentType {
    CASH,
    CARD,
    QR_WALLET,
    CREDIT_DUE
}

enum class MovementType {
    SALE,
    RETURN,
    RECEIPT,
    ADJUSTMENT
}

enum class TaxMode {
    EXCLUSIVE,
    INCLUSIVE
}

enum class OutboxOp {
    INSERT,
    UPDATE,
    DELETE
}

data class CartItem(
    val product: com.example.pos.data.local.ProductEntity,
    val quantity: Int = 1,
    val discountPercent: Double = 0.0,
    val notes: String = ""
) {
    val unitPriceInCents: Long get() = product.sellingPriceInCents
    val subtotalInCents: Long get() = unitPriceInCents * quantity
    val discountInCents: Long get() = ((subtotalInCents * discountPercent) / 100.0).toLong()
    val discountedSubtotalInCents: Long get() = subtotalInCents - discountInCents
    val taxInCents: Long get() = ((discountedSubtotalInCents * product.taxRatePercent) / 100.0).toLong()
    val totalInCents: Long get() = discountedSubtotalInCents + taxInCents
}

data class CartSummary(
    val subtotalInCents: Long,
    val orderDiscountInCents: Long,
    val taxTotalInCents: Long,
    val grandTotalInCents: Long
)

data class PaymentEntry(
    val type: PaymentType,
    val amountInCents: Long,
    val referenceNo: String = "",
    val changeGivenInCents: Long = 0L
)

data class ZReport(
    val shiftId: String,
    val cashierName: String,
    val openedAt: Long,
    val closedAt: Long,
    val openingFloatInCents: Long,
    val totalSalesCount: Int,
    val totalSalesAmountInCents: Long,
    val cashSalesInCents: Long,
    val cardSalesInCents: Long,
    val qrSalesInCents: Long,
    val creditSalesInCents: Long,
    val payInInCents: Long,
    val payOutInCents: Long,
    val expectedCashInCents: Long,
    val countedCashInCents: Long,
    val cashVarianceInCents: Long,
    val expectedOnlineInCents: Long = cardSalesInCents + qrSalesInCents,
    val countedOnlineInCents: Long = 0L,
    val onlineVarianceInCents: Long = 0L,
    val onlineBillsCount: Int = 0,
    val safeDropInCents: Long = 0L,
    val creditCollectionInCents: Long = 0L,
    val closingFloatInCents: Long = 0L,
    val depositAmountInCents: Long = 0L,
    val openingGapInCents: Long = 0L,
    val varianceReason: String = "",
    val status: String = "APPROVED"
)

data class ShiftLiveMetrics(
    val shiftId: String,
    val cashierName: String,
    val openedAt: Long,
    val openingFloatInCents: Long,
    val salesCount: Int,
    val salesTotalInCents: Long,
    val cashSalesInCents: Long,
    val cardSalesInCents: Long,
    val qrSalesInCents: Long,
    val creditSalesInCents: Long,
    val expectedOnlineInCents: Long,
    val payInInCents: Long,
    val payOutInCents: Long,
    val expectedCashInCents: Long,
    val safeDropInCents: Long = 0L,
    val creditCollectionInCents: Long = 0L,
    val isStale: Boolean = false
)
