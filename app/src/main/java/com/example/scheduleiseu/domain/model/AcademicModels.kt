package com.example.scheduleiseu.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Subject(
    val id: String,
    val title: String,
    val typeLabel: String,
    val grade: String,
)

@Immutable
data class Session(
    val id: String,
    val title: String,
    val subjects: List<Subject>,
    val averageScore: String? = null,
)

@Immutable
data class Course(
    val id: String,
    val title: String,
    val averageScore: String?,
    val sessions: List<Session>,
)
