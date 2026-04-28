package com.example.scheduleiseu.feature.auth.teacherregistration

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.R
import com.example.scheduleiseu.core.designsystem.components.AppCard
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppDimens
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.AppCrossfade
import com.example.scheduleiseu.core.ui.animation.FadeSlideVisibility
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.SoftAppear
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize

@Immutable
data class TeacherRegistrationUiState(
    val selectedTeacherName: String? = null,
    val isLoading: Boolean = false,
    val controlsEnabled: Boolean = true,
    val errorMessage: String? = null
)

sealed interface TeacherRegistrationAction {
    data object SearchTeacherClicked : TeacherRegistrationAction
    data object CreateAccountClicked : TeacherRegistrationAction
    data object ContinueWithoutRegistrationClicked : TeacherRegistrationAction
}

@Composable
fun TeacherRegistrationScreen(
    state: TeacherRegistrationUiState,
    onAction: (TeacherRegistrationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val canCreateAccount = state.selectedTeacherName != null && state.controlsEnabled && !state.isLoading
    val formOffset by animateFloatAsState(
        targetValue = if (state.isLoading) -18f else 0f,
        label = "teacherRegistrationFormOffset"
    )

    Surface(modifier = modifier.fillMaxSize(), color = AppColors.White) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.app_back),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.OuterPadding, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(36.dp))

                SoftAppear {
                    Text(
                        text = "Регистрация аккаунта",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.ScreenTitle,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SoftAppear {
                    Text(
                        text = "Выберите преподавателя через поиск, затем создайте аккаунт",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.ScreenTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SoftAppear {
                    AppCard(
                        modifier = Modifier
                            .appAnimatedContentSize()
                            .graphicsLayer { translationY = formOffset },
                        containerColor = AppColors.DarkGreen,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.appAnimatedContentSize(),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.SectionSpacing)
                        ) {
                            Text(
                                text = "Преподаватель",
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.White
                            )

                            Text(
                                text = "Поиск открывает список преподавателей. После выбора аккаунт будет привязан к выбранному сотруднику.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.White.copy(alpha = 0.78f)
                            )

                            SearchTeacherButton(
                                enabled = state.controlsEnabled && !state.isLoading,
                                selectedTeacherName = state.selectedTeacherName,
                                onClick = { onAction(TeacherRegistrationAction.SearchTeacherClicked) }
                            )

                            TeacherSelectionCard(selectedTeacherName = state.selectedTeacherName)

                            FadeSlideVisibility(visible = !state.errorMessage.isNullOrBlank()) {
                                ErrorCard(message = state.errorMessage.orEmpty())
                            }

                            PressScale(enabled = canCreateAccount) {
                                Button(
                                    onClick = { onAction(TeacherRegistrationAction.CreateAccountClicked) },
                                    enabled = canCreateAccount,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = AppShapes.extraLarge,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.HeaderGreen,
                                        contentColor = AppColors.White,
                                        disabledContainerColor = AppColors.HeaderGreen.copy(alpha = 0.45f),
                                        disabledContentColor = AppColors.White.copy(alpha = 0.68f)
                                    )
                                ) {
                                    AppCrossfade(targetState = state.isLoading, label = "teacherRegistrationButton") { loading ->
                                        if (loading) {
                                            CircularProgressIndicator(color = AppColors.White, strokeWidth = 2.dp)
                                        } else {
                                            Text(text = "Создать аккаунт", style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SoftAppear {
                    PressScale(enabled = state.controlsEnabled && !state.isLoading) {
                        TextButton(
                            onClick = { onAction(TeacherRegistrationAction.ContinueWithoutRegistrationClicked) },
                            enabled = state.controlsEnabled && !state.isLoading
                        ) {
                            Text(
                                text = "Продолжить без регистрации",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.ScreenTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTeacherButton(
    enabled: Boolean,
    selectedTeacherName: String?,
    onClick: () -> Unit
) {
    val title = if (selectedTeacherName == null) "Найти преподавателя" else "Изменить преподавателя"

    PressScale(enabled = enabled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = AppShapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.White,
                contentColor = AppColors.FieldText,
                disabledContainerColor = AppColors.White.copy(alpha = 0.8f),
                disabledContentColor = AppColors.FieldText.copy(alpha = 0.5f)
            )
        ) {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            Text(text = "  $title", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun TeacherSelectionCard(selectedTeacherName: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .appAnimatedContentSize()
            .background(color = AppColors.White, shape = AppShapes.extraLarge)
            .border(width = 1.dp, color = AppColors.FieldBorder, shape = AppShapes.extraLarge)
            .padding(14.dp)
    ) {
        AppCrossfade(targetState = selectedTeacherName, label = "teacherSelection") { teacherName ->
            if (teacherName == null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Преподаватель ещё не выбран",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.FieldText
                    )
                    Text(
                        text = "Нажмите кнопку поиска выше, чтобы найти сотрудника по имени.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.FieldText.copy(alpha = 0.7f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Выбранный преподаватель",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.FieldText.copy(alpha = 0.72f)
                    )
                    Text(
                        text = teacherName,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.FieldText
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = AppColors.ErrorSurface, shape = AppShapes.extraLarge)
            .border(width = 1.dp, color = AppColors.Error.copy(alpha = 0.5f), shape = AppShapes.extraLarge)
            .padding(12.dp)
    ) {
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = AppColors.White)
    }
}

@Preview(showBackground = true)
@Composable
private fun TeacherRegistrationScreenPreview() {
    ScheduleIsEuTheme {
        TeacherRegistrationScreen(state = TeacherRegistrationUiState(), onAction = {})
    }
}
