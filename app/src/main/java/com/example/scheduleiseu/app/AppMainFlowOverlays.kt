package com.example.scheduleiseu.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.feature.auth.teacherregistration.TeacherSearchBottomSheet
import com.example.scheduleiseu.feature.home.HomeActionsMenu
import com.example.scheduleiseu.feature.home.ScheduleUiState
import com.example.scheduleiseu.feature.home.ScheduleViewModel
import com.example.scheduleiseu.feature.home.TeacherScheduleViewModel
import com.example.scheduleiseu.feature.menu.MenuDrawerOverlay
import com.example.scheduleiseu.feature.menu.MenuProfileUiState
import com.example.scheduleiseu.feature.navigation.AppNavigationUiState
import com.example.scheduleiseu.feature.navigation.AppNavigationViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AppMainFlowOverlays(
    state: AppNavigationUiState,
    navigationViewModel: AppNavigationViewModel,
    scheduleViewModel: ScheduleViewModel?,
    scheduleState: ScheduleUiState,
    teacherScheduleViewModel: TeacherScheduleViewModel?,
    teacherScheduleState: ScheduleUiState,
    menuProfileState: MenuProfileUiState
) {
    if (!state.isMainFlow) return

    if (state.isHomeActionsOpen) {
        HomeActionsMenu(
            role = state.userRole.toPresentationRole(),
            showGroupSelection = scheduleState.selectedTeacherName == null,
            onDismiss = navigationViewModel::closeHomeActions,
            onSelectTeacherClick = navigationViewModel::openTeacherSelector,
            onSelectGroupClick = navigationViewModel::openGroupSelector,
            onSelectWeekClick = navigationViewModel::openWeekSelector,
        )
    }

    AppBottomSheets(
        state = state,
        navigationViewModel = navigationViewModel,
        scheduleViewModel = scheduleViewModel,
        scheduleState = scheduleState,
        teacherScheduleViewModel = teacherScheduleViewModel,
        teacherScheduleState = teacherScheduleState
    )

    MenuDrawerOverlay(
        isOpen = state.isDrawerOpen,
        onClose = navigationViewModel::closeDrawer,
        onDestinationClick = navigationViewModel::onDrawerDestinationSelected,
        profileState = menuProfileState,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppBottomSheets(
    state: AppNavigationUiState,
    navigationViewModel: AppNavigationViewModel,
    scheduleViewModel: ScheduleViewModel?,
    scheduleState: ScheduleUiState,
    teacherScheduleViewModel: TeacherScheduleViewModel?,
    teacherScheduleState: ScheduleUiState
) {
    if (state.shouldShowTeacherSelector && scheduleViewModel != null) {
        TeacherSearchBottomSheet(
            query = scheduleState.teacherQuery,
            results = scheduleState.filteredTeachers,
            isLoading = scheduleState.isLoading && scheduleState.availableTeachers.isEmpty(),
            onQueryChange = scheduleViewModel::onTeacherQueryChanged,
            onTeacherClick = { item ->
                navigationViewModel.closeActiveSelector()
                scheduleViewModel.onExternalTeacherSelected(item)
            },
            onClearQueryClick = scheduleViewModel::onTeacherSearchCleared,
            onDismiss = navigationViewModel::closeActiveSelector,
        )
    }

    if (state.shouldShowGroupSelector && scheduleViewModel != null) {
        SimpleSelectionBottomSheet(
            title = "Выбрать группу",
            items = scheduleState.availableGroups,
            selectedItem = scheduleState.selectedGroupTitle,
            isScrollable = true,
            onSelect = { groupTitle ->
                navigationViewModel.closeActiveSelector()
                scheduleViewModel.onExternalGroupSelected(groupTitle)
            },
            onDismiss = navigationViewModel::closeActiveSelector,
        )
    }

    if (state.shouldShowWeekSelector) {
        val activeScheduleState = if (scheduleViewModel != null) scheduleState else teacherScheduleState
        SimpleSelectionBottomSheet(
            title = "Выбрать неделю",
            items = activeScheduleState.availableWeeks.map { week ->
                if (week.isCached) "${week.title} +" else week.title
            },
            selectedItem = activeScheduleState.selectedWeek?.let { week ->
                if (week.isCached) "${week.title} +" else week.title
            },
            isScrollable = true,
            onSelect = { week ->
                navigationViewModel.closeActiveSelector()
                if (scheduleViewModel != null) {
                    scheduleViewModel.onWeekSelected(week)
                } else {
                    teacherScheduleViewModel?.onWeekSelected(week)
                }
            },
            onDismiss = navigationViewModel::closeActiveSelector,
        )
    }
}

private fun UserRole.toPresentationRole(): com.example.scheduleiseu.domain.model.UserRole {
    return when (this) {
        UserRole.STUDENT -> com.example.scheduleiseu.domain.model.UserRole.Student
        UserRole.TEACHER -> com.example.scheduleiseu.domain.model.UserRole.Teacher
    }
}
