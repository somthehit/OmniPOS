package com.example.pos.domain

import android.util.Log
import com.example.pos.data.local.StoreSettingsEntity
import com.example.pos.data.model.PaymentEntry
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.StaffRole
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Triggers that can request a cash drawer opening.
 */
enum class DrawerTriggerReason {
    CASH_SALE,
    SHIFT_OPEN_COUNT,
    PAID_IN,
    PAID_OUT,
    SAFE_DROP,
    MANUAL_ADMIN_TEST,
    SUPERVISOR_OVERRIDE
}

sealed interface DrawerKickResult {
    data class Success(val message: String) : DrawerKickResult
    data class Blocked(val reason: String) : DrawerKickResult
    data class Error(val reason: String) : DrawerKickResult
}

/**
 * Enterprise Cash Drawer Kick Controller.
 * Ensures:
 * 1. Only opens on eligible cash transactions or authorized manual supervisor/admin actions.
 * 2. Never opens for 100% online/card transactions.
 * 3. Enforces a 1.5-second cooldown and transaction ID deduplication to prevent duplicate kick pulses.
 * 4. Checks store settings and printer hardware compatibility.
 */
class CashDrawerManager(
    private val printerManager: ThermalPrinterManager
) {
    private val tag = "CashDrawerManager"
    private val lastKickTimestamp = AtomicLong(0L)
    private val lastTransactionId = AtomicReference<String?>(null)

    companion object {
        const val COOLDOWN_MS = 1500L
    }

    /**
     * Checks if a set of payment tenders contains any cash.
     */
    fun hasCashTender(payments: List<PaymentEntry>): Boolean {
        return payments.any { it.type == PaymentType.CASH && it.amountInCents > 0L }
    }

    fun hasCashTenderEntities(payments: List<com.example.pos.data.local.SalePaymentEntity>): Boolean {
        return payments.any { it.paymentType == PaymentType.CASH && it.amountInCents > 0L }
    }

    /**
     * Evaluates whether a role is authorized for the given trigger reason.
     */
    fun isAuthorized(role: StaffRole, reason: DrawerTriggerReason): Boolean {
        return when (reason) {
            DrawerTriggerReason.CASH_SALE,
            DrawerTriggerReason.SHIFT_OPEN_COUNT,
            DrawerTriggerReason.PAID_IN,
            DrawerTriggerReason.PAID_OUT,
            DrawerTriggerReason.SAFE_DROP -> true // Any active staff (Cashier, Manager, Admin, Owner) during operational flow

            DrawerTriggerReason.MANUAL_ADMIN_TEST -> {
                role == StaffRole.OWNER || role == StaffRole.ADMIN || role == StaffRole.MANAGER
            }

            DrawerTriggerReason.SUPERVISOR_OVERRIDE -> {
                role == StaffRole.OWNER || role == StaffRole.ADMIN || role == StaffRole.MANAGER
            }
        }
    }

    /**
     * Attempts to trigger the cash drawer pulse via the connected ESC/POS thermal printer.
     */
    suspend fun requestDrawerKick(
        reason: DrawerTriggerReason,
        role: StaffRole,
        settings: StoreSettingsEntity?,
        transactionId: String? = null,
        pin: Int = 0,
        onAuditLog: ((action: String, details: String) -> Unit)? = null
    ): DrawerKickResult {
        // 1. Check Store Settings: is cash drawer trigger enabled?
        if (settings?.cashDrawerTrigger == false) {
            val msg = "Cash drawer trigger is disabled in store settings."
            Log.d(tag, msg)
            return DrawerKickResult.Blocked(msg)
        }

        // 2. Check Role Authorization
        if (!isAuthorized(role, reason)) {
            val msg = "Role ${role.name} is not authorized for manual drawer kick ($reason)."
            Log.w(tag, msg)
            onAuditLog?.invoke("DRAWER_KICK_DENIED", msg)
            return DrawerKickResult.Blocked(msg)
        }

        // 3. Deduplication and Debounce Check
        val now = System.currentTimeMillis()
        val lastTime = lastKickTimestamp.get()
        if (now - lastTime < COOLDOWN_MS) {
            val msg = "Drawer kick ignored: Cooldown active (${now - lastTime}ms < ${COOLDOWN_MS}ms)."
            Log.d(tag, msg)
            return DrawerKickResult.Blocked(msg)
        }

        if (transactionId != null && transactionId == lastTransactionId.get() && now - lastTime < 10000L) {
            val msg = "Drawer kick ignored: Duplicate transaction ID '$transactionId'."
            Log.d(tag, msg)
            return DrawerKickResult.Blocked(msg)
        }

        // 4. Check Printer Connection
        val targetAddress = settings?.printerName
        if (targetAddress.isNullOrBlank()) {
            val msg = "No configured thermal printer found to send cash drawer pulse."
            Log.w(tag, msg)
            return DrawerKickResult.Error(msg)
        }

        // 5. Generate and Send ESC/POS Drawer Kick Command
        return try {
            val commandBytes = EscPosCommandBuilder().openCashDrawer(pin = pin).toBytes()
            val printResult = printerManager.print(commandBytes, targetAddress)

            when (printResult) {
                is PrintResult.Success -> {
                    lastKickTimestamp.set(now)
                    if (transactionId != null) {
                        lastTransactionId.set(transactionId)
                    }
                    val detail = "Drawer pulse sent (Pin $pin, Trigger: $reason, Tx: ${transactionId ?: "Manual"})"
                    Log.i(tag, detail)
                    onAuditLog?.invoke("CASH_DRAWER_OPENED", detail)
                    DrawerKickResult.Success("Cash drawer opened successfully.")
                }
                is PrintResult.DeviceNotFound -> {
                    val msg = "Printer not found or unreachable ($targetAddress)."
                    Log.w(tag, msg)
                    DrawerKickResult.Error(msg)
                }
                is PrintResult.PermissionDenied -> {
                    val msg = "Bluetooth permission required to trigger drawer."
                    Log.w(tag, msg)
                    DrawerKickResult.Error(msg)
                }
                is PrintResult.Error -> {
                    val msg = "Drawer trigger failed: ${printResult.reason}"
                    Log.e(tag, msg)
                    DrawerKickResult.Error(msg)
                }
            }
        } catch (e: Exception) {
            val msg = "Unexpected error while pulsing drawer: ${e.message}"
            Log.e(tag, msg, e)
            DrawerKickResult.Error(msg)
        }
    }
}
