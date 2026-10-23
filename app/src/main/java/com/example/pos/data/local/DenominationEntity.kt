package com.example.pos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONObject

/**
 * TypeConverter to serialize Map<Int, Int> to/from JSON in SQLite.
 * Uses Android's built-in org.json.JSONObject for high performance without external dependencies.
 */
class DenominationMapConverter {
    @TypeConverter
    fun fromMap(map: Map<Int, Int>?): String {
        if (map == null || map.isEmpty()) return "{}"
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k.toString(), v) }
        return obj.toString()
    }

    @TypeConverter
    fun toMap(json: String?): Map<Int, Int> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<Int, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val denom = key.toIntOrNull()
                if (denom != null) {
                    result[denom] = obj.optInt(key, 0)
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

/**
 * Room entity representing an offline-first shift cash denomination record with sync status.
 * Scoped by cashierId and status ("OPEN" or "CLOSED") to guarantee independent drawer floats and lifecycles.
 */
@Entity(
    tableName = "shift_denominations",
    indices = [
        Index(value = ["cashierId", "status"])
    ]
)
@TypeConverters(DenominationMapConverter::class)
data class ShiftDenominationEntity(
    @PrimaryKey
    val shiftId: String,          // e.g. "shift_${cashierId}_${System.currentTimeMillis()}"
    val cashierId: String,        // Explicitly binds to the logged-in cashier
    val deviceId: String = "",    // Originating POS terminal / device ID
    val status: String = "OPEN",  // "OPEN" or "CLOSED"
    val denominations: Map<Int, Int>,
    val totalAmount: Long,
    val isSynced: Boolean = false,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
