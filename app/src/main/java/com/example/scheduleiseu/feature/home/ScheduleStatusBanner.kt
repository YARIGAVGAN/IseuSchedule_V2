package com.example.scheduleiseu.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppFontFamily
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.designsystem.theme.AppSpacing
import com.example.scheduleiseu.core.ui.animation.FadeSlideVisibility
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize

@Composable
internal fun ScheduleStatusBanner(
    state: ScheduleUiState,
    modifier: Modifier = Modifier
) {
    val message = when {
        state.isOfflineMode -> state.offlineMessage ?: OFFLINE_MESSAGE
        state.isLoading && state.days.isNotEmpty() -> when (state.loadingStage) {
            ScheduleLoadingStage.BackgroundRefresh -> "Обновляем расписание в фоне"
            ScheduleLoadingStage.Selection -> "Загружаем выбранное расписание"
            ScheduleLoadingStage.RetryAfterNetworkRestore -> "Пробуем обновить после восстановления сети"
            ScheduleLoadingStage.Initial, null -> "Загружаем расписание"
        }
        else -> null
    }

    FadeSlideVisibility(visible = message != null, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                .appAnimatedContentSize()
                .background(
                    color = if (state.isOfflineMode) AppColors.Error else AppColors.DarkGreen,
                    shape = AppShapes.extraLarge
                )
                .border(
                    width = 1.dp,
                    color = AppColors.White.copy(alpha = 0.18f),
                    shape = AppShapes.extraLarge
                )
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message.orEmpty(),
                color = AppColors.White,
                fontFamily = AppFontFamily
            )
        }
    }
}

internal const val OFFLINE_MESSAGE = "Нет подключения к интернету. Показываем сохраненные данные."
