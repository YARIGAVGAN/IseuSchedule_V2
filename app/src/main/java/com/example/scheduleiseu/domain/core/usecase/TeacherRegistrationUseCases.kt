package com.example.scheduleiseu.domain.core.usecase

import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.TeacherRegistrationContext
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository

class LoadTeacherRegistrationContextUseCase(
    private val repository: TeacherRegistrationRepository
) {
    suspend operator fun invoke(): TeacherRegistrationContext = repository.loadInitialContext()
}

class SaveTeacherProfileUseCase(
    private val repository: TeacherRegistrationRepository
) {
    suspend operator fun invoke(teacherId: String): TeacherProfile = repository.saveTeacherProfile(teacherId)
}
