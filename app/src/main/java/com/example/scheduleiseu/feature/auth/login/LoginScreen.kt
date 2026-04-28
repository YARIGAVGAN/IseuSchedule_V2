package com.example.scheduleiseu.feature.auth.login

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.R
import com.example.scheduleiseu.core.designsystem.components.AppCard
import com.example.scheduleiseu.core.designsystem.components.AppSettingsSwitch
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
data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val captcha: String = "",
    val captchaBitmap: Bitmap? = null,
    val autoLogin: Boolean = true,
    val controlsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface LoginAction {
    data class LoginChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data class CaptchaChanged(val value: String) : LoginAction
    data class AutoLoginChanged(val value: Boolean) : LoginAction
    data object RefreshCaptchaClicked : LoginAction
    data object ContinueWithoutRegistrationClicked : LoginAction
    data object LoginClicked : LoginAction
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val canLogin = state.controlsEnabled &&
        !state.isLoading &&
        state.login.isNotBlank() &&
        state.password.isNotBlank() &&
        state.captcha.isNotBlank()
    val authPanelOffset by animateFloatAsState(
        targetValue = if (state.isLoading) -18f else 0f,
        label = "loginAuthPanelOffset"
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
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = AppDimens.OuterPadding, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                SoftAppear {
                    Text(
                        text = "Вход",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.ScreenTitle
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SoftAppear {
                    Text(
                        text = "Введите данные аккаунта и код с изображения",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.ScreenTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                SoftAppear {
                    AppCard(
                        modifier = Modifier
                            .appAnimatedContentSize()
                            .graphicsLayer { translationY = authPanelOffset },
                        containerColor = AppColors.DarkGreen,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SectionSpacing)) {
                            AuthTextField(
                                value = state.login,
                                onValueChange = { onAction(LoginAction.LoginChanged(it)) },
                                label = "Логин",
                                enabled = state.controlsEnabled && !state.isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                )
                            )

                            PasswordField(
                                value = state.password,
                                onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
                                enabled = state.controlsEnabled && !state.isLoading
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Captcha",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.White
                                )

                                CaptchaBlock(
                                    captchaBitmap = state.captchaBitmap,
                                    enabled = state.controlsEnabled && !state.isLoading,
                                    onRefreshClick = { onAction(LoginAction.RefreshCaptchaClicked) }
                                )

                                AuthTextField(
                                    value = state.captcha,
                                    onValueChange = { onAction(LoginAction.CaptchaChanged(it)) },
                                    label = "Код с изображения",
                                    enabled = state.controlsEnabled && !state.isLoading,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Автовход",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.White
                                    )
                                    Text(
                                        text = "Сохранять данные для повторного входа",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.White.copy(alpha = 0.72f)
                                    )
                                }

                                AppSettingsSwitch(
                                    checked = state.autoLogin,
                                    onCheckedChange = { onAction(LoginAction.AutoLoginChanged(it)) }
                                )
                            }

                            FadeSlideVisibility(visible = !state.errorMessage.isNullOrBlank()) {
                                ErrorCard(message = state.errorMessage.orEmpty())
                            }

                            PressScale(enabled = canLogin) {
                                Button(
                                    onClick = { onAction(LoginAction.LoginClicked) },
                                    enabled = canLogin,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = AppShapes.extraLarge,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.HeaderGreen,
                                        contentColor = AppColors.White,
                                        disabledContainerColor = AppColors.HeaderGreen.copy(alpha = 0.45f),
                                        disabledContentColor = AppColors.White.copy(alpha = 0.7f)
                                    )
                                ) {
                                    AppCrossfade(targetState = state.isLoading, label = "loginButtonContent") { loading ->
                                        if (loading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = AppColors.White
                                            )
                                        } else {
                                            Text(text = "Войти", style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SoftAppear {
                    PressScale(enabled = state.controlsEnabled && !state.isLoading) {
                        TextButton(
                            onClick = { onAction(LoginAction.ContinueWithoutRegistrationClicked) },
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.FieldText),
        label = {
            Text(
                text = label,
                color = AppColors.White
            )
        },
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        shape = AppShapes.extraLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColors.FieldText,
            unfocusedTextColor = AppColors.FieldText,
            disabledTextColor = AppColors.FieldText.copy(alpha = 0.55f),
            focusedContainerColor = AppColors.FieldSurface,
            unfocusedContainerColor = AppColors.FieldSurface,
            disabledContainerColor = AppColors.FieldSurface.copy(alpha = 0.82f),
            focusedBorderColor = AppColors.LightGreen,
            unfocusedBorderColor = AppColors.FieldBorder,
            disabledBorderColor = AppColors.FieldBorder.copy(alpha = 0.6f),
            focusedLabelColor = AppColors.FieldText.copy(alpha = 0.82f),
            unfocusedLabelColor = AppColors.FieldText.copy(alpha = 0.7f),
            disabledLabelColor = AppColors.FieldText.copy(alpha = 0.45f),
            cursorColor = AppColors.LightGreen
        )
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean
) {

    var isPasswordVisible by remember { mutableStateOf(false) }

    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = "Пароль",
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            PressScale(enabled = enabled) {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }, enabled = enabled) {
                    Icon(
                    imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (isPasswordVisible) "Скрыть пароль" else "Показать пароль",
                    tint = AppColors.FieldText
                    )
                }
            }
        }
    )
}

@Composable
private fun CaptchaBlock(
    captchaBitmap: Bitmap?,
    enabled: Boolean,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(88.dp)
                .background(color = AppColors.FieldSurface, shape = AppShapes.extraLarge)
                .border(width = 1.dp, color = AppColors.Divider, shape = AppShapes.extraLarge),
            contentAlignment = Alignment.Center
        ) {
            AppCrossfade(targetState = captchaBitmap, label = "captchaImage") { bitmap ->
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captcha",
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                } else {
                    Text(
                        text = "Изображение captcha",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextDark
                    )
                }
            }
        }

        PressScale(enabled = enabled) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(color = AppColors.White, shape = RoundedCornerShape(AppDimens.CardCornerLarge))
                    .border(width = 1.dp, color = AppColors.FieldBorder, shape = RoundedCornerShape(AppDimens.CardCornerLarge))
                    .clickable(enabled = enabled, onClick = onRefreshClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Обновить captcha",
                    tint = if (enabled) AppColors.FieldText else AppColors.FieldText.copy(alpha = 0.4f)
                )
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
            .border(width = 1.dp, color = AppColors.Error.copy(alpha = 0.45f), shape = AppShapes.extraLarge)
            .padding(12.dp)
    ) {
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = AppColors.White)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    ScheduleIsEuTheme {
        LoginScreen(state = LoginUiState(), onAction = {})
    }
}
