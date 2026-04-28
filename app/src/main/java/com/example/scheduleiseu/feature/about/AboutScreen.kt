package com.example.scheduleiseu.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.core.designsystem.components.AppCard
import com.example.scheduleiseu.core.designsystem.components.AppScreenScaffold
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppSpacing
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme

private data class AboutSection(
    val title: String,
    val description: String,
)

private val aboutSections = listOf(
    AboutSection(
        title = "Что это за приложение",
        description = "Schedule ISEU помогает студентам и преподавателям быстро находить актуальное расписание, следить за новостями учебного процесса и держать под рукой важную информацию в одном интерфейсе.",
    ),
    AboutSection(
        title = "Что доступно внутри",
        description = "В приложении собраны расписание занятий успеваемость и персональные настройки отображения. Доступны уведомления, помогающие определить следующую пару за 15 минут до ее начала (если пара первая), и сообщения о следующей паре в конце предыдущей. ",
    )
)

@Composable
fun AboutScreen(
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(
        title = "О приложении",
        onMenuClick = onMenuClick,
        modifier = modifier,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            item {
                AboutHeroCard(
                    modifier = Modifier
                        .padding(top = AppSpacing.md)
                        .padding(horizontal = AppSpacing.md),
                )
            }

            items(aboutSections, key = AboutSection::title) { section ->
                AboutSectionCard(
                    section = section,
                    modifier = Modifier.padding(horizontal = AppSpacing.md),
                )
            }

            item {
                AboutFooterCard(
                    modifier = Modifier.padding(horizontal = AppSpacing.md),
                )
            }
        }
    }
}

@Composable
private fun AboutHeroCard(modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier,
        containerColor = AppColors.DarkGreen,
        contentPadding = PaddingValues(AppSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Schedule ISEU",
                style = MaterialTheme.typography.titleLarge.copy(color = AppColors.White),
            )

            Text(
                text = "Университетский помощник для расписания и учебной информации.",
                style = MaterialTheme.typography.bodyLarge.copy(color = AppColors.White),
            )

            Text(
                text = "Версия интерфейса 2.0",
                style = MaterialTheme.typography.bodySmall.copy(color = AppColors.White.copy(alpha = 0.85f)),
            )
        }
    }
}

@Composable
private fun AboutSectionCard(
    section: AboutSection,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier,
        containerColor = AppColors.White,
        contentPadding = PaddingValues(AppSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium.copy(color = AppColors.ScreenTitle),
            )

            Text(
                text = section.description,
                style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.ScreenText),
            )
        }
    }
}

@Composable
private fun AboutFooterCard(modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier,
        containerColor = AppColors.LightGreen,
        contentPadding = PaddingValues(AppSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Сделано для быстрого доступа к учебным данным",
                    style = MaterialTheme.typography.titleMedium.copy(color = AppColors.ScreenTitle),
                )
                Text(
                    text = "Раздел повторяет цветовую палитру и паттерны приложения, поэтому выглядит как естественная часть продукта.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.ScreenText),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AboutScreenPreview() {
    ScheduleIsEuTheme {
        AboutScreen()
    }
}
