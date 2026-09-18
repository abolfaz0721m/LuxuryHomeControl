package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AutomationRule
import com.example.data.model.IslandProfile
import com.example.data.model.ManagedApp
import com.example.data.model.SecurityLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Island Profiles ---
    @Query("SELECT * FROM island_profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): Flow<List<IslandProfile>>

    @Query("SELECT * FROM island_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): IslandProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: IslandProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<IslandProfile>)

    @Update
    suspend fun updateProfile(profile: IslandProfile)

    @Delete
    suspend fun deleteProfile(profile: IslandProfile)

    @Query("DELETE FROM island_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)

    // --- Managed Apps ---
    @Query("SELECT * FROM managed_apps WHERE isIslandApp = 1 ORDER BY appName ASC")
    fun getIslandApps(): Flow<List<ManagedApp>>

    @Query("SELECT * FROM managed_apps WHERE isIslandApp = 0 ORDER BY appName ASC")
    fun getPersonalApps(): Flow<List<ManagedApp>>

    @Query("SELECT * FROM managed_apps WHERE islandId = :islandId ORDER BY appName ASC")
    fun getAppsByIsland(islandId: String): Flow<List<ManagedApp>>

    @Query("SELECT * FROM managed_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): ManagedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: ManagedApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<ManagedApp>)

    @Update
    suspend fun updateApp(app: ManagedApp)

    @Delete
    suspend fun deleteApp(app: ManagedApp)

    @Query("UPDATE managed_apps SET isFrozen = :frozen WHERE islandId = :islandId")
    suspend fun setFreezeStateForIsland(islandId: String, frozen: Boolean)

    @Query("UPDATE managed_apps SET isFrozen = :frozen WHERE uniqueId = :uniqueId")
    suspend fun setAppFreeze(uniqueId: String, frozen: Boolean)

    @Query("UPDATE managed_apps SET isHidden = :hidden WHERE uniqueId = :uniqueId")
    suspend fun setAppHidden(uniqueId: String, hidden: Boolean)

    @Query("DELETE FROM managed_apps WHERE islandId = :islandId")
    suspend fun deleteAppsByIsland(islandId: String)

    @Query("DELETE FROM managed_apps")
    suspend fun clearAllApps()

    // --- Automation Rules ---
    @Query("SELECT * FROM automation_rules")
    fun getAllRules(): Flow<List<AutomationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRule)

    @Delete
    suspend fun deleteRule(rule: AutomationRule)

    // --- Security Logs ---
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<SecurityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLog)

    @Query("DELETE FROM security_logs")
    suspend fun clearLogs()
}
