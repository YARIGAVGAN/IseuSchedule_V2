package com.example.scheduleiseu.domain.core.usecase

import com.example.scheduleiseu.domain.core.model.SemesterPerformance
import com.example.scheduleiseu.domain.core.repository.ActiveSessionRepository
import com.example.scheduleiseu.domain.core.repository.PerformanceRepository
import kotlinx.coroutines.flow.Flow

class ObserveLatestPerformanceUseCase(
    private val performanceRepository: PerformanceRepository
) {
    operator fun invoke(): Flow<SemesterPerformance?> {
        return performanceRepository.observeCachedLatestSemesterPerformance()
    }
}

class ObserveAllPerformanceUseCase(
    private val performanceRepository: PerformanceRepository
) {
    operator fun invoke(): Flow<List<SemesterPerformance>> {
        return performanceRepository.observeCachedAllSemesterPerformance()
    }
}

class ObserveSemesterPerformanceUseCase(
    private val performanceRepository: PerformanceRepository
) {
    operator fun invoke(semesterId: String, semesterTitle: String): Flow<SemesterPerformance?> {
        return performanceRepository.observeCachedSemesterPerformance(semesterId, semesterTitle)
    }
}

class RefreshLatestPerformanceUseCase(
    private val performanceRepository: PerformanceRepository,
    private val activeSessionRepository: ActiveSessionRepository
) {
    suspend operator fun invoke(): SemesterPerformance {
        val session = activeSessionRepository.getActiveSession()
            ?: throw IllegalStateException("Сессия личного кабинета не активна")
        return performanceRepository.refreshLatestSemesterPerformance(session)
    }
}

class RefreshSemesterPerformanceUseCase(
    private val performanceRepository: PerformanceRepository,
    private val activeSessionRepository: ActiveSessionRepository
) {
    suspend operator fun invoke(semesterId: String, semesterTitle: String): SemesterPerformance {
        val session = activeSessionRepository.getActiveSession()
            ?: throw IllegalStateException("Сессия личного кабинета не активна")
        return performanceRepository.refreshSemesterPerformance(
            session = session,
            semesterId = semesterId,
            semesterTitle = semesterTitle
        )
    }
}
