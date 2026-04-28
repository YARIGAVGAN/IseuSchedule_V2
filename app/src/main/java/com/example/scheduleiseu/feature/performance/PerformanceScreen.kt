package com.example.scheduleiseu.feature.performance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.R
import com.example.scheduleiseu.core.designsystem.components.AppScreenScaffold
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.designsystem.theme.AppSpacing
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.AppCrossfade
import com.example.scheduleiseu.core.ui.animation.AppMotion
import com.example.scheduleiseu.core.ui.animation.FadeSlideVisibility
import com.example.scheduleiseu.core.ui.animation.FloatUpVisibility
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.SoftAppear
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.core.ui.animation.appClickable
import com.example.scheduleiseu.domain.model.Course
import com.example.scheduleiseu.domain.model.Session
import com.example.scheduleiseu.domain.model.Subject

@Composable
fun PerformanceScreen(
    state: PerformanceUiState,
    onMenuClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    onSemesterClick: (semesterId: String, semesterTitle: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(
        title = "Успеваемость",
        onMenuClick = onMenuClick,
        modifier = modifier,
    ) {
        SoftAppear(modifier = Modifier.fillMaxSize()) {
            AppCrossfade(
                targetState = when {
                    state.isLoading -> "loading"
                    state.errorMessage != null && state.course == null -> "error"
                    state.isEmpty -> "empty"
                    state.course != null -> "content"
                    else -> "empty"
                },
                label = "performanceState",
            ) { targetState ->
                when (targetState) {
                    "loading" -> PerformanceLoadingState()
                    "error" -> PerformanceMessageState(
                        message = state.errorMessage.orEmpty(),
                        onClick = onRetryClick
                    )
                    "empty" -> PerformanceMessageState(
                        message = "Данные об успеваемости не найдены",
                        onClick = onRetryClick
                    )
                    "content" -> state.course?.let { course ->
                        PerformanceContent(
                            course = course,
                            selectedSemesterId = state.selectedSemesterId,
                            loadingSemesterId = state.loadingSemesterId,
                            errorMessage = state.errorMessage,
                            onRetryClick = onRetryClick,
                            onSemesterClick = onSemesterClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceContent(
    course: Course,
    selectedSemesterId: String?,
    loadingSemesterId: String?,
    errorMessage: String?,
    onRetryClick: () -> Unit,
    onSemesterClick: (semesterId: String, semesterTitle: String) -> Unit,
) {
    var courseExpanded by rememberSaveable { mutableStateOf(true) }
    var expandedSessionId by rememberSaveable(selectedSemesterId) { mutableStateOf(selectedSemesterId) }

    Column(
        modifier = Modifier
            .appAnimatedContentSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = AppSpacing.lg),
    ) {
        ExpandableCard(
            title = course.title,
            subtitle = "Средний балл: ${course.averageScore}",
            expanded = courseExpanded,
            expandedContainerColor = AppColors.MidGreen,
            onClick = {
                courseExpanded = !courseExpanded
                if (!courseExpanded) expandedSessionId = null
            },
            modifier = Modifier.padding(top = AppSpacing.md),
        )

        FadeSlideVisibility(visible = !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.White),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .appClickable(pressedScale = 0.99f, onClick = onRetryClick)
                    .padding(AppSpacing.md)
            )
        }

        FloatUpVisibility(visible = courseExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appAnimatedContentSize()
            ) {
                course.sessions.forEach { session ->
                    SoftAppear {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .appAnimatedContentSize()
                        ) {
                            val isExpanded = expandedSessionId == session.id
                            val isLoading = loadingSemesterId == session.id

                            ExpandableCard(
                                title = session.title,
                                subtitle = if (isLoading) "Загрузка..." else session.averageScore?.let { "Средний балл: ${it}" },
                                expanded = isExpanded,
                                expandedContainerColor = AppColors.DarkGreen,
                                onClick = {
                                    expandedSessionId = if (isExpanded) null else session.id
                                    if (!isExpanded) onSemesterClick(session.id, session.title)
                                },
                                modifier = Modifier.padding(top = AppSpacing.md),
                            )

                            FloatUpVisibility(visible = isExpanded) {
                                AppCrossfade(
                                    targetState = when {
                                        isLoading -> "loading"
                                        session.subjects.isEmpty() -> "empty"
                                        else -> "subjects"
                                    },
                                    label = "semesterContent",
                                ) { targetState ->
                                    when (targetState) {
                                        "loading" -> SemesterLoadingState()
                                        "empty" -> SemesterMessageState("Данные семестра не загружены")
                                        "subjects" -> SubjectsGrid(
                                            subjects = session.subjects,
                                            modifier = Modifier.padding(
                                                start = AppSpacing.md,
                                                end = AppSpacing.md,
                                                top = AppSpacing.md,
                                            ),
                                        )
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

@Composable
private fun ExpandableCard(
    title: String,
    subtitle: String?,
    expanded: Boolean,
    expandedContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = if (expanded) 0.78f else 1f,
        animationSpec = tween(durationMillis = AppMotion.ContentChangeDurationMillis, easing = AppMotion.Easing),
        label = "cardAlpha"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing),
        label = "iconRotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md)
            .appClickable(pressedScale = 0.93f, onClick = onClick)
            .appAnimatedContentSize()
            .background(
                color = if (expanded) expandedContainerColor else AppColors.DarkGreen,
                shape = AppShapes.extraLarge,
            )
            .alpha(alpha)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(color = AppColors.White),
            )

            AppCrossfade(targetState = if (expanded) null else subtitle, label = "subtitle") { visibleSubtitle ->
                if (!visibleSubtitle.isNullOrBlank()) {
                    Text(
                        text = visibleSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = AppColors.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(modifier = Modifier)
                }
            }
        }

        PressScale {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_open),
                    contentDescription = "Изменить состояние секции",
                    tint = Color.Unspecified,
                    modifier = Modifier.rotate(iconRotation),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectsGrid(
    subjects: List<Subject>,
    modifier: Modifier = Modifier,
) {
    val rows = subjects.chunked(2)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        rows.forEach { rowSubjects ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                rowSubjects.forEach { subject ->
                    SubjectCard(
                        subject = subject,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (rowSubjects.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .wrapContentHeight()
            .background(Color.Transparent, shape = AppShapes.large),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = AppColors.LightGreen, shape = AppShapes.subjectTop)
                .padding(horizontal = AppSpacing.md, vertical = 10.dp),
        ) {
            Text(
                text = subject.title,
                style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.Black),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = AppColors.BottomCard, shape = AppShapes.subjectBottom)
                .padding(horizontal = AppSpacing.md, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = subject.typeLabel,
                style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.Black),
                modifier = Modifier.weight(1f),
            )

            Text(
                text = subject.grade,
                style = MaterialTheme.typography.titleMedium.copy(color = AppColors.GradeGreen),
            )
        }
    }
}

@Composable
private fun PerformanceLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AppColors.White)
    }
}

@Composable
private fun PerformanceMessageState(
    message: String,
    onClick: () -> Unit
) {
    PressScale {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(horizontal = AppSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(color = AppColors.White),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SemesterLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AppColors.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SemesterMessageState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.White),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.md)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PerformanceScreenPreview() {
    ScheduleIsEuTheme {
        PerformanceScreen(
            state = PerformanceUiState(
                course = Course(
                    id = "course_1",
                    title = "1 курс",
                    averageScore = "7.0",
                    sessions = listOf(
                        Session(
                            id = "winter",
                            title = "Зимняя сессия",
                            subjects = listOf(
                                Subject("1", "Физика", "Экзамен", "9"),
                                Subject("2", "Основы алгоритмизации и программирования", "Экзамен", "9")
                            )
                        )
                    )
                )
            )
        )
    }
}
