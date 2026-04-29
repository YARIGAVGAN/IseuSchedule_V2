package com.example.scheduleiseu.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scheduleiseu.R
import com.example.scheduleiseu.core.designsystem.components.AppBackground
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppDimens
import com.example.scheduleiseu.core.designsystem.theme.AppFontFamily
import com.example.scheduleiseu.core.designsystem.theme.AppSpacing
import com.example.scheduleiseu.core.ui.animation.AppCrossfade
import com.example.scheduleiseu.core.ui.animation.AppMotion
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.core.ui.animation.appRevealMotion
import com.example.scheduleiseu.domain.core.model.ScheduleDay

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.scheduleStickyHeader(
    days: List<ScheduleDay> = emptyList(),
    selectedDay: ScheduleDay? = null,
    onDayClick: (String) -> Unit,
) {
    stickyHeader {
        Box {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.header_back),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            HomeDaysHeader(
                days = days,
                selectedDay = selectedDay,
                onDayClick = onDayClick,
            )
        }
    }
}

@Composable
internal fun ScheduleHomeScaffold(
    onMenuClick: () -> Unit,
    onResetTemporaryContextClick: () -> Unit,
    onScreenSettingsClick: () -> Unit,
    selectedDay: ScheduleDay? = null,
    isTemporaryContext: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AppBackground(
        backgroundRes = R.drawable.body_back,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopHeader(
                bigText = selectedDay?.date?.substringBefore('.') ?: "12",
                topSmallText = selectedDay?.title ?: "Марта",
                bottomSmallText = selectedDay?.date ?: "Понедельник",
                isTemporaryContext = isTemporaryContext,
                onMenuClick = onMenuClick,
                onResetTemporaryContextClick = onResetTemporaryContextClick,
                onScreenSettingsClick = onScreenSettingsClick,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun HomeTopHeader(
    bigText: String = "12",
    topSmallText: String = "Марта",
    bottomSmallText: String = "Понедельник",
    isTemporaryContext: Boolean = false,
    onMenuClick: () -> Unit = {},
    onResetTemporaryContextClick: () -> Unit = {},
    onScreenSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.header_back),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .appAnimatedContentSize()
                .appRevealMotion(initialOffsetY = 0f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(AppSpacing.xxl))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppCrossfade(targetState = isTemporaryContext, label = "homeHeaderLeftAction") { temporary ->
                        if (temporary) {
                            HeaderVectorIconButton(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Сбросить временный контекст",
                                onClick = onResetTemporaryContextClick,
                            )
                        } else {
                            HeaderPainterIconButton(
                                iconRes = R.drawable.icon_menu,
                                contentDescription = "Открыть меню",
                                onClick = onMenuClick,
                            )
                        }
                    }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = bigText,
                        fontSize = 60.sp,
                        lineHeight = 60.sp,
                        color = AppColors.White,
                        maxLines = 1,
                        fontFamily = AppFontFamily,
                        modifier = Modifier.appRevealMotion(initialOffsetY = 0f, initialScale = 0.99f),
                    )

                    Spacer(modifier = Modifier.width(AppSpacing.sm))

                    Column {
                        HomeHeaderCaption(topSmallText)
                        HomeHeaderCaption(bottomSmallText)
                    }
                }

                HeaderPainterIconButton(
                    iconRes = R.drawable.icon_screen_settings,
                    contentDescription = "Настройки экрана",
                    onClick = onScreenSettingsClick,
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xxl))
        }
    }
}

@Composable
private fun HeaderPainterIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.width(AppDimens.HeaderSideBoxWidth),
        contentAlignment = Alignment.Center,
    ) {
        PressScale {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(AppDimens.HeaderIconSize),
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(AppDimens.HeaderIconSize),
                    tint = Color.Unspecified,
                )
            }
        }
    }
}

@Composable
private fun HeaderVectorIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.width(AppDimens.HeaderSideBoxWidth),
        contentAlignment = Alignment.Center,
    ) {
        PressScale {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(AppDimens.HeaderIconSize),
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(AppDimens.HeaderIconSize),
                    tint = AppColors.White,
                )
            }
        }
    }
}

@Composable
private fun HomeHeaderCaption(text: String) {
    Text(
        text = text,
        fontSize = 28.sp,
        lineHeight = 30.sp,
        color = AppColors.White,
        maxLines = 1,
        fontFamily = AppFontFamily,
        modifier = Modifier.appRevealMotion(initialOffsetY = 0f, initialScale = 0.995f),
    )
}

@Composable
internal fun HomeDaysHeader(
    days: List<ScheduleDay> = emptyList(),
    selectedDay: ScheduleDay? = null,
    onDayClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val resolvedDays = (if (days.isNotEmpty()) days else defaultDays())
        .filterNot { it.title.isSundayTitle() }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            resolvedDays.forEach { day ->
                val isSelected = selectedDay?.date == day.date
                val dayBackgroundColor = animateColorAsState(
                    targetValue = if (isSelected) AppColors.White.copy(alpha = 0.18f) else Color.Transparent,
                    animationSpec = tween(durationMillis = AppMotion.ContentChangeDurationMillis, easing = AppMotion.Easing),
                    label = "dayButtonBackground",
                ).value

                PressScale(modifier = Modifier.weight(1f), pressedScale = 0.9f) {
                    TextButton(
                        onClick = { onDayClick(day.date) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(color = dayBackgroundColor)
                            .appAnimatedContentSize()
                            .appRevealMotion(initialOffsetY = 8f, initialScale = 0.99f),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = AppColors.White,
                        ),
                    ) {
                        Text(
                            text = day.title.toShortDayLabel(),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = AppFontFamily,
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 2.dp,
            color = AppColors.White,
        )
    }
}

private fun defaultDays(): List<ScheduleDay> {
    val labels = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
    return labels.mapIndexed { index, title ->
        ScheduleDay(
            title = title,
            date = "default-$index",
            lessons = emptyList(),
            isCurrentDay = false,
        )
    }
}

private fun String.isSundayTitle(): Boolean {
    return trim().lowercase() in setOf("воскресенье", "вс", "вс.")
}

private fun String.toShortDayLabel(): String {
    return when (trim().lowercase()) {
        "понедельник" -> "Пн."
        "вторник" -> "Вт."
        "среда" -> "Ср."
        "четверг" -> "Чт."
        "пятница" -> "Пт."
        "суббота" -> "Сб."
        "воскресенье" -> "Вс."
        else -> take(3)
    }
}
