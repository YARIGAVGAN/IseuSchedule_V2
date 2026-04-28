package com.example.scheduleiseu.domain.core.repository

import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.SemesterPerformance
import com.example.scheduleiseu.domain.core.model.SemesterReference
import kotlinx.coroutines.flow.Flow

interface PerformanceRepository {
    fun observeCachedLatestSemesterPerformance(): Flow<SemesterPerformance?>
    fun observeCachedAllSemesterPerformance(): Flow<List<SemesterPerformance>>
    fun observeCachedSemesterPerformance(semesterId: String, semesterTitle: String): Flow<SemesterPerformance?>

    suspend fun refreshAvailableSemesters(session: AuthSession): List<SemesterReference>
    suspend fun refreshLatestSemesterPerformance(session: AuthSession): SemesterPerformance
    suspend fun refreshSemesterPerformance(
        session: AuthSession,
        semesterId: String,
        semesterTitle: String
    ): SemesterPerformance
}
