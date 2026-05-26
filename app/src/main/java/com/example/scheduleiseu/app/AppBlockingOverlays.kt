package com.example.scheduleiseu.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.ui.animation.AppCrossfade
import com.example.scheduleiseu.core.ui.animation.FadeSlideVisibility
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize

@Composable
internal fun StartupBlockingOverlay(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = AppColors.White) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = AppColors.HeaderGreen
            )
        }
    }
}

@Composable
internal fun OfflineLoginBanner(
    visible: Boolean,
    onClick: () -> Unit
) {
    FadeSlideVisibility(visible = visible) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            PressScale {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable(onClick = onClick),
                    color = AppColors.Error,
                    shape = AppShapes.extraLarge
                ) {
                    Text(
                        text = "Оффлайн, нажмите, чтобы войти",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
internal fun BootstrapBlockingOverlay(
    visible: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit
) {
    FadeSlideVisibility(visible = visible) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            color = AppColors.White.copy(alpha = 0.96f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appAnimatedContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AppCrossfade(targetState = errorMessage, label = "bootstrapState") { message ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (message == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = AppColors.HeaderGreen
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Загружаем данные для первого входа",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppColors.ScreenTitle,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Профиль, расписание и успеваемость сохраняются в кэш",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.ScreenTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "Не удалось завершить первичную загрузку",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppColors.ScreenTitle,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.ScreenTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                PressScale {
                                    Button(
                                        onClick = onRetryClick,
                                        modifier = Modifier
                                            .fillMaxWidth(0.72f)
                                            .height(52.dp),
                                        shape = AppShapes.extraLarge,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppColors.HeaderGreen,
                                            contentColor = AppColors.White
                                        )
                                    ) {
                                        Text(text = "Повторить", style = MaterialTheme.typography.bodyLarge)
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
