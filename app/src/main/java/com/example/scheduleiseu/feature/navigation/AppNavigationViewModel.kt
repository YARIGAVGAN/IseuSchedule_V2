package com.example.scheduleiseu.feature.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.network.NetworkMonitor
import com.example.scheduleiseu.domain.core.repository.AuthRepository
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository
import com.example.scheduleiseu.domain.core.service.CaptchaRecognizer
import com.example.scheduleiseu.domain.core.usecase.BackgroundRefreshResult
import com.example.scheduleiseu.domain.core.usecase.BackgroundRefreshUseCase
import com.example.scheduleiseu.domain.core.usecase.FirstEntryBootstrapUseCase
import com.example.scheduleiseu.domain.model.SettingsItem
import com.example.scheduleiseu.feature.menu.DrawerDestination
import com.example.scheduleiseu.notification.LessonNotificationScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppNavigationViewModel(
    private val preferencesDataSource: AppPreferencesDataSource,
    private val authRepository: AuthRepository,
    private val teacherRegistrationRepository: TeacherRegistrationRepository,
    private val scheduleRepository: ScheduleRepository,
    private val firstEntryBootstrapUseCase: FirstEntryBootstrapUseCase,
    private val backgroundRefreshUseCase: BackgroundRefreshUseCase,
    private val networkMonitor: NetworkMonitor,
    private val captchaRecognizer: CaptchaRecognizer,
    private val lessonNotificationScheduler: LessonNotificationScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(
        AppNavigationUiState(
            settings = settingsFrom(
                role = UserRole.STUDENT,
                cacheCurrentAndPreviousWeek = false,
                lessonNotificationsEnabled = false,
                showMismatchedSubgroupLessons = true
            )
        )
    )
    val state: StateFlow<AppNavigationUiState> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<AppNavigationCommand>()
    val commands: SharedFlow<AppNavigationCommand> = _commands.asSharedFlow()

    private var pendingStudentBootstrapAccountKey: String? = null
    private var startupAutoLoginInProgress = false
    private var startupAutoLoginAttempted = false


    init {
        observeSettings()
        observeConnectivity()
        resolveStartDestination()
    }

    fun onRouteChanged(route: String) {
        _state.update { current ->
            current.copy(
                currentRoute = route,
                isDrawerOpen = if (route in AppRoute.mainRoutes) current.isDrawerOpen else false,
                isHomeActionsOpen = if (route == AppRoute.Home.route) current.isHomeActionsOpen else false,
                activeHomeSelector = if (route == AppRoute.Home.route) current.activeHomeSelector else null
            )
        }
    }

    fun onStudentModeSelected() {
        viewModelScope.launch {
            preferencesDataSource.setUserRole(UserRole.STUDENT)
            preferencesDataSource.setStudentScheduleOnlyModeEnabled(false)
            _state.update {
                it.copy(
                    userRole = UserRole.STUDENT,
                    studentDisplayName = null,
                    bootstrapErrorMessage = null,
                    offlineLoginRequired = false,
                    isStudentScheduleOnlyMode = false
                )
            }
            _commands.emit(AppNavigationCommand.Navigate(AppRoute.StudentLogin.route))
        }
    }

    fun onTeacherModeSelected() {
        viewModelScope.launch {
            preferencesDataSource.setUserRole(UserRole.TEACHER)
            preferencesDataSource.setStudentScheduleOnlyModeEnabled(false)
            _state.update {
                it.copy(
                    userRole = UserRole.TEACHER,
                    studentDisplayName = null,
                    bootstrapErrorMessage = null,
                    offlineLoginRequired = false,
                    isStudentScheduleOnlyMode = false
                )
            }
            _commands.emit(AppNavigationCommand.Navigate(AppRoute.TeacherRegistration.route))
        }
    }

    fun onStudentLoginSucceeded(session: AuthSession, accountLogin: String) {
        viewModelScope.launch {
            preferencesDataSource.setUserRole(UserRole.STUDENT)
            preferencesDataSource.setStudentScheduleOnlyModeEnabled(false)
            val accountKey = accountLogin.toStudentBootstrapAccountKey()
            if (accountKey == null) {
                _state.update {
                    it.copy(
                        isBootstrapping = false,
                        bootstrapErrorMessage = "Не удалось определить аккаунт для первичной загрузки"
                    )
                }
                return@launch
            }
            pendingStudentBootstrapAccountKey = accountKey
            val hasStudentProfile = preferencesDataSource.getStudentProfile() != null
            val registrationCompleted = preferencesDataSource.isStudentRegistrationCompleted()
            val shouldOpenHome = hasStudentProfile || registrationCompleted

            _state.update {
                it.copy(
                    userRole = UserRole.STUDENT,
                    studentDisplayName = session.displayName?.takeIf { name -> name.isNotBlank() },
                    bootstrapErrorMessage = null,
                    offlineLoginRequired = false,
                    isStudentScheduleOnlyMode = false
                )
            }

            if (shouldOpenHome) {
                navigateHomeAfterStudentBootstrapIfNeeded(accountKey)
            } else {
                _commands.emit(
                    AppNavigationCommand.Navigate(
                        route = AppRoute.StudentRegistration.route,
                        popUpToRoute = AppRoute.StudentLogin.route,
                        inclusive = true
                    )
                )
            }
        }
    }

    fun onStudentScheduleOnlyRequested() {
        viewModelScope.launch {
            preferencesDataSource.setUserRole(UserRole.STUDENT)
            preferencesDataSource.setStudentScheduleOnlyModeEnabled(true)
            preferencesDataSource.clearStudentCredentials()
            preferencesDataSource.clearAuthFlags()
            pendingStudentBootstrapAccountKey = null
            _state.update {
                it.copy(
                    userRole = UserRole.STUDENT,
                    isStudentScheduleOnlyMode = true,
                    offlineLoginRequired = false,
                    isBootstrapping = false,
                    bootstrapErrorMessage = null,
                    isDrawerOpen = false,
                    isHomeActionsOpen = false,
                    activeHomeSelector = null
                )
            }
            _commands.emit(
                AppNavigationCommand.Navigate(
                    route = AppRoute.StudentRegistration.route,
                    popUpToRoute = AppRoute.StudentLogin.route,
                    inclusive = true
                )
            )
        }
    }

    fun onStudentRegistrationCompleted() {
        viewModelScope.launch {
            preferencesDataSource.setUserRole(UserRole.STUDENT)
            preferencesDataSource.setStudentRegistrationCompleted(true)
            val scheduleOnly = preferencesDataSource.isStudentScheduleOnlyModeEnabled()
            _state.update {
                it.copy(
                    userRole = UserRole.STUDENT,
                    isStudentScheduleOnlyMode = scheduleOnly,
                    bootstrapErrorMessage = null,
                    offlineLoginRequired = false
                )
            }

            if (scheduleOnly) {
                pendingStudentBootstrapAccountKey = null
                openHome(runBackgroundRefresh = true, offlineLoginRequired = false)
                return@launch
            }

            val accountKey = pendingStudentBootstrapAccountKey
                ?: preferencesDataSource.getSavedStudentLogin()?.toStudentBootstrapAccountKey()

            if (accountKey == null) {
                _state.update {
                    it.copy(
                        isBootstrapping = false,
                        bootstrapErrorMessage = "Не удалось определить аккаунт для первичной загрузки"
                    )
                }
                return@launch
            }

            pendingStudentBootstrapAccountKey = accountKey
            navigateHomeAfterStudentBootstrapIfNeeded(accountKey)
        }
    }

    fun onTeacherRegistrationCompleted() {
        viewModelScope.launch {
            preferencesDataSource.setUserRole(UserRole.TEACHER)
            preferencesDataSource.setStudentScheduleOnlyModeEnabled(false)
            _state.update {
                it.copy(
                    userRole = UserRole.TEACHER,
                    bootstrapErrorMessage = null,
                    offlineLoginRequired = false,
                    isStudentScheduleOnlyMode = false
                )
            }
            openHome(runBackgroundRefresh = true, offlineLoginRequired = false)
        }
    }

    fun onTeacherRegistrationSkipped() {
        viewModelScope.launch {
            preferencesDataSource.setUserRole(UserRole.TEACHER)
            preferencesDataSource.setStudentScheduleOnlyModeEnabled(false)
            _state.update {
                it.copy(
                    userRole = UserRole.TEACHER,
                    bootstrapErrorMessage = null,
                    offlineLoginRequired = false,
                    isStudentScheduleOnlyMode = false
                )
            }
            openHome(runBackgroundRefresh = true, offlineLoginRequired = false)
        }
    }

    fun retryStudentBootstrap() {
        val accountKey = pendingStudentBootstrapAccountKey ?: return
        viewModelScope.launch {
            navigateHomeAfterStudentBootstrapIfNeeded(accountKey)
        }
    }

    fun onOfflineLoginBannerClick() {
        if (!_state.value.shouldShowOfflineLoginBanner) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDrawerOpen = false,
                    isHomeActionsOpen = false,
                    activeHomeSelector = null
                )
            }
            _commands.emit(AppNavigationCommand.Navigate(AppRoute.StudentLogin.route))
        }
    }

    fun openDrawer() {
        _state.update { it.copy(isDrawerOpen = true, logoutErrorMessage = null) }
    }

    fun closeDrawer() {
        _state.update { it.copy(isDrawerOpen = false) }
    }

    fun onDrawerDestinationSelected(destination: DrawerDestination) {
        when (destination) {
            DrawerDestination.PERFORMANCE -> if (_state.value.isStudentScheduleOnlyMode) {
                navigateMain(AppRoute.Home.route)
            } else {
                navigateMain(AppRoute.Performance.route)
            }
            DrawerDestination.HOME -> navigateMain(AppRoute.Home.route)
            DrawerDestination.INFO -> navigateMain(AppRoute.Info.route)
            DrawerDestination.SETTINGS -> navigateMain(AppRoute.Settings.route)
            DrawerDestination.LOGOUT -> logout()
        }
    }

    fun openHomeActions() {
        _state.update { it.copy(isHomeActionsOpen = true) }
    }

    fun closeHomeActions() {
        _state.update { it.copy(isHomeActionsOpen = false) }
    }

    fun openTeacherSelector() {
        _state.update { it.copy(isHomeActionsOpen = false, activeHomeSelector = ActiveHomeSelector.Teacher) }
    }

    fun openGroupSelector() {
        _state.update { it.copy(isHomeActionsOpen = false, activeHomeSelector = ActiveHomeSelector.Group) }
    }

    fun openWeekSelector() {
        _state.update { it.copy(isHomeActionsOpen = false, activeHomeSelector = ActiveHomeSelector.Week) }
    }

    fun closeActiveSelector() {
        _state.update { it.copy(activeHomeSelector = null) }
    }

    fun openRegistrationEdit() {
        val route = when (_state.value.userRole) {
            UserRole.TEACHER -> AppRoute.TeacherRegistration.route
            UserRole.STUDENT -> AppRoute.StudentRegistration.route
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDrawerOpen = false,
                    isHomeActionsOpen = false,
                    activeHomeSelector = null
                )
            }
            _commands.emit(AppNavigationCommand.Navigate(route = route))
        }
    }

    fun updateSetting(
        id: String,
        checked: Boolean,
        requestPostNotificationsPermission: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (id) {
                CACHE_CURRENT_AND_PREVIOUS_WEEK_ID -> {
                    preferencesDataSource.setCacheCurrentAndPreviousWeekEnabled(checked)
                    if (!checked) {
                        scheduleRepository.clearAllCachedScheduleWeeks()
                    }
                    updateSettingsState(
                        cacheCurrentAndPreviousWeek = checked,
                        lessonNotificationsEnabled = preferencesDataSource.isLessonNotificationsEnabled(),
                        showMismatchedSubgroupLessons = preferencesDataSource.isShowMismatchedSubgroupLessonsEnabled()
                    )
                }

                SHOW_MISMATCHED_SUBGROUP_LESSONS_ID -> {
                    preferencesDataSource.setShowMismatchedSubgroupLessonsEnabled(checked)
                    updateSettingsState(
                        cacheCurrentAndPreviousWeek = preferencesDataSource.isCacheCurrentAndPreviousWeekEnabled(),
                        lessonNotificationsEnabled = preferencesDataSource.isLessonNotificationsEnabled(),
                        showMismatchedSubgroupLessons = checked
                    )
                }

                LESSON_NOTIFICATIONS_ID -> {
                    preferencesDataSource.setLessonNotificationsEnabled(checked)
                    updateSettingsState(
                        cacheCurrentAndPreviousWeek = preferencesDataSource.isCacheCurrentAndPreviousWeekEnabled(),
                        lessonNotificationsEnabled = checked,
                        showMismatchedSubgroupLessons = preferencesDataSource.isShowMismatchedSubgroupLessonsEnabled()
                    )
                    if (checked) {
                        requestPostNotificationsPermission()
                        lessonNotificationScheduler.scheduleNext()
                    } else {
                        lessonNotificationScheduler.cancelAll()
                    }
                }
            }
        }
    }

    fun scheduleLessonNotifications() {
        viewModelScope.launch {
            lessonNotificationScheduler.scheduleNext()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                preferencesDataSource.observeCacheCurrentAndPreviousWeekEnabled(),
                preferencesDataSource.observeLessonNotificationsEnabled(),
                preferencesDataSource.observeShowMismatchedSubgroupLessonsEnabled()
            ) { cacheCurrentAndPreviousWeek, lessonNotificationsEnabled, showMismatchedSubgroupLessons ->
                Triple(cacheCurrentAndPreviousWeek, lessonNotificationsEnabled, showMismatchedSubgroupLessons)
            }.collect { (cacheCurrentAndPreviousWeek, lessonNotificationsEnabled, showMismatchedSubgroupLessons) ->
                updateSettingsState(
                    cacheCurrentAndPreviousWeek = cacheCurrentAndPreviousWeek,
                    lessonNotificationsEnabled = lessonNotificationsEnabled,
                    showMismatchedSubgroupLessons = showMismatchedSubgroupLessons
                )
            }
        }
    }

    private fun updateSettingsState(
        cacheCurrentAndPreviousWeek: Boolean,
        lessonNotificationsEnabled: Boolean,
        showMismatchedSubgroupLessons: Boolean
    ) {
        _state.update { current ->
            current.copy(
                settings = settingsFrom(
                    role = current.userRole,
                    cacheCurrentAndPreviousWeek = cacheCurrentAndPreviousWeek,
                    lessonNotificationsEnabled = lessonNotificationsEnabled,
                    showMismatchedSubgroupLessons = showMismatchedSubgroupLessons
                )
            )
        }
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            authRepository.clearActiveSession()

            val role = preferencesDataSource.getUserRole() ?: UserRole.STUDENT
            val hasSavedStudentCredentials = preferencesDataSource.hasSavedStudentCredentials()
            val hasStudentProfile = preferencesDataSource.getStudentProfile() != null
            val studentRegistrationCompleted = preferencesDataSource.isStudentRegistrationCompleted()
            val hasTeacherProfile = preferencesDataSource.getTeacherProfile() != null
            val scheduleOnly = preferencesDataSource.isStudentScheduleOnlyModeEnabled()
            val cacheCurrentAndPreviousWeek = preferencesDataSource.isCacheCurrentAndPreviousWeekEnabled()
            val lessonNotificationsEnabled = preferencesDataSource.isLessonNotificationsEnabled()
            val showMismatchedSubgroupLessons = preferencesDataSource.isShowMismatchedSubgroupLessonsEnabled()
            val canOpenConfiguredStudentFlow = hasSavedStudentCredentials &&
                (hasStudentProfile || studentRegistrationCompleted)

            val targetRoute = when {
                role == UserRole.STUDENT && scheduleOnly && studentRegistrationCompleted -> AppRoute.Home.route
                role == UserRole.STUDENT && scheduleOnly -> AppRoute.StudentRegistration.route
                role == UserRole.STUDENT && canOpenConfiguredStudentFlow -> AppRoute.Home.route
                role == UserRole.STUDENT && hasSavedStudentCredentials -> AppRoute.StudentLogin.route
                role == UserRole.TEACHER && hasTeacherProfile -> AppRoute.Home.route
                else -> AppRoute.Start.route
            }
            val isOnline = networkMonitor.isCurrentlyOnline()
            val shouldTryStartupAutoLogin =
                role == UserRole.STUDENT
                    &&
                !scheduleOnly
                    &&
                targetRoute == AppRoute.Home.route
                    &&
                hasSavedStudentCredentials
                    &&
                isOnline

            _state.update {
                it.copy(
                    isStartResolved = true,
                    isBootstrapping = false,
                    bootstrapErrorMessage = null,
                    userRole = role,
                    settings = settingsFrom(
                        role,
                        cacheCurrentAndPreviousWeek,
                        lessonNotificationsEnabled,
                        showMismatchedSubgroupLessons
                    ),
                    currentRoute = targetRoute,
                    offlineLoginRequired =
                        role == UserRole.STUDENT
                                &&
                        !scheduleOnly
                                &&
                        targetRoute == AppRoute.Home.route
                                &&
                        !shouldTryStartupAutoLogin,
                    isStudentScheduleOnlyMode = scheduleOnly,
                    isOfflineMode = !isOnline,
                    offlineMessage = if (isOnline) null else OFFLINE_MESSAGE
                )
            }
            if (targetRoute == AppRoute.Home.route) {
                if (shouldTryStartupAutoLogin) {
                    launchStartupAutoLoginIfNeeded()
                } else {
                    launchBackgroundRefresh()
                }
            }
        }
    }

    private suspend fun navigateHomeAfterStudentBootstrapIfNeeded(accountKey: String) {
        if (firstEntryBootstrapUseCase.isCompleted(accountKey)) {
            openHome(runBackgroundRefresh = true, offlineLoginRequired = false)
            return
        }

        _state.update {
            it.copy(
                isBootstrapping = true,
                bootstrapErrorMessage = null,
                isDrawerOpen = false,
                isHomeActionsOpen = false,
                activeHomeSelector = null,
                offlineLoginRequired = false
            )
        }

        runCatching {
            firstEntryBootstrapUseCase(accountKey)
        }.onSuccess {
            openHome(runBackgroundRefresh = false, offlineLoginRequired = false)
        }.onFailure { throwable ->
            _state.update {
                it.copy(
                    isBootstrapping = false,
                    bootstrapErrorMessage = throwable.message ?: "Не удалось выполнить первичную загрузку"
                )
            }
        }
    }

    private suspend fun openHome(runBackgroundRefresh: Boolean, offlineLoginRequired: Boolean) {
        val isOnline = networkMonitor.isCurrentlyOnline()
        _state.update {
            it.copy(
                isBootstrapping = false,
                bootstrapErrorMessage = null,
                currentRoute = AppRoute.Home.route,
                isDrawerOpen = false,
                isHomeActionsOpen = false,
                activeHomeSelector = null,
                offlineLoginRequired = offlineLoginRequired && !it.isStudentScheduleOnlyMode,
                isOfflineMode = !isOnline,
                offlineMessage = if (isOnline) null else OFFLINE_MESSAGE
            )
        }
        _commands.emit(AppNavigationCommand.ClearBackStackAndNavigate(AppRoute.Home.route))
        if (runBackgroundRefresh) {
            launchBackgroundRefresh()
        }
    }

    private fun launchBackgroundRefresh() {
        if (startupAutoLoginInProgress) return

        viewModelScope.launch {
            if (startupAutoLoginInProgress) return@launch
            if (!networkMonitor.isCurrentlyOnline()) {
                _state.update { current ->
                    current.copy(
                        isOfflineMode = true,
                        offlineMessage = OFFLINE_MESSAGE,
                        offlineLoginRequired = current.offlineLoginRequired && !current.isStudentScheduleOnlyMode
                    )
                }
                return@launch
            }

            val result = runCatching { backgroundRefreshUseCase() }
                .getOrDefault(BackgroundRefreshResult.LoginRequired)

            _state.update { current ->
                if (current.userRole == UserRole.STUDENT && current.isMainFlow && !current.isStudentScheduleOnlyMode) {
                    current.copy(
                        offlineLoginRequired = result == BackgroundRefreshResult.LoginRequired,
                        isOfflineMode = false,
                        offlineMessage = null
                    )
                } else {
                    current.copy(isOfflineMode = false, offlineMessage = null)
                }
            }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                _state.update {
                    it.copy(
                        isOfflineMode = !isOnline,
                        offlineMessage = if (isOnline) null else OFFLINE_MESSAGE
                    )
                }
                if (!isOnline) {
                    startupAutoLoginAttempted = false
                    return@collect
                }

                if (_state.value.isMainFlow) {
                    val autoLoginStarted = launchStartupAutoLoginIfNeeded()
                    if (!autoLoginStarted) {
                        launchBackgroundRefresh()
                    }
                }
            }
        }
    }

    private fun launchStartupAutoLoginIfNeeded(): Boolean {
        if (startupAutoLoginInProgress || startupAutoLoginAttempted) return false
        if (!networkMonitor.isCurrentlyOnline()) return false

        val current = _state.value
        if (current.userRole != UserRole.STUDENT || current.isStudentScheduleOnlyMode || !current.isMainFlow) {
            return false
        }

        startupAutoLoginInProgress = true
        startupAutoLoginAttempted = true

        viewModelScope.launch {
            val login = preferencesDataSource.getSavedStudentLogin().orEmpty()
            val password = preferencesDataSource.getSavedStudentPassword().orEmpty()
            if (login.isBlank() || password.isBlank()) {
                startupAutoLoginInProgress = false
                _state.update { currentState ->
                    currentState.copy(
                        offlineLoginRequired = currentState.isMainFlow &&
                            currentState.userRole == UserRole.STUDENT &&
                            !currentState.isStudentScheduleOnlyMode,
                        isOfflineMode = false,
                        offlineMessage = null
                    )
                }
                return@launch
            }

            val result = runCatching {
                val session = authRepository.prepareSession(UserRole.STUDENT)
                val captchaBytes = authRepository.loadCaptcha(session)
                val captcha = captchaRecognizer.recognize(captchaBytes).orEmpty().trim()
                if (captcha.isBlank()) {
                    throw IllegalStateException("Captcha не распознана автоматически")
                }
                authRepository.signIn(
                    session = session,
                    login = login,
                    password = password,
                    captcha = captcha
                )
            }

            startupAutoLoginInProgress = false

            result.onSuccess { authenticatedSession ->
                preferencesDataSource.setStudentScheduleOnlyModeEnabled(false)
                preferencesDataSource.saveStudentCredentials(login = login, password = password)
                val accountKey = login.toStudentBootstrapAccountKey()
                pendingStudentBootstrapAccountKey = accountKey
                _state.update {
                    it.copy(
                        studentDisplayName = authenticatedSession.displayName?.takeIf { name -> name.isNotBlank() },
                        offlineLoginRequired = false,
                        isOfflineMode = false,
                        offlineMessage = null,
                        bootstrapErrorMessage = null
                    )
                }

                if (accountKey != null && !firstEntryBootstrapUseCase.isCompleted(accountKey)) {
                    navigateHomeAfterStudentBootstrapIfNeeded(accountKey)
                } else {
                    launchBackgroundRefresh()
                }
            }.onFailure {
                _state.update { currentState ->
                    currentState.copy(
                        offlineLoginRequired = currentState.isMainFlow &&
                            currentState.userRole == UserRole.STUDENT &&
                            !currentState.isStudentScheduleOnlyMode,
                        isOfflineMode = false,
                        offlineMessage = null
                    )
                }
            }
        }

        return true
    }
    private fun navigateMain(route: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDrawerOpen = false,
                    isHomeActionsOpen = false,
                    activeHomeSelector = null,
                    logoutErrorMessage = null
                )
            }
            _commands.emit(
                AppNavigationCommand.Navigate(
                    route = route,
                    popUpToRoute = AppRoute.Home.route,
                    inclusive = false
                )
            )
        }
    }

    private fun logout() {
        if (_state.value.logoutInProgress) return

        val role = _state.value.userRole
        if (role == UserRole.TEACHER) {
            logoutTeacher()
        } else {
            logoutStudent()
        }
    }

    private fun logoutTeacher() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDrawerOpen = false,
                    logoutInProgress = true,
                    logoutErrorMessage = null,
                    isHomeActionsOpen = false,
                    activeHomeSelector = null
                )
            }

            val logoutResult = runCatching {
                teacherRegistrationRepository.clearSavedTeacherProfile()
                scheduleRepository.clearTeacherSessionState()
                authRepository.clearActiveSession()
                preferencesDataSource.setStudentScheduleOnlyModeEnabled(false)
                preferencesDataSource.setUserRole(UserRole.STUDENT)
            }

            _state.update {
                it.copy(
                    logoutInProgress = false,
                    userRole = UserRole.STUDENT,
                    currentRoute = if (logoutResult.isSuccess) AppRoute.Start.route else it.currentRoute,
                    studentDisplayName = if (logoutResult.isSuccess) null else it.studentDisplayName,
                    logoutErrorMessage = logoutResult.exceptionOrNull()?.message,
                    bootstrapErrorMessage = if (logoutResult.isSuccess) null else it.bootstrapErrorMessage,
                    offlineLoginRequired = false,
                    isStudentScheduleOnlyMode = false
                )
            }
            if (logoutResult.isSuccess) {
                pendingStudentBootstrapAccountKey = null
                _commands.emit(AppNavigationCommand.ClearBackStackAndNavigate(AppRoute.Start.route))
            }
        }
    }

    private fun logoutStudent() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDrawerOpen = false,
                    logoutInProgress = true,
                    logoutErrorMessage = null,
                    isHomeActionsOpen = false,
                    activeHomeSelector = null
                )
            }

            val logoutResult = runCatching {
                if (_state.value.isStudentScheduleOnlyMode) {
                    authRepository.clearActiveSession()
                    preferencesDataSource.clearStudentScheduleOnlyMode()
                    preferencesDataSource.setUserRole(UserRole.STUDENT)
                } else {
                    authRepository.logout()
                }
            }
            val logoutError = logoutResult.exceptionOrNull()?.message
            _state.update {
                it.copy(
                    logoutInProgress = false,
                    userRole = UserRole.STUDENT,
                    currentRoute = if (logoutResult.isSuccess) AppRoute.Start.route else it.currentRoute,
                    studentDisplayName = if (logoutResult.isSuccess) null else it.studentDisplayName,
                    logoutErrorMessage = logoutError,
                    bootstrapErrorMessage = if (logoutResult.isSuccess) null else it.bootstrapErrorMessage,
                    offlineLoginRequired = false,
                    isStudentScheduleOnlyMode = if (logoutResult.isSuccess) false else it.isStudentScheduleOnlyMode
                )
            }
            if (logoutResult.isSuccess) {
                pendingStudentBootstrapAccountKey = null
                _commands.emit(AppNavigationCommand.ClearBackStackAndNavigate(AppRoute.Start.route))
            }
        }
    }

    private fun String.toStudentBootstrapAccountKey(): String? {
        return trim().lowercase().takeIf { it.isNotBlank() }
    }

    private companion object {
        const val CACHE_CURRENT_AND_PREVIOUS_WEEK_ID = "cache_current_previous_week"
        const val LESSON_NOTIFICATIONS_ID = "lesson_notifications"
        const val SHOW_MISMATCHED_SUBGROUP_LESSONS_ID = "show_mismatched_subgroup_lessons"
        const val OFFLINE_MESSAGE = "Нет подключения к интернету. Показываем сохраненные данные."

        fun settingsFrom(
            role: UserRole,
            cacheCurrentAndPreviousWeek: Boolean,
            lessonNotificationsEnabled: Boolean,
            showMismatchedSubgroupLessons: Boolean
        ): List<SettingsItem> {
            val notificationsSetting = SettingsItem(
                id = LESSON_NOTIFICATIONS_ID,
                title = "Уведомления о парах",
                checked = lessonNotificationsEnabled
            )

            return when (role) {
                UserRole.TEACHER -> listOf(notificationsSetting)
                UserRole.STUDENT -> listOf(
                    SettingsItem(
                        id = CACHE_CURRENT_AND_PREVIOUS_WEEK_ID,
                        title = "Сохранять текущую и следующую неделю в кэш",
                        checked = cacheCurrentAndPreviousWeek
                    ),
                    SettingsItem(
                        id = SHOW_MISMATCHED_SUBGROUP_LESSONS_ID,
                        title = "Показывать пары другой подгруппы",
                        checked = showMismatchedSubgroupLessons
                    ),
                    notificationsSetting
                )
            }
        }
    }
}
