package com.example.pos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pos.data.model.TaxMode
import kotlinx.coroutines.flow.Flow

/**
 * Organization DAO providing CRUD operations strictly scoped by org_id for multi-tenant data isolation.
 */
@Dao
interface OrganizationDao {

    // --- Create ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrg(org: OrganizationEntity)

    // --- Read (strictly scoped by org_id) ---
    @Query("SELECT * FROM organizations WHERE org_id = :orgId LIMIT 1")
    fun getOrganization(orgId: String): Flow<OrganizationEntity?>

    @Query("SELECT * FROM organizations WHERE org_id = :orgId LIMIT 1")
    suspend fun getOrgById(orgId: String): OrganizationEntity?

    @Query("SELECT * FROM organizations WHERE org_id = :orgId ORDER BY name ASC")
    fun getOrganizations(orgId: String): Flow<List<OrganizationEntity>>

    // --- Update (strictly scoped by org_id) ---
    @Update
    suspend fun updateOrg(org: OrganizationEntity)

    @Query("UPDATE organizations SET name = :name, tax_id = :taxId, currency_symbol = :currencySymbol, tax_mode = :taxMode WHERE org_id = :orgId")
    suspend fun updateOrgDetails(
        orgId: String,
        name: String,
        taxId: String,
        currencySymbol: String,
        taxMode: TaxMode
    )

    // --- Delete (strictly scoped by org_id) ---
    @Query("DELETE FROM organizations WHERE org_id = :orgId")
    suspend fun deleteOrgById(orgId: String)
}
