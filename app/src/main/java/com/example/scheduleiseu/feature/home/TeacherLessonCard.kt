package com.example.scheduleiseu.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppDimens
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.core.ui.animation.appRevealMotion
import com.example.scheduleiseu.domain.core.model.Lesson

@Composable
fun TeacherLessonCard(
    lesson: Lesson,
    modifier: Modifier = Modifier
) {
    val (displayStartTime, displayEndTime) = lesson.resolveDisplayTime()
    val displayLocation = lesson.classroom.orEmpty().trim()

    PressScale(
        modifier = modifier
            .fillMaxWidth()
            .appRevealMotion(initialOffsetY = 16f, initialScale = 0.992f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .appAnimatedContentSize()
                .background(
                    color = AppColors.LessonCard,
                    shape = RoundedCornerShape(AppDimens.CardCornerLarge)
                )
                .border(
                    width = 3.dp,
                    color = AppColors.LessonBorder,
                    shape = AppShapes.medium
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            LessonTimeColumn(
                startTime = displayStartTime,
                endTime = displayEndTime
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val groupText = lesson.subgroup.orEmpty().trim()

                if (groupText.isNotBlank()) {
                    Text(
                        text = groupText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                lesson.topic?.takeIf { it.isNotBlank() }?.let { topic ->
                    Text(
                        text = topic,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (displayLocation.isNotBlank()) {
                    LocationRow(address = displayLocation)
                }
            }
        }
    }
}

@Composable
private fun LessonTimeColumn(
    startTime: String,
    endTime: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = startTime,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.White,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .width(2.dp)
                .height(20.dp)
                .background(AppColors.White)
        )

        Text(
            text = endTime,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.White,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun LocationRow(
    address: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = AppColors.White
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = address,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.White.copy(alpha = 0.8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Lesson.resolveDisplayTime(): Pair<String, String> {
    val start = startTime.trim()
    val end = endTime.orEmpty().trim()

    if (start.isNotBlank() || end.isNotBlank()) {
        return start to end
    }

    val raw = rawTimeRange.orEmpty()
        .replace('–', '-')
        .replace('—', '-')
        .replace('\u00A0', ' ')
        .trim()

    if (raw.isBlank()) return "" to ""

    val parts = raw
        .split("-", limit = 2)
        .map { it.trim() }

    return if (parts.size == 2) {
        parts[0] to parts[1]
    } else {
        raw to ""
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TeacherLessonCardPreview() {
    ScheduleIsEuTheme {
        TeacherLessonCard(
            lesson = Lesson(
                id = "teacher-card-preview",
                title = "Компьютерное моделирование биологически активных веществ",
                type = "практ. зан.",
                teacherName = null,
                classroom = "ауд. 309",
                startTime = "08:30",
                endTime = "09:50",
                subgroup = "ФЭМ В51МД1/1",
                note = "ул. Долгобродская, 23/1",
                topic = "Тема занятия",
                rawTimeRange = "08:30-09:50"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
