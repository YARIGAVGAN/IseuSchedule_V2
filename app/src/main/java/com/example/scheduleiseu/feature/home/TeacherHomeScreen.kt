package com.example.scheduleiseu.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppFontFamily
import com.example.scheduleiseu.core.designsystem.theme.AppSpacing
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.AppCrossfade
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.domain.core.model.Lesson
import com.example.scheduleiseu.domain.core.model.ScheduleDay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeacherHomeScreen(
    state: ScheduleUiState,
    onMenuClick: () -> Unit = {},
    onScreenSettingsClick: () -> Unit = {},
    onDayClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ScheduleHomeScaffold(
        onMenuClick = onMenuClick,
        onResetTemporaryContextClick = {},
        onScreenSettingsClick = onScreenSettingsClick,
        selectedDay = state.selectedDay,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(contentPadding = PaddingValues(bottom = AppSpacing.lg)) {
                scheduleStickyHeader(
                    days = state.days,
                    selectedDay = state.selectedDay,
                    onDayClick = onDayClick
                )
                if (state.errorMessage != null && state.days.isEmpty()) {
                    item(key = "empty-error") {
                        EmptyScheduleMessage(
                            message = state.errorMessage,
                            modifier = Modifier.padding(top = AppSpacing.lg)
                        )
                    }
                }
                item { ScheduleStatusBanner(state = state) }
                item(key = "teacher-lessons-${state.selectedDay?.date.orEmpty()}") {
                    AppCrossfade(
                        targetState = state.lessonsForSelectedDay,
                        label = "teacherScheduleLessons",
                    ) { lessons ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .appAnimatedContentSize()
                        ) {
                            lessons.forEach { lesson ->
                                TeacherLessonCard(
                                    lesson = lesson,
                                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                                )
                            }
                        }
                    }
                }
            }

            AppCrossfade(
                targetState = when {
                    state.isLoading && state.days.isEmpty() -> "loading"
                    else -> "content"
                },
                label = "teacherScheduleState",
            ) { targetState ->
                when (targetState) {
                    "loading" -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TeacherHomeScreenPreview() {
    val day = ScheduleDay(
        title = "Понедельник",
        date = "12.03.2026",
        lessons = listOf(
            Lesson(
                id = "teacher-preview-1",
                title = "Компьютерное моделирование биологически активных веществ",
                type = "практ. зан.",
                classroom = "ауд. 309",
                startTime = "08:30",
                endTime = "09:50",
                subgroup = "ФЭМ В51МД1/1",
                note = "ул. Долгобродская, 23/1",
                topic = "Тема занятия"
            )
        ),
        isCurrentDay = true
    )
    ScheduleIsEuTheme {
        TeacherHomeScreen(
            state = ScheduleUiState(
                days = listOf(day),
                selectedDay = day,
                lessonsForSelectedDay = day.lessons
            )
        )
    }
}
