package com.omnipos.shift

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

// ---------- Requests (cashier). Counts only: the SERVER computes every total and variance. ----------

@Serializable data class CountDto(val denomination: Int, val count: Int)

@Serializable
data class OpenShiftRequest(
    val shiftId: String,
    val tenantId: String,
    val branchId: String,
    val terminalId: String,
    val deviceId: String,
    val cashierId: String,
    val businessDate: String,
    val openedAt: Long,
    val counts: List<CountDto>,
    val openingGapReason: String?,
)

@Serializable
data class MovementRequest(
    val id: String,
    val shiftId: String,
    val type: String,
    val amount: Long,
    val reason: String,
    val createdAt: Long,
)

@Serializable
data class DeclarationDto(
    val method: String,
    val declaredAmount: Long?,
    val reference: String?,
    val verification: String,
    val reasonCode: String?,
    val note: String?,
)

@Serializable
data class CreditNoteDto(
    val issuedCount: Int,
    val issuedTotal: Long,
    val cashRefund: Long,
    val customerBalance: Long,
    val bankRefund: Long,
    val redeemedAsTender: Long,
)

@Serializable
data class CloseShiftRequest(
    val shiftId: String,
    val closedAt: Long,
    val counts: List<CountDto>,
    val closingFloat: Long,
    val declarations: List<DeclarationDto>,
    val creditNote: CreditNoteDto,
    val varianceReason: String?,
)

// ---------- Admin DTOs (values below are computed by the server) ----------

@Serializable
data class AdminShiftRow(
    val id: String,
    val cashierName: String,
    val terminalId: String,
    val branchId: String,
    val status: String,
    val openedAt: Long,
    val closedAt: Long? = null,
    val salesSoFar: Long = 0,
    val unsyncedCount: Int = 0,
    val flags: List<String> = emptyList(),
)

@Serializable data class OnlineVarianceDto(
    val method: String,
    val posAmount: Long,
    val confirmedAmount: Long,
    val variance: Long,
    val verification: String,
    val reasonCode: String? = null,
    val reference: String? = null,
)

@Serializable data class MovementDto(
    val type: String,
    val amount: Long,
    val reason: String,
    val createdAt: Long,
)

@Serializable
data class AdminShiftDetail(
    val id: String,
    val cashierName: String,
    val terminalId: String,
    val status: String,
    val openedAt: Long,
    val closedAt: Long? = null,
    val closedBy: String? = null,
    val openingCash: Long,
    val openingExpected: Long? = null,
    val openingGap: Long = 0,
    val expectedCash: Long? = null,
    val countedCash: Long? = null,
    val cashVariance: Long? = null,
    val netCashChange: Long? = null,
    val closingFloat: Long? = null,
    val depositAmount: Long? = null,
    val openingCounts: List<CountDto> = emptyList(),
    val closingCounts: List<CountDto> = emptyList(),
    val online: List<OnlineVarianceDto> = emptyList(),
    val creditNote: CreditNoteDto? = null,
    val movements: List<MovementDto> = emptyList(),
    val cashierReason: String? = null,
    val flags: List<String> = emptyList(),
    val reviewNote: String? = null,
)

@Serializable data class ReviewRequest(val decision: String, val note: String)           // APPROVE | FLAG
@Serializable data class ForceCloseRequest(val counts: List<CountDto>, val closingFloat: Long, val reason: String)
@Serializable data class ReopenRequest(val reason: String)

// ---------- Retrofit ----------

interface ShiftApi {
    /** Used by the outbox worker so every queued write goes through one code path. */
    @POST
    suspend fun postRaw(
        @Url path: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("admin/shifts")
    suspend fun adminShifts(
        @Query("status") status: String? = null,
        @Query("branch") branch: String? = null,
        @Query("cashier") cashier: String? = null,
        @Query("date") date: String? = null,
        @Query("notClosedFromYesterday") notClosedFromYesterday: Boolean? = null,
    ): List<AdminShiftRow>

    @GET("admin/shifts/{id}")
    suspend fun adminShift(@Path("id") id: String): AdminShiftDetail

    @POST("admin/shifts/{id}/review")
    suspend fun review(@Path("id") id: String, @Header("Idempotency-Key") key: String, @Body body: ReviewRequest)

    @POST("admin/shifts/{id}/force-close")
    suspend fun forceClose(@Path("id") id: String, @Header("Idempotency-Key") key: String, @Body body: ForceCloseRequest)

    @POST("admin/shifts/{id}/reopen")
    suspend fun reopen(@Path("id") id: String, @Header("Idempotency-Key") key: String, @Body body: ReopenRequest)
}

// ---------- Outbox sync (offline-first) ----------

/**
 * Construct with your existing DI. Example with a WorkerFactory:
 *   ShiftSyncWorker(ctx, params, shiftDao, shiftApi)
 */
class ShiftSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val dao: ShiftDao,
    private val api: ShiftApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val json = "application/json".toMediaType()
        for (item in dao.pendingOutbox()) {
            try {
                val res = api.postRaw(item.path, item.idempotencyKey, item.payloadJson.toRequestBody(json))
                when {
                    res.isSuccessful -> {
                        dao.deleteOutbox(item.id)
                        dao.markSynced(item.shiftId)
                    }
                    res.code() == 409 -> {
                        // e.g. server already has an active shift for this terminal/cashier.
                        // Never create a second shift: flag it so admin is alerted.
                        dao.markConflict(item.shiftId)
                        dao.deleteOutbox(item.id)
                    }
                    res.code() in 400..499 -> {
                        dao.bumpAttempts(item.id)
                        if (item.attempts >= 5) dao.deleteOutbox(item.id)
                    }
                    else -> return Result.retry()
                }
            } catch (e: IOException) {
                return Result.retry()
            }
        }
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val req = OneTimeWorkRequestBuilder<ShiftSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("shift-outbox-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, req)
        }
    }
}
