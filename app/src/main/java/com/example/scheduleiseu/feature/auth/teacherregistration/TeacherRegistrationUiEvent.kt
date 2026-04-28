package com.example.scheduleiseu.feature.auth.teacherregistration

import com.example.scheduleiseu.domain.core.model.TeacherProfile

sealed interface TeacherRegistrationUiEvent {
    data class RegistrationCompleted(val profile: TeacherProfile) : TeacherRegistrationUiEvent
}
