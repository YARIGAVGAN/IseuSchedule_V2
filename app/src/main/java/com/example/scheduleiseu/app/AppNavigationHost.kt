package com.example.scheduleiseu.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.feature.about.AboutScreen
import com.example.scheduleiseu.feature.auth.login.LoginScreen
import com.example.scheduleiseu.feature.auth.login.LoginUiEvent
import com.example.scheduleiseu.feature.auth.login.LoginViewModel
import com.example.scheduleiseu.feature.auth.login.LoginViewModelFactory
import com.example.scheduleiseu.feature.auth.start.StartScreen
import com.example.scheduleiseu.feature.auth.studentregistration.StudentRegistrationScreen
import com.example.scheduleiseu.feature.auth.studentregistration.StudentRegistrationUiEvent
import com.example.scheduleiseu.feature.auth.studentregistration.StudentRegistrationViewModel
import com.example.scheduleiseu.feature.auth.studentregistration.StudentRegistrationViewModelFactory
import com.example.scheduleiseu.feature.auth.teacherregistration.TeacherRegistrationAction
import com.example.scheduleiseu.feature.auth.teacherregistration.TeacherRegistrationScreen
import com.example.scheduleiseu.feature.auth.teacherregistration.TeacherRegistrationUiEvent
import com.example.scheduleiseu.feature.auth.teacherregistration.TeacherRegistrationViewModel
import com.example.scheduleiseu.feature.auth.teacherregistration.TeacherRegistrationViewModelFactory
import com.example.scheduleiseu.feature.auth.teacherregistration.TeacherSearchBottomSheet
import com.example.scheduleiseu.feature.home.HomeScreen
import com.example.scheduleiseu.feature.home.ScheduleUiState
import com.example.scheduleiseu.feature.home.ScheduleViewModel
import com.example.scheduleiseu.feature.home.TeacherHomeScreen
import com.example.scheduleiseu.feature.home.TeacherScheduleViewModel
import com.example.scheduleiseu.feature.navigation.AppNavigationCommand
import com.example.scheduleiseu.feature.navigation.AppNavigationUiState
import com.example.scheduleiseu.feature.navigation.AppNavigationViewModel
import com.example.scheduleiseu.feature.navigation.AppRoute
import com.example.scheduleiseu.feature.performance.PerformanceFeatureHost
import com.example.scheduleiseu.feature.settings.SettingsScreen
import com.example.scheduleiseu.core.ui.animation.appScreenEnterTransition
import com.example.scheduleiseu.core.ui.animation.appScreenExitTransition
import com.example.scheduleiseu.core.ui.animation.appScreenPopEnterTransition
import com.example.scheduleiseu.core.ui.animation.appScreenPopExitTransition

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AppNavigationHost(
    navController: NavHostController,
    startDestination: String,
    navigationState: AppNavigationUiState,
    navigationViewModel: AppNavigationViewModel,
    scheduleViewModel: ScheduleViewModel?,
    scheduleState: ScheduleUiState,
    teacherScheduleViewModel: TeacherScheduleViewModel?,
    teacherScheduleState: ScheduleUiState,
    requestPostNotificationsPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { appScreenEnterTransition() },
        exitTransition = { appScreenExitTransition() },
        popEnterTransition = { appScreenPopEnterTransition() },
        popExitTransition = { appScreenPopExitTransition() },
    ) {
        composable(AppRoute.Start.route) {
            StartScreen(
                onStudentClick = navigationViewModel::onStudentModeSelected,
                onTeacherClick = navigationViewModel::onTeacherModeSelected,
            )
        }

        composable(AppRoute.StudentLogin.route) {
            val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory())
            val loginState by loginViewModel.state.collectAsState()

            LaunchedEffect(loginViewModel) {
                loginViewModel.events.collect { event ->
                    when (event) {
                        is LoginUiEvent.LoginSucceeded -> navigationViewModel.onStudentLoginSucceeded(
                            session = event.session,
                            accountLogin = event.accountLogin
                        )

                        LoginUiEvent.ContinueWithoutRegistration -> navigationViewModel.onStudentScheduleOnlyRequested()
                    }
                }
            }

            LoginScreen(
                state = loginState,
                onAction = loginViewModel::onAction,
                modifier = modifier,
            )
        }

        composable(AppRoute.StudentRegistration.route) {
            val registrationViewModel: StudentRegistrationViewModel = viewModel(
                factory = StudentRegistrationViewModelFactory()
            )
            val registrationState by registrationViewModel.state.collectAsState()

            LaunchedEffect(navigationState.studentDisplayName) {
                registrationViewModel.prefillNameIfBlank(navigationState.studentDisplayName)
            }

            LaunchedEffect(registrationViewModel) {
                registrationViewModel.events.collect { event ->
                    when (event) {
                        is StudentRegistrationUiEvent.RegistrationCompleted -> {
                            navigationViewModel.onStudentRegistrationCompleted()
                        }
                    }
                }
            }

            StudentRegistrationScreen(
                state = registrationState,
                onAction = registrationViewModel::onAction,
                modifier = modifier,
            )
        }

        composable(AppRoute.TeacherRegistration.route) {
            val registrationViewModel: TeacherRegistrationViewModel = viewModel(
                factory = TeacherRegistrationViewModelFactory()
            )
            val registrationState by registrationViewModel.state.collectAsState()
            val searchState by registrationViewModel.searchState.collectAsState()

            LaunchedEffect(registrationViewModel) {
                registrationViewModel.events.collect { event ->
                    when (event) {
                        is TeacherRegistrationUiEvent.RegistrationCompleted -> {
                            navigationViewModel.onTeacherRegistrationCompleted()
                        }
                    }
                }
            }

            TeacherRegistrationScreen(
                state = registrationState,
                onAction = { action ->
                    when (action) {
                        TeacherRegistrationAction.ContinueWithoutRegistrationClicked -> {
                            navigationViewModel.onTeacherRegistrationSkipped()
                        }

                        else -> registrationViewModel.onAction(action)
                    }
                },
                modifier = modifier,
            )

            if (searchState.isVisible) {
                TeacherSearchBottomSheet(
                    query = searchState.query,
                    results = searchState.results,
                    isLoading = searchState.isLoading,
                    onQueryChange = registrationViewModel::onSearchQueryChange,
                    onTeacherClick = registrationViewModel::onTeacherSelected,
                    onClearQueryClick = registrationViewModel::onClearSearchQueryClick,
                    onDismiss = registrationViewModel::onSearchDismiss,
                )
            }
        }

        composable(AppRoute.Home.route) {
            if (navigationState.userRole == UserRole.STUDENT && scheduleViewModel != null) {
                HomeScreen(
                    state = scheduleState,
                    onMenuClick = navigationViewModel::openDrawer,
                    onResetTemporaryContextClick = scheduleViewModel::resetTemporaryContext,
                    onScreenSettingsClick = navigationViewModel::openHomeActions,
                    onDayClick = scheduleViewModel::onDayClick,
                    onNextWeekSwipe = scheduleViewModel::onNextWeekRequested,
                    onPreviousWeekSwipe = scheduleViewModel::onPreviousWeekRequested,
                    onRefresh = scheduleViewModel::refreshSchedule,
                    modifier = modifier,
                )
            } else {
                TeacherHomeScreen(
                    state = teacherScheduleState,
                    onMenuClick = navigationViewModel::openDrawer,
                    onScreenSettingsClick = navigationViewModel::openHomeActions,
                    onDayClick = teacherScheduleViewModel?.let { it::onDayClick } ?: { _: String -> },
                    onNextWeekSwipe = teacherScheduleViewModel?.let { it::onNextWeekRequested } ?: {},
                    onPreviousWeekSwipe = teacherScheduleViewModel?.let { it::onPreviousWeekRequested } ?: {},
                    onRefresh = teacherScheduleViewModel?.let { it::refreshSchedule } ?: {},
                    modifier = modifier,
                )
            }
        }

        composable(AppRoute.Performance.route) {
            if (navigationState.isStudentScheduleOnlyMode) {
                LaunchedEffect(Unit) {
                    navigationViewModel.onDrawerDestinationSelected(com.example.scheduleiseu.feature.menu.DrawerDestination.HOME)
                }
            } else {
                PerformanceFeatureHost(
                    onMenuClick = navigationViewModel::openDrawer,
                    modifier = modifier,
                )
            }
        }

        composable(AppRoute.Info.route) {
            AboutScreen(
                onMenuClick = navigationViewModel::openDrawer,
                modifier = modifier,
            )
        }

        composable(AppRoute.Settings.route) {
            SettingsScreen(
                items = if (navigationState.userRole == UserRole.TEACHER) {
                    navigationState.settings.filter { it.title == "Уведомления о парах" }
                } else {
                    navigationState.settings
                },
                onMenuClick = navigationViewModel::openDrawer,
                onRegistrationDataClick = if (navigationState.userRole == UserRole.STUDENT) {
                    navigationViewModel::openRegistrationEdit
                } else {
                    null
                },
                onItemCheckedChange = { id, checked ->
                    navigationViewModel.updateSetting(
                        id = id,
                        checked = checked,
                        requestPostNotificationsPermission = requestPostNotificationsPermission
                    )
                },
                modifier = modifier,
            )
        }
    }
}

internal fun NavHostController.handleNavigationCommand(command: AppNavigationCommand) {
    when (command) {
        is AppNavigationCommand.Navigate -> navigate(command.route) {
            command.popUpToRoute?.let { route ->
                popUpTo(route) { inclusive = command.inclusive }
            }
            launchSingleTop = command.launchSingleTop
        }

        is AppNavigationCommand.ClearBackStackAndNavigate -> navigate(command.route) {
            popUpTo(graph.id) { inclusive = true }
            launchSingleTop = true
        }

        is AppNavigationCommand.ShowToast -> Unit
    }
}
