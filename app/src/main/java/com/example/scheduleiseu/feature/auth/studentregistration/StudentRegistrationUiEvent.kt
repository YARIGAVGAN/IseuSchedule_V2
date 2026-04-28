package com.example.scheduleiseu.feature.auth.studentregistration

import com.example.scheduleiseu.domain.core.model.StudentProfile

sealed interface StudentRegistrationUiEvent {
    data class RegistrationCompleted(val profile: StudentProfile) : StudentRegistrationUiEvent
}
