package com.example.scheduleiseu.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.ui.animation.AppCrossfade
import com.example.scheduleiseu.core.ui.animation.FadeSlideVisibility
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.core.ui.animation.appScreenEnterTransition
import com.example.scheduleiseu.core.ui.animation.appScreenExitTransition
import com.example.scheduleiseu.core.ui.animation.appScreenPopEnterTransition
import com.example.scheduleiseu.core.ui.animation.appScreenPopExitTransition
import com.example.scheduleiseu.data.repository.core.BsuCabinetDataComponent
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
import com.example.scheduleiseu.feature.home.HomeActionsMenu
import com.example.scheduleiseu.feature.home.HomeScreen
import com.example.scheduleiseu.feature.home.ScheduleUiState
import com.example.scheduleiseu.feature.home.ScheduleViewModel
import com.example.scheduleiseu.feature.home.ScheduleViewModelFactory
import com.example.scheduleiseu.feature.home.TeacherHomeScreen
import com.example.scheduleiseu.feature.home.TeacherScheduleViewModel
import com.example.scheduleiseu.feature.home.TeacherScheduleViewModelFactory
import com.example.scheduleiseu.feature.menu.MenuDrawerOverlay
import com.example.scheduleiseu.feature.menu.MenuProfileUiState
import com.example.scheduleiseu.feature.menu.MenuProfileViewModel
import com.example.scheduleiseu.feature.menu.MenuProfileViewModelFactory
import com.example.scheduleiseu.feature.navigation.AppNavigationCommand
import com.example.scheduleiseu.feature.navigation.AppNavigationUiState
import com.example.scheduleiseu.feature.navigation.AppNavigationViewModel
import com.example.scheduleiseu.feature.navigation.AppNavigationViewModelFactory
import com.example.scheduleiseu.feature.navigation.AppRoute
import com.example.scheduleiseu.feature.performance.PerformanceFeatureHost
import com.example.scheduleiseu.feature.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BsuCabinetDataComponent.initialize(context.applicationContext)

    val navController = rememberNavController()
    val navigationViewModel: AppNavigationViewModel = viewModel(factory = AppNavigationViewModelFactory())
    val navigationState by navigationViewModel.state.collectAsState()

    val postNotificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { navigationViewModel.scheduleLessonNotifications() }
    )
    val requestPostNotificationsPermission: () -> Unit = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            navigationViewModel.scheduleLessonNotifications()
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentBackStackEntry?.destination?.route) {
        currentBackStackEntry?.destination?.route?.let(navigationViewModel::onRouteChanged)
    }

    LaunchedEffect(navigationViewModel, navController) {
        navigationViewModel.commands.collect { command ->
            navController.handleNavigationCommand(command)
        }
    }

    if (!navigationState.isStartResolved) {
        StartupBlockingOverlay(modifier = modifier)
        return
    }

    val studentScheduleViewModel: ScheduleViewModel? = if (
        navigationState.isMainFlow && navigationState.userRole == UserRole.STUDENT
    ) {
        viewModel(factory = ScheduleViewModelFactory())
    } else {
        null
    }
    val scheduleState = studentScheduleViewModel?.state?.collectAsState()?.value ?: ScheduleUiState()

    val teacherScheduleViewModel: TeacherScheduleViewModel? = if (
        navigationState.isMainFlow && navigationState.userRole == UserRole.TEACHER
    ) {
        viewModel(factory = TeacherScheduleViewModelFactory())
    } else {
        null
    }
    val teacherScheduleState = teacherScheduleViewModel?.state?.collectAsState()?.value ?: ScheduleUiState()

    val menuProfileViewModel: MenuProfileViewModel? = if (navigationState.isMainFlow) {
        viewModel(factory = MenuProfileViewModelFactory())
    } else {
        null
    }
    val menuProfileState = menuProfileViewModel?.state?.collectAsState()?.value ?: MenuProfileUiState()
    AppNavHost(
        navController = navController,
        startDestination = navigationState.currentRoute,
        navigationState = navigationState,
        navigationViewModel = navigationViewModel,
        scheduleViewModel = studentScheduleViewModel,
        scheduleState = scheduleState,
        teacherScheduleViewModel = teacherScheduleViewModel,
        teacherScheduleState = teacherScheduleState,
        requestPostNotificationsPermission = requestPostNotificationsPermission,
        modifier = modifier,
    )

    MainFlowOverlays(
        state = navigationState,
        navigationViewModel = navigationViewModel,
        scheduleViewModel = studentScheduleViewModel,
        scheduleState = scheduleState,
        teacherScheduleViewModel = teacherScheduleViewModel,
        teacherScheduleState = teacherScheduleState,
        menuProfileState = menuProfileState
    )

    OfflineLoginBanner(
        visible = navigationState.shouldShowOfflineLoginBanner,
        onClick = navigationViewModel::onOfflineLoginBannerClick
    )

    BootstrapBlockingOverlay(
        visible = navigationState.isBootstrapping || navigationState.bootstrapErrorMessage != null,
        errorMessage = navigationState.bootstrapErrorMessage,
        onRetryClick = navigationViewModel::retryStudentBootstrap
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppNavHost(
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
){
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
                        is StudentRegistrationUiEvent.RegistrationCompleted -> navigationViewModel.onStudentRegistrationCompleted()
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
                        is TeacherRegistrationUiEvent.RegistrationCompleted -> navigationViewModel.onTeacherRegistrationCompleted()
                    }
                }
            }

            TeacherRegistrationScreen(
                state = registrationState,
                onAction = { action ->
                    when (action) {
                        TeacherRegistrationAction.ContinueWithoutRegistrationClicked -> navigationViewModel.onTeacherRegistrationSkipped()
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
                    modifier = modifier,
                )
            } else {
                TeacherHomeScreen(
                    state = teacherScheduleState,
                    onMenuClick = navigationViewModel::openDrawer,
                    onScreenSettingsClick = navigationViewModel::openHomeActions,
                    onDayClick = teacherScheduleViewModel?.let { it::onDayClick } ?: { _: String -> },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainFlowOverlays(
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
            items = activeScheduleState.availableWeeks.map { week -> if (week.isCached) "${week.title} +" else week.title },
            selectedItem = activeScheduleState.selectedWeek?.let { week -> if (week.isCached) "${week.title} +" else week.title },
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

    MenuDrawerOverlay(
        isOpen = state.isDrawerOpen,
        onClose = navigationViewModel::closeDrawer,
        onDestinationClick = navigationViewModel::onDrawerDestinationSelected,
        profileState = menuProfileState,
    )
}

@Composable
private fun StartupBlockingOverlay(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = AppColors.White) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = AppColors.HeaderGreen
            )
        }
    }
}



