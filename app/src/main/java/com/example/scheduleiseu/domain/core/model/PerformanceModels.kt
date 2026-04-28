package com.example.scheduleiseu.domain.core.model

data class SemesterReference(
    val id: String,
    val title: String
)

data class SubjectPerformance(
    val subjectName: String,
    val controlType: String,
    val result: String
)

data class SemesterPerformance(
    val semesterId: String?,
    val semesterTitle: String,
    val averageScore: String? = null,
    val subjects: List<SubjectPerformance>,
    val availableSemesters: List<SemesterReference> = emptyList()
)
