package com.example.scheduleiseu.feature.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.scheduleiseu.data.repository.core.PerformanceRepositoryFactory
import com.example.scheduleiseu.domain.core.repository.ActiveSessionRepository
import com.example.scheduleiseu.domain.core.repository.PerformanceRepository
import com.example.scheduleiseu.domain.core.usecase.ObserveAllPerformanceUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveLatestPerformanceUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveSemesterPerformanceUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshLatestPerformanceUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshSemesterPerformanceUseCase

class PerformanceViewModelFactory(
    private val performanceRepository: PerformanceRepository = PerformanceRepositoryFactory.create(),
    private val activeSessionRepository: ActiveSessionRepository = PerformanceRepositoryFactory.createActiveSessionRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PerformanceViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return PerformanceViewModel(
            observeLatestPerformance = ObserveLatestPerformanceUseCase(performanceRepository),
            observeAllPerformance = ObserveAllPerformanceUseCase(performanceRepository),
            observeSemesterPerformance = ObserveSemesterPerformanceUseCase(performanceRepository),
            refreshLatestPerformance = RefreshLatestPerformanceUseCase(
                performanceRepository = performanceRepository,
                activeSessionRepository = activeSessionRepository
            ),
            refreshSemesterPerformance = RefreshSemesterPerformanceUseCase(
                performanceRepository = performanceRepository,
                activeSessionRepository = activeSessionRepository
            )
        ) as T
    }
}
