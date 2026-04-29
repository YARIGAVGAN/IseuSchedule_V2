package com.example.scheduleiseu.feature.navigation

sealed interface AppNavigationCommand {
    data class Navigate(
        val route: String,
        val popUpToRoute: String? = null,
        val inclusive: Boolean = false,
        val launchSingleTop: Boolean = true
    ) : AppNavigationCommand

    data class ClearBackStackAndNavigate(val route: String) : AppNavigationCommand

    data class ShowToast(
        val message: String,
        val durationMillis: Long = 5_000L
    ) : AppNavigationCommand
}
