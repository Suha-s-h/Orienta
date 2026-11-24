package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules ORDER BY updatedAt DESC")
    fun observeAllRules(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleForPackage(packageName: String): AppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: AppRuleEntity)

    @Query("UPDATE app_rules SET isEnabled = :enabled, updatedAt = :updatedAt WHERE packageName = :packageName")
    suspend fun setRuleEnabled(packageName: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("DELETE FROM app_rules")
    suspend fun clearAllRules()
}
