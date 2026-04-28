package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.local.cache.PerformanceCacheCodec
import com.example.scheduleiseu.data.local.db.CachedPerformanceEntity
import com.example.scheduleiseu.data.local.db.PerformanceCacheDao
import com.example.scheduleiseu.data.mapper.contract.ProgressTableResultToSemesterPerformanceMapper
import com.example.scheduleiseu.data.mapper.contract.SemesterLinkToSemesterReferenceMapper
import com.example.scheduleiseu.data.remote.datasource.PerformanceRemoteDataSource
import com.example.scheduleiseu.data.remote.model.SemesterLink
import com.example.scheduleiseu.data.session.AuthSessionStore
import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.SemesterPerformance
import com.example.scheduleiseu.domain.core.model.SemesterReference
import com.example.scheduleiseu.domain.core.repository.PerformanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PerformanceRepositoryImpl(
    private val remoteDataSource: PerformanceRemoteDataSource,
    private val authSessionStore: AuthSessionStore,
    private val semesterMapper: SemesterLinkToSemesterReferenceMapper,
    private val performanceMapper: ProgressTableResultToSemesterPerformanceMapper,
    private val performanceCacheDao: PerformanceCacheDao? = null
) : PerformanceRepository {

    private val performanceRefreshMutex = Mutex()

    override fun observeCachedLatestSemesterPerformance(): Flow<SemesterPerformance?> {
        return performanceCacheDao
            ?.observeAll()
            ?.map { cached -> cached.latestAcademicSemester()?.let(PerformanceCacheCodec::fromEntity) }
            ?: flowOf(null)
    }

    override fun observeCachedAllSemesterPerformance(): Flow<List<SemesterPerformance>> {
        return performanceCacheDao
            ?.observeAll()
            ?.map { cached ->
                cached.sortedCachedPerformanceAcademically()
                    .map(PerformanceCacheCodec::fromEntity)
            }
            ?: flowOf(emptyList())
    }

    override fun observeCachedSemesterPerformance(
        semesterId: String,
        semesterTitle: String
    ): Flow<SemesterPerformance?> {
        val cacheKey = semesterCacheKey(semesterId = semesterId, semesterTitle = semesterTitle)
        return performanceCacheDao
            ?.observeByKey(cacheKey)
            ?.map { cached -> cached?.let(PerformanceCacheCodec::fromEntity) }
            ?: flowOf(null)
    }

    override suspend fun refreshAvailableSemesters(session: AuthSession): List<SemesterReference> =
        performanceRefreshMutex.withLock {
            ensureAuthenticatedSession(session)
            remoteDataSource.getAvailableSemesters()
                .sortedSemesterLinksAcademically()
                .map(semesterMapper::map)
        }

    override suspend fun refreshLatestSemesterPerformance(session: AuthSession): SemesterPerformance =
        performanceRefreshMutex.withLock {
            ensureAuthenticatedSession(session)

            val semesters = remoteDataSource.getAvailableSemesters()
                .sortedSemesterLinksAcademically()

            val latestSemester = semesters.lastOrNull()
            val remoteResult = if (latestSemester != null) {
                remoteDataSource.getSemesterProgress(latestSemester)
            } else {
                remoteDataSource.getLatestSemesterProgress()
            }

            val remotePerformance = performanceMapper.map(remoteResult)
            val performance = remotePerformance.copy(
                semesterId = latestSemester?.eventTarget,
                semesterTitle = latestSemester?.title ?: remotePerformance.semesterTitle,
                availableSemesters = semesters.map(semesterMapper::map)
            )

            cachePerformance(performance)
            performance
        }

    override suspend fun refreshSemesterPerformance(
        session: AuthSession,
        semesterId: String,
        semesterTitle: String
    ): SemesterPerformance = performanceRefreshMutex.withLock {
        ensureAuthenticatedSession(session)

        val semesters = remoteDataSource.getAvailableSemesters()
            .sortedSemesterLinksAcademically()

        val selectedSemester = semesters.findSelectedSemester(
            semesterId = semesterId,
            semesterTitle = semesterTitle
        ) ?: throw IllegalArgumentException("Семестр не найден: $semesterTitle")

        val performance = performanceMapper.map(remoteDataSource.getSemesterProgress(selectedSemester))
            .copy(
                semesterId = selectedSemester.eventTarget,
                semesterTitle = selectedSemester.title,
                availableSemesters = semesters.map(semesterMapper::map)
            )

        cachePerformance(performance)
        performance
    }

    private suspend fun cachePerformance(performance: SemesterPerformance) {
        performanceCacheDao?.upsert(
            PerformanceCacheCodec.toEntity(
                cacheKey = semesterCacheKey(performance.semesterId, performance.semesterTitle),
                performance = performance
            )
        )
    }

    private fun semesterCacheKey(semesterId: String?, semesterTitle: String): String {
        return semesterId?.takeIf { it.isNotBlank() } ?: semesterTitle
    }

    private fun List<SemesterLink>.findSelectedSemester(
        semesterId: String,
        semesterTitle: String
    ): SemesterLink? {
        return firstOrNull { it.eventTarget == semesterId }
            ?: firstOrNull { it.title == semesterTitle }
    }

    private fun List<SemesterLink>.sortedSemesterLinksAcademically(): List<SemesterLink> {
        return sortedWith(
            compareBy<SemesterLink> { it.title.extractCourseNumber() ?: Int.MAX_VALUE }
                .thenBy { it.title.extractSessionOrder() }
                .thenBy { it.title }
        )
    }

    private fun List<CachedPerformanceEntity>.latestAcademicSemester(): CachedPerformanceEntity? {
        return sortedCachedPerformanceAcademically().lastOrNull()
    }

    private fun List<CachedPerformanceEntity>.sortedCachedPerformanceAcademically(): List<CachedPerformanceEntity> {
        return sortedWith(
            compareBy<CachedPerformanceEntity> { it.semesterTitle.extractCourseNumber() ?: Int.MAX_VALUE }
                .thenBy { it.semesterTitle.extractSessionOrder() }
                .thenBy { it.semesterTitle }
        )
    }

    private fun String.extractCourseNumber(): Int? {
        return Regex("""(\d+)\s*курс""", RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private fun String.extractSessionOrder(): Int {
        val normalized = lowercase()
        return when {
            "зим" in normalized -> 0
            "вес" in normalized -> 1
            "лет" in normalized -> 2
            "осен" in normalized -> 3
            else -> 99
        }
    }

    private fun ensureAuthenticatedSession(session: AuthSession) {
        val storedSession = authSessionStore.getSession()
            ?: throw IllegalStateException("Сессия личного кабинета не активна")

        if (!session.isAuthenticated || !storedSession.isAuthenticated) {
            throw IllegalStateException("Требуется авторизация в личном кабинете")
        }

        if (storedSession.sessionKey != session.sessionKey) {
            throw IllegalStateException("Сессия личного кабинета устарела")
        }
    }
}