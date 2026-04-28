package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.domain.core.usecase.FirstEntryBootstrapUseCase

object FirstEntryBootstrapUseCaseFactory {
    fun create(): FirstEntryBootstrapUseCase {
        return FirstEntryBootstrapUseCase(
            activeSessionRepository = ProfileRepositoryFactory.createActiveSessionRepository(),
            bootstrapRepository = BootstrapRepositoryImpl(BsuCabinetDataComponent.preferences),
            profileRepository = ProfileRepositoryFactory.create(),
            studentRegistrationRepository = StudentRegistrationRepositoryFactory.create(),
            scheduleRepository = ScheduleRepositoryFactory.create(),
            performanceRepository = PerformanceRepositoryFactory.create(),
            onOwnScheduleRefreshed = BsuCabinetDataComponent.lessonNotificationScheduler::scheduleNext
        )
    }
}
