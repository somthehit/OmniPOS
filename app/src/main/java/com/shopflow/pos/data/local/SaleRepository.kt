package com.shopflow.pos.data.local

import androidx.room.withTransaction
import com.shopflow.pos.data.local.entity.*

data class CompleteSaleCommand(
    val sale: SaleEntity,                    // receiptNumber is ignored and allocated here
    val items: List<SaleItemEntity>,
    val payments: List<PaymentEntity>,
    val payloadJson: String,                 // serialized aggregate to upload (built by the caller)
)

sealed interface CompleteSaleResult {
    data class Recorded(val saleId: String, val receiptNumber: String) : CompleteSaleResult
    /** Retry / double-tap: the sale already exists. Nothing was written twice. */
    data class AlreadyRecorded(val saleId: String, val receiptNumber: String) : CompleteSaleResult
}

/**
 * The offline checkout write path. ONE local transaction:
 *   sale + items + payments + stock movements + outbox entry (+ receipt number allocation).
 * If any step throws, nothing is persisted. Totals/discount/tax are computed by the domain layer
 * BEFORE this call; stock policy (block/warn/allow) is also decided by the domain layer.
 */
class SaleRepository(
    private val db: ShopFlowDatabase,
    private val activeOrganizationId: String,
    private val idGen: () -> String,                         // UUIDv7 generator
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun completeSale(cmd: CompleteSaleCommand): CompleteSaleResult = db.withTransaction {
        val org = activeOrganizationId
        val s = cmd.sale
        require(s.organizationId == org) { "sale belongs to a different organization" }
        require(cmd.items.isNotEmpty()) { "sale has no items" }
        require(cmd.items.all { it.organizationId == org && it.saleId == s.id }) { "item scope mismatch" }
        require(cmd.payments.all { it.organizationId == org && it.saleId == s.id }) { "payment scope mismatch" }

        val saleDao = db.saleDao()
        saleDao.findByIdempotencyKey(org, s.idempotencyKey)?.let {
            return@withTransaction CompleteSaleResult.AlreadyRecorded(it.id, it.receiptNumber)
        }

        // Receipt number: per-register sequence => offline devices never collide.
        val registerDao = db.registerDao()
        val register = registerDao.get(org, s.registerId) ?: error("unknown register")
        registerDao.ensureCounter(ReceiptCounterEntity(org, s.registerId))
        val seq = registerDao.counterValue(org, s.registerId)
        registerDao.bumpCounter(org, s.registerId)
        val receiptNumber = "${register.receiptPrefix}-${seq.toString().padStart(6, '0')}"

        val now = nowMs()
        saleDao.insertSale(s.copy(receiptNumber = receiptNumber, syncState = SyncState.PENDING))
        saleDao.insertItems(cmd.items)
        saleDao.insertPayments(cmd.payments)

        // Stock is a ledger: write negative movements, never overwrite a balance.
        val movements = cmd.items.mapNotNull { item ->
            val product = db.productDao().get(org, item.productId) ?: error("unknown product ${item.productId}")
            if (!product.trackStock) null else StockMovementEntity(
                id = idGen(), organizationId = org, branchId = s.branchId, productId = item.productId,
                movementType = MovementType.SALE, quantityMilli = -item.quantityMilli,
                unitCostMinor = product.costMinor, sourceType = "sale", sourceId = s.id,
                idempotencyKey = "sale:${s.id}:${item.id}", createdAt = now,
                createdBy = s.cashierId, deviceId = s.deviceId,
            )
        }
        db.stockDao().insertMovements(movements)

        db.outboxDao().enqueue(
            SyncOutboxEntity(
                id = idGen(), organizationId = org, entityType = "sale", entityId = s.id,
                operation = "UPSERT_AGGREGATE", payloadJson = cmd.payloadJson,
                idempotencyKey = s.idempotencyKey, createdAt = now, updatedAt = now,
            )
        )
        CompleteSaleResult.Recorded(s.id, receiptNumber)
    }
}
