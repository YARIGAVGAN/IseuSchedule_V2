package com.example.scheduleiseu.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scheduleiseu.R
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppFontFamily
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.designsystem.theme.AppSpacing
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.AppCrossfade
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.core.ui.animation.appRevealMotion
import com.example.scheduleiseu.domain.core.model.Lesson
import com.example.scheduleiseu.domain.core.model.ScheduleDay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: ScheduleUiState,
    onMenuClick: () -> Unit = {},
    onResetTemporaryContextClick: () -> Unit = {},
    onScreenSettingsClick: () -> Unit = {},
    onDayClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ScheduleHomeScaffold(
        onMenuClick = onMenuClick,
        onResetTemporaryContextClick = onResetTemporaryContextClick,
        onScreenSettingsClick = onScreenSettingsClick,
        selectedDay = state.selectedDay,
        isTemporaryContext = state.isTemporaryContext,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(contentPadding = PaddingValues(bottom = AppSpacing.lg)) {
                scheduleStickyHeader(
                    days = state.days,
                    selectedDay = state.selectedDay,
                    onDayClick = onDayClick,
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
                val isTeacherScheduleView =
                    state.isTemporaryContext &&
                            (
                                    !state.selectedTeacherName.isNullOrBlank() ||
                                            state.scheduleContext?.selectedTeacherId != null
                                    )
                item(key = "lessons-${state.selectedDay?.date.orEmpty()}-$isTeacherScheduleView") {
                    AppCrossfade(
                        targetState = state.lessonsForSelectedDay,
                        label = "scheduleLessons",
                    ) { lessons ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .appAnimatedContentSize()
                        ) {
                            lessons.forEach { lesson ->
                                if (isTeacherScheduleView) {
                                    TeacherLessonCard(
                                        lesson = lesson,
                                        modifier = Modifier.padding(top = AppSpacing.md),
                                    )
                                } else {
                                    LessonCard(
                                        lesson = lesson,
                                        modifier = Modifier.padding(top = AppSpacing.md),
                                    )
                                }
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
                label = "scheduleState",
            ) { targetState ->
                when (targetState) {
                    "loading" -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
internal fun EmptyScheduleMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .padding(horizontal = AppSpacing.lg),
        color = AppColors.White,
        fontFamily = AppFontFamily,
    )
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    modifier: Modifier = Modifier,
) {
    val locationLine = listOfNotNull(
        lesson.classroom?.trim()?.takeIf { it.isNotBlank() },
        lesson.note?.trim()?.takeIf { it.isNotBlank() }
    ).joinToString(", ")
    val subgroupLine = lesson.subgroup.orEmpty().trim().toSubgroupDisplayText()

    PressScale(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md)
            .appRevealMotion(initialOffsetY = 16f, initialScale = 0.992f),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .appAnimatedContentSize()
                    .background(color = AppColors.LessonCard, shape = AppShapes.medium)
                    .border(width = 3.dp, color = AppColors.LessonBorder, shape = AppShapes.medium),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .wrapContentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.header_card),
                        contentDescription = "Тип занятия",
                        modifier = Modifier
                            .matchParentSize()
                            .padding(2.dp),
                    )

                    TextLine(
                        text = lesson.type.orEmpty(),
                        fontSize = 18.sp,
                        color = AppColors.Black,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 9.dp, end = 9.dp, top = 29.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LessonTimeColumn(
                        startTime = lesson.startTime,
                        endTime = lesson.endTime.orEmpty(),
                        lineColor = AppColors.White,
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        TextLine(
                            text = lesson.title,
                            fontSize = 16.sp,
                            color = AppColors.White,
                        )
                        TextLine(
                            text = lesson.teacherName.orEmpty(),
                            fontSize = 14.sp,
                            color = AppColors.White,
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextLine(
                            text = locationLine,
                            fontSize = 16.sp,
                            color = AppColors.White,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        TextLine(
                            text = subgroupLine,
                            fontSize = 16.sp,
                            color = AppColors.White,
                        )
                    }
                }
            }
        }
    }
}

private fun String.toSubgroupDisplayText(): String {
    val normalized = trim()
    val subgroup = Regex("""^([12])$""").matchEntire(normalized)?.groupValues?.getOrNull(1)
        ?: Regex("""^([12])\s*(?:п\/гр|подгр\.?|подгруппа)$""", RegexOption.IGNORE_CASE)
            .matchEntire(normalized)
            ?.groupValues
            ?.getOrNull(1)
    return subgroup?.let { "$it подгр." } ?: normalized
}

@Composable
private fun LessonTimeColumn(
    startTime: String,
    endTime: String,
    lineColor: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextLine(text = startTime, fontSize = 16.sp, color = AppColors.White)

        Box(
            modifier = Modifier
                .padding(vertical = 2.dp)
                .width(2.dp)
                .height(6.dp)
                .background(lineColor),
        )

        TextLine(text = endTime, fontSize = 16.sp, color = AppColors.White)
    }
}

@Composable
private fun TextLine(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        color = color,
        fontFamily = AppFontFamily,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HomeScreenPreview() {
    ScheduleIsEuTheme {
        HomeScreen(
            state = ScheduleUiState(
                days = listOf(
                    ScheduleDay(title = "Понедельник", date = "12.03.2026", lessons = emptyList()),
                    ScheduleDay(title = "Вторник", date = "13.03.2026", lessons = emptyList())
                ),
                selectedDay = ScheduleDay(title = "Понедельник", date = "12.03.2026", lessons = emptyList()),
                lessonsForSelectedDay = listOf(
                    Lesson(
                        id = "1",
                        title = "Объектно-ориентированное проектирование и программирование",
                        type = "практ.зан",
                        teacherName = "пр. Куканков Григорий Петрович",
                        classroom = "ауд. 101",
                        startTime = "10:25",
                        endTime = "11:45",
                        note = "К1",
                    )
                )
            )
        )
    }
}
