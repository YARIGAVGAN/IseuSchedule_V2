package com.example.scheduleiseu.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.scheduleiseu.data.repository.core.BsuCabinetDataComponent
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.feature.home.ScheduleUiState
import com.example.scheduleiseu.feature.home.ScheduleViewModel
import com.example.scheduleiseu.feature.home.ScheduleViewModelFactory
import com.example.scheduleiseu.feature.home.TeacherScheduleViewModel
import com.example.scheduleiseu.feature.home.TeacherScheduleViewModelFactory
import com.example.scheduleiseu.feature.menu.MenuProfileUiState
import com.example.scheduleiseu.feature.menu.MenuProfileViewModel
import com.example.scheduleiseu.feature.menu.MenuProfileViewModelFactory
import com.example.scheduleiseu.feature.navigation.AppNavigationCommand
import com.example.scheduleiseu.feature.navigation.AppNavigationViewModel
import com.example.scheduleiseu.feature.navigation.AppNavigationViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun AppRoot(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BsuCabinetDataComponent.initialize(context.applicationContext)

    val navController = rememberNavController()
    val navigationViewModel: AppNavigationViewModel = viewModel(factory = AppNavigationViewModelFactory())
    val navigationState by navigationViewModel.state.collectAsState()

    var activeToast by remember { mutableStateOf<AppToastMessage?>(null) }

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
            when (command) {
                is AppNavigationCommand.ShowToast -> {
                    activeToast = AppToastMessage(
                        message = command.message,
                        durationMillis = command.durationMillis
                    )
                }

                else -> navController.handleNavigationCommand(command)
            }
        }
    }

    LaunchedEffect(activeToast?.id) {
        val toast = activeToast ?: return@LaunchedEffect
        delay(toast.durationMillis)
        if (activeToast?.id == toast.id) {
            activeToast = null
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

    AppNavigationHost(
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

    AppMainFlowOverlays(
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

    AppToastHost(message = activeToast)
}
