package com.example.scheduleiseu.domain.core.usecase

import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.UserPhoto
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.repository.ActiveSessionRepository
import com.example.scheduleiseu.domain.core.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

class ObserveStudentProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(): Flow<StudentProfile?> = profileRepository.observeCachedStudentProfile()
}

class ObserveTeacherProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(): Flow<TeacherProfile?> = profileRepository.observeCachedTeacherProfile()
}

class ObserveCachedUserPhotoUseCase(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(role: UserRole): Flow<UserPhoto?> {
        return profileRepository.observeCachedUserPhoto(role)
    }
}

class RefreshStudentProfileUseCase(
    private val profileRepository: ProfileRepository,
    private val activeSessionRepository: ActiveSessionRepository
) {
    suspend operator fun invoke(): StudentProfile {
        val session = activeSessionRepository.getActiveSession()
            ?: throw IllegalStateException("Нет активной сессии")
        return profileRepository.refreshStudentProfile(session)
    }
}

class RefreshTeacherProfileUseCase(
    private val profileRepository: ProfileRepository,
    private val activeSessionRepository: ActiveSessionRepository
) {
    suspend operator fun invoke(): TeacherProfile {
        val session = activeSessionRepository.getActiveSession()
            ?: throw IllegalStateException("Нет активной сессии")
        return profileRepository.refreshTeacherProfile(session)
    }
}

class RefreshUserPhotoUseCase(
    private val profileRepository: ProfileRepository,
    private val activeSessionRepository: ActiveSessionRepository
) {
    suspend operator fun invoke(): UserPhoto? {
        val session = activeSessionRepository.getActiveSession()
            ?: throw IllegalStateException("Нет активной сессии")
        return profileRepository.refreshUserPhoto(session)
    }
}

class GetActiveUserRoleUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val savedUserRoleProvider: (() -> UserRole?)? = null
) {
    operator fun invoke(): UserRole? {
        return activeSessionRepository.getActiveSession()?.userRole
            ?: savedUserRoleProvider?.invoke()
    }
}