@Composable
private fun OfflineLoginBanner(
    visible: Boolean,
    onClick: () -> Unit
) {
    FadeSlideVisibility(visible = visible) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            PressScale {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable(onClick = onClick),
                    color = AppColors.Error,
                    shape = AppShapes.extraLarge
                ) {
                    Text(
                        text = "Оффлайн, нажмите, чтобы войти",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}



@Composable
private fun BootstrapBlockingOverlay(
    visible: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit
) {
    FadeSlideVisibility(visible = visible) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            color = AppColors.White.copy(alpha = 0.96f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appAnimatedContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AppCrossfade(targetState = errorMessage, label = "bootstrapState") { message ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (message == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = AppColors.HeaderGreen
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Загружаем данные для первого входа",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppColors.ScreenTitle,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Профиль, расписание и успеваемость сохраняются в кэш",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.ScreenTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "Не удалось завершить первичную загрузку",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppColors.ScreenTitle,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.ScreenTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                PressScale {
                                    Button(
                                        onClick = onRetryClick,
                                        modifier = Modifier.fillMaxWidth(0.72f).height(52.dp),
                                        shape = AppShapes.extraLarge,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppColors.HeaderGreen,
                                            contentColor = AppColors.White
                                        )
                                    ) {
                                        Text(text = "Повторить", style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun NavHostController.handleNavigationCommand(command: AppNavigationCommand) {
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
    }
}

private fun UserRole.toPresentationRole(): com.example.scheduleiseu.domain.model.UserRole {
    return when (this) {
        UserRole.STUDENT -> com.example.scheduleiseu.domain.model.UserRole.Student
        UserRole.TEACHER -> com.example.scheduleiseu.domain.model.UserRole.Teacher
    }
}
