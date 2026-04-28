package com.example.scheduleiseu.feature.navigation

import com.example.scheduleiseu.domain.model.SettingsItem

enum class ActiveHomeSelector {
    Teacher,
    Group,
    Week
}

data class AppNavigationUiState(
    val isStartResolved: Boolean = false,
    val isBootstrapping: Boolean = false,
    val bootstrapErrorMessage: String? = null,
    val currentRoute: String = AppRoute.Start.route,
    val userRole: com.example.scheduleiseu.domain.core.model.UserRole = com.example.scheduleiseu.domain.core.model.UserRole.STUDENT,
    val isDrawerOpen: Boolean = false,
    val isHomeActionsOpen: Boolean = false,
    internal val activeHomeSelector: ActiveHomeSelector? = null,
    val settings: List<SettingsItem> = emptyList(),
    val logoutInProgress: Boolean = false,
    val logoutErrorMessage: String? = null,
    val studentDisplayName: String? = null,
    val offlineLoginRequired: Boolean = false,
    val isOfflineMode: Boolean = false,
    val offlineMessage: String? = null,
    val isStudentScheduleOnlyMode: Boolean = false
) {
    val isMainFlow: Boolean = currentRoute in AppRoute.mainRoutes
    val shouldShowTeacherSelector: Boolean = isMainFlow && activeHomeSelector == ActiveHomeSelector.Teacher
    val shouldShowGroupSelector: Boolean = isMainFlow && activeHomeSelector == ActiveHomeSelector.Group
    val shouldShowWeekSelector: Boolean = isMainFlow && activeHomeSelector == ActiveHomeSelector.Week
    val shouldShowOfflineLoginBanner: Boolean = false

}
