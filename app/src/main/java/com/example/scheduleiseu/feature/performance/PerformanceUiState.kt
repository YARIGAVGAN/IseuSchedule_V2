package com.example.scheduleiseu.feature.performance

import com.example.scheduleiseu.domain.model.Course

data class PerformanceUiState(
    val course: Course? = null,
    val selectedSemesterId: String? = null,
    val loadingSemesterId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && course?.sessions.orEmpty().isEmpty()
}
