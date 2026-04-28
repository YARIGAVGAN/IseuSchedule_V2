package com.example.scheduleiseu.data.remote.datasource

import com.example.scheduleiseu.data.remote.model.ProgressTableResult
import com.example.scheduleiseu.data.remote.model.SemesterLink

interface PerformanceRemoteDataSource {
    suspend fun getAvailableSemesters(): List<SemesterLink>
    suspend fun getLatestSemesterProgress(): ProgressTableResult
    suspend fun getSemesterProgress(semester: SemesterLink): ProgressTableResult
}
