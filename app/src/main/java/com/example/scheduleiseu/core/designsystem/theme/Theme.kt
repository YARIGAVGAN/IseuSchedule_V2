package com.example.scheduleiseu.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = AppColors.HeaderGreen,
    secondary = AppColors.DarkGreen,
    tertiary = AppColors.LightGreen,
    background = AppColors.White,
    surface = AppColors.White,
    onPrimary = AppColors.White,
    onSecondary = AppColors.White,
    onTertiary = AppColors.Black,
    onBackground = AppColors.ScreenText,
    onSurface = AppColors.ScreenText,
    error = AppColors.Error,
)

@Composable
fun ScheduleIsEuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content,
    )
}
