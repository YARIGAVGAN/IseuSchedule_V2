package com.example.scheduleiseu.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.scheduleiseu.core.designsystem.components.AppCard
import com.example.scheduleiseu.core.designsystem.components.AppScreenScaffold
import com.example.scheduleiseu.core.designsystem.components.AppSettingsSwitch
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppSpacing
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.SoftAppear
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.domain.model.SettingsItem

@Composable
fun SettingsScreen(
    items: List<SettingsItem>,
    onMenuClick: () -> Unit = {},
    onRegistrationDataClick: (() -> Unit)? = null,
    onItemCheckedChange: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(
        title = "Настройки",
        onMenuClick = onMenuClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            items.forEach { item ->
                SoftAppear {
                    SettingsToggleCard(
                        title = item.title,
                        checked = item.checked,
                        onCheckedChange = { checked -> onItemCheckedChange(item.id, checked) },
                    )
                }
            }

            onRegistrationDataClick?.let { onClick ->
                SoftAppear {
                    SettingsActionCard(
                        title = "Изменение регистрационных данных",
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    PressScale(modifier = modifier.fillMaxWidth()) {
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .appAnimatedContentSize()
                .clickable { onCheckedChange(!checked) },
        containerColor = AppColors.DarkGreen,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = AppSpacing.md,
            vertical = AppSpacing.sm,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppSettingsSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(color = AppColors.White),
                modifier = Modifier
                    .padding(start = AppSpacing.sm)
                    .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressScale(modifier = modifier.fillMaxWidth()) {
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            containerColor = AppColors.DarkGreen,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = AppSpacing.md,
                vertical = AppSpacing.md,
            ),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(color = AppColors.White),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SettingsScreenPreview() {
    ScheduleIsEuTheme {
        SettingsScreen(
            items = listOf(
                SettingsItem("cache_current_previous_week", "Сохранять текущую и следующую неделю в кэш", false)
            )
        )
    }
}
