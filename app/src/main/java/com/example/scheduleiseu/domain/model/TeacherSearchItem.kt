package com.example.scheduleiseu.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class TeacherSearchItem(
    val id: String,
    val fullName: String,
    val subtitle: String? = null,
)
