package com.example.pos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pos.data.model.StaffRole
import kotlinx.coroutines.flow.Flow

/**
 * User DAO providing CRUD operations strictly scoped by org_id for multi-tenant data isolation.
 */
@Dao
interface UserDao {

    // --- Create ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    // --- Read (strictly scoped by org_id) ---
    @Query("SELECT * FROM users WHERE org_id = :orgId ORDER BY full_name ASC")
    fun getUsers(orgId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE org_id = :orgId AND is_active = 1 ORDER BY full_name ASC")
    fun getActiveUsers(orgId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE org_id = :orgId AND id = :userId LIMIT 1")
    suspend fun getUserById(orgId: String, userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE org_id = :orgId AND email = :email LIMIT 1")
    suspend fun getUserByEmail(orgId: String, email: String): UserEntity?

    // --- Update (strictly scoped by org_id) ---
    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET full_name = :fullName, role = :role, is_active = :isActive, updated_at = :updatedAt WHERE org_id = :orgId AND id = :userId")
    suspend fun updateUserDetails(
        orgId: String,
        userId: String,
        fullName: String,
        role: StaffRole,
        isActive: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE users SET pin_hash = :pinHash, updated_at = :updatedAt WHERE org_id = :orgId AND id = :userId")
    suspend fun updateUserPin(
        orgId: String,
        userId: String,
        pinHash: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    // --- Delete (strictly scoped by org_id) ---
    @Query("DELETE FROM users WHERE org_id = :orgId AND id = :userId")
    suspend fun deleteUser(orgId: String, userId: String)

    @Query("DELETE FROM users WHERE org_id = :orgId")
    suspend fun deleteAllUsers(orgId: String)
}
