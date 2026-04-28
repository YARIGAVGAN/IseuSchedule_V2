package com.example.scheduleiseu.data.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_performance")
data class CachedPerformanceEntity(
    @PrimaryKey val cacheKey: String,
    val semesterId: String?,
    val semesterTitle: String,
    val averageScore: String?,
    val subjectsJson: String,
    val semestersJson: String,
    val cachedAtMillis: Long
)

@Dao
interface PerformanceCacheDao {
    @Query("SELECT * FROM cached_performance WHERE cacheKey = :cacheKey LIMIT 1")
    fun observeByKey(cacheKey: String): Flow<CachedPerformanceEntity?>

    @Query("SELECT * FROM cached_performance WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByKey(cacheKey: String): CachedPerformanceEntity?

    @Query("SELECT * FROM cached_performance")
    fun observeAll(): Flow<List<CachedPerformanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedPerformanceEntity)
}
