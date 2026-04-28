package com.example.scheduleiseu.data.remote.datasource.impl

import com.example.scheduleiseu.data.remote.datasource.PerformanceRemoteDataSource
import com.example.scheduleiseu.data.remote.model.ProgressTableResult
import com.example.scheduleiseu.data.remote.model.SemesterLink
import com.example.scheduleiseu.data.remote.parser.BsuParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PerformanceRemoteDataSourceImpl(
    private val parser: BsuParser
) : PerformanceRemoteDataSource {

    override suspend fun getAvailableSemesters(): List<SemesterLink> = withContext(Dispatchers.IO) {
        parser.getAvailableSemesters()
    }

    override suspend fun getLatestSemesterProgress(): ProgressTableResult = withContext(Dispatchers.IO) {
        val (_, html) = parser.openLatestSemester()
        parser.parseProgressTable(html, BsuParser.STUD_PROGRESS_URL)
    }

    override suspend fun getSemesterProgress(semester: SemesterLink): ProgressTableResult = withContext(Dispatchers.IO) {
        val html = parser.openSemester(semester)
        parser.parseProgressTable(html, BsuParser.STUD_PROGRESS_URL)
    }
}
