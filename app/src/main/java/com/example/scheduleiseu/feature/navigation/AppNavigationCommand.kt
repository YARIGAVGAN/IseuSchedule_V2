package com.example.scheduleiseu.feature.navigation

sealed interface AppNavigationCommand {
    data class Navigate(
        val route: String,
        val popUpToRoute: String? = null,
        val inclusive: Boolean = false,
        val launchSingleTop: Boolean = true
    ) : AppNavigationCommand

    data class ClearBackStackAndNavigate(val route: String) : AppNavigationCommand
}
