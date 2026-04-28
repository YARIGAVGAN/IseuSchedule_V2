package com.example.scheduleiseu.feature.navigation

sealed class AppRoute(val route: String) {
    data object Start : AppRoute("start")
    data object StudentLogin : AppRoute("student_login")
    data object StudentRegistration : AppRoute("student_registration")
    data object TeacherRegistration : AppRoute("teacher_registration")
    data object Home : AppRoute("home")
    data object Performance : AppRoute("performance")
    data object Info : AppRoute("info")
    data object Settings : AppRoute("settings")

    companion object {
        val mainRoutes = setOf(
            Home.route,
            Performance.route,
            Info.route,
            Settings.route
        )
    }
}
