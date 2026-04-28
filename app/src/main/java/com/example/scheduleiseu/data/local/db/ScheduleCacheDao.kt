package com.example.scheduleiseu.data.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_schedule_weeks")
data class CachedScheduleWeekEntity(
    @PrimaryKey val cacheKey: String,
    val role: String,
    val ownerId: String,
    val weekValue: String,
    val weekTitle: String,
    val isCurrentWeek: Boolean,
    val isNextWeek: Boolean,
    val cachedAtMillis: Long,
    val payloadJson: String
)

@Dao
interface ScheduleCacheDao {
    @Query("SELECT * FROM cached_schedule_weeks WHERE cacheKey = :cacheKey LIMIT 1")
    fun observeByKey(cacheKey: String): Flow<CachedScheduleWeekEntity?>

    @Query("SELECT * FROM cached_schedule_weeks WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByKey(cacheKey: String): CachedScheduleWeekEntity?

    @Query(
        """
        SELECT * FROM cached_schedule_weeks
        WHERE role = :role AND ownerId = :ownerId
        ORDER BY isCurrentWeek DESC, cachedAtMillis DESC
        LIMIT 1
        """
    )
    fun observeCurrentOrLatest(role: String, ownerId: String): Flow<CachedScheduleWeekEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedScheduleWeekEntity)

    @Query("SELECT weekValue FROM cached_schedule_weeks WHERE role = :role AND ownerId = :ownerId")
    fun observeCachedWeekValues(role: String, ownerId: String): Flow<List<String>>

    @Query(
        """
        SELECT * FROM cached_schedule_weeks
        WHERE role = :role AND ownerId = :ownerId
        ORDER BY isCurrentWeek DESC, cachedAtMillis DESC
        """
    )
    fun observeWeeksForOwner(role: String, ownerId: String): Flow<List<CachedScheduleWeekEntity>>

    @Query(
        """
        SELECT * FROM cached_schedule_weeks
        WHERE role = :role AND ownerId = :ownerId
        ORDER BY isCurrentWeek DESC, isNextWeek DESC, cachedAtMillis DESC
        """
    )
    suspend fun getWeeksForOwner(role: String, ownerId: String): List<CachedScheduleWeekEntity>

    @Query("DELETE FROM cached_schedule_weeks WHERE role = :role AND ownerId = :ownerId AND weekValue NOT IN (:allowedWeekValues)")
    suspend fun deleteWeeksOutsidePolicy(role: String, ownerId: String, allowedWeekValues: List<String>)

    @Query("DELETE FROM cached_schedule_weeks WHERE role = :role AND ownerId = :ownerId")
    suspend fun deleteAllWeeksForOwner(role: String, ownerId: String)

    @Query("DELETE FROM cached_schedule_weeks WHERE role = :role")
    suspend fun deleteAllWeeksForRole(role: String)

}
