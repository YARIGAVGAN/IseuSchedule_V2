package com.example.scheduleiseu.feature.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.scheduleiseu.R
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppDimens
import com.example.scheduleiseu.core.designsystem.theme.AppElevation
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.designsystem.theme.AppSpacing
import com.example.scheduleiseu.core.ui.animation.AppCrossfade
import com.example.scheduleiseu.core.ui.animation.FadeVisibility
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.core.ui.animation.appRevealMotion

enum class DrawerDestination {
    PERFORMANCE,
    HOME,
    SETTINGS,
    INFO,
    LOGOUT,
}

@Composable
fun MenuDrawerOverlay(
    isOpen: Boolean,
    onClose: () -> Unit,
    onDestinationClick: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
    profileState: MenuProfileUiState = MenuProfileUiState(),
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val drawerWidth = minOf(screenWidth * (380f / 412f), AppDimens.DrawerWidth)

    Box(modifier = modifier.fillMaxSize()) {
        FadeVisibility(
            visible = isOpen,
            modifier = Modifier.matchParentSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Overlay)
                    .clickable(onClick = onClose),
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            modifier = Modifier.fillMaxSize(),
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeIn(
                animationSpec = tween(durationMillis = 400),
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(
                    durationMillis = 250,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeOut(
                animationSpec = tween(durationMillis = 160),
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.TopStart,
            ) {
                DrawerPanel(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(drawerWidth)
                        .padding(
                            top = WindowInsets.statusBars
                                .asPaddingValues()
                                .calculateTopPadding(),
                        )
                        .navigationBarsPadding(),
                    profileState = profileState,
                    onClose = onClose,
                    onDestinationClick = onDestinationClick,
                )
            }
        }
    }
}

@Composable
private fun DrawerPanel(
    modifier: Modifier = Modifier,
    profileState: MenuProfileUiState,
    onClose: () -> Unit,
    onDestinationClick: (DrawerDestination) -> Unit,
) {
    Card(
        modifier = modifier
            .appAnimatedContentSize()
            .appRevealMotion(initialOffsetX = -24f, initialOffsetY = 0f),
        shape = AppShapes.drawer,
        colors = CardDefaults.cardColors(containerColor = AppColors.DrawerSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.high),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ProfileHeaderCard(
                state = profileState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            DrawerMenuSection(
                isTeacherMode = profileState.isTeacherMode,
                isScheduleOnlyMode = profileState.isScheduleOnlyMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                onClose = onClose,
                onDestinationClick = onDestinationClick,
            )
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    state: MenuProfileUiState,
    modifier: Modifier = Modifier,
) {
    val detailsText = when {
        state.isLoading -> "Загрузка профиля..."
        !state.errorMessage.isNullOrBlank() -> state.errorMessage
        else -> state.details
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .appAnimatedContentSize()
                .padding(top = if (state.showPhoto) AppDimens.AvatarOffset else 0.dp)
                .appRevealMotion(initialOffsetY = 10f),
            shape = AppShapes.large,
            colors = CardDefaults.cardColors(containerColor = AppColors.DarkGreen),
            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.medium),
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Column(
                    modifier = Modifier
                        .padding(
                            start = if (state.showPhoto) {
                                AppDimens.AvatarWidth + AppSpacing.md
                            } else {
                                0.dp
                            },
                        )
                        .then(
                            if (state.showPhoto) {
                                Modifier.height(
                                    AppDimens.AvatarHeight -
                                            AppDimens.AvatarOffset +
                                            AppSpacing.sm,
                                )
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Text(
                        text = state.fullName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = AppColors.White,
                        ),
                        maxLines = 3,
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    Text(
                        text = state.groupOrPosition,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppColors.White.copy(alpha = 0.9f),
                        ),
                        maxLines = 2,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = detailsText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = AppColors.White,
                    ),
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (state.averageScore != null) {
                        "Средний балл: ${state.averageScore}"
                    } else {
                        "Роль: ${state.role}"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = AppColors.White,
                    ),
                )
            }
        }

        if (state.showPhoto) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
                    .align(Alignment.TopStart),
            ) {
                Card(
                    shape = AppShapes.large,
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = AppElevation.medium,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .width(AppDimens.AvatarWidth)
                            .height(AppDimens.AvatarHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppCrossfade(
                            targetState = state.photoBitmap,
                            label = "profilePhoto",
                        ) { photoBitmap ->
                            if (photoBitmap != null) {
                                Image(
                                    bitmap = photoBitmap.asImageBitmap(),
                                    contentDescription = "Фото профиля",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ise_logo),
                                    contentDescription = "Фото профиля",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(AppSpacing.md),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuSection(
    isTeacherMode: Boolean,
    isScheduleOnlyMode: Boolean,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onDestinationClick: (DrawerDestination) -> Unit,
) {
    fun handleDestinationClick(destination: DrawerDestination) {
        onDestinationClick(destination)
        onClose()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = AppSpacing.sm),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!isTeacherMode && !isScheduleOnlyMode) {
                MenuButtonItem(
                    iconRes = R.drawable.study,
                    text = "Успеваемость",
                ) {
                    handleDestinationClick(DrawerDestination.PERFORMANCE)
                }
            }

            MenuButtonItem(
                iconRes = R.drawable.schedule,
                text = "Расписание",
            ) {
                handleDestinationClick(DrawerDestination.HOME)
            }

            if (!isTeacherMode) {
                MenuButtonItem(
                    iconRes = R.drawable.info,
                    text = "О приложении",
                ) {
                    handleDestinationClick(DrawerDestination.INFO)
                }
            }

            MenuButtonItem(
                iconRes = R.drawable.settings,
                text = "Настройки",
            ) {
                handleDestinationClick(DrawerDestination.SETTINGS)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalDivider(
            thickness = 1.dp,
            color = AppColors.Divider,
        )

        Spacer(modifier = Modifier.height(10.dp))

        MenuButtonItem(
            iconRes = R.drawable.logout,
            text = "Выйти",
            isLogout = true,
        ) {
            handleDestinationClick(DrawerDestination.LOGOUT)
        }
    }
}

@Composable
private fun MenuButtonItem(
    iconRes: Int,
    text: String,
    isLogout: Boolean = false,
    onClick: () -> Unit,
) {
    PressScale {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .appAnimatedContentSize()
                .appRevealMotion(initialOffsetX = -16f, initialOffsetY = 0f)
                .clickable(onClick = onClick),
            shape = AppShapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (isLogout) {
                    AppColors.ErrorSurface
                } else {
                    AppColors.HeaderGreen
                },
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = AppElevation.none,
            ),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = AppSpacing.md,
                    vertical = 14.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(
                        if (isLogout) {
                            AppColors.Logout
                        } else {
                            AppColors.TextDark
                        },
                    ),
                )

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (isLogout) {
                            AppColors.Logout
                        } else {
                            AppColors.White
                        },
                    ),
                )
            }
        }
    }
}
