package com.example.scheduleiseu.feature.auth.studentregistration

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.R
import com.example.scheduleiseu.core.designsystem.components.AppCard
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppDimens
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.FadeSlideVisibility
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.SoftAppear
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.core.ui.animation.appPressFeedback

@Immutable
data class StudentRegistrationUiState(
    val name: String = "",
    val selectedFaculty: String? = null,
    val selectedStudyForm: String? = null,
    val selectedCourse: String? = null,
    val selectedGroup: String? = null,
    val selectedSubgroup: String? = null,
    val facultyOptions: List<String> = emptyList(),
    val studyFormOptions: List<String> = emptyList(),
    val courseOptions: List<String> = emptyList(),
    val groupOptions: List<String> = emptyList(),
    val subgroupOptions: List<String> = listOf("1", "2"),
    val isLoading: Boolean = false,
    val controlsEnabled: Boolean = true,
    val errorMessage: String? = null
)

sealed interface StudentRegistrationAction {
    data class NameChanged(val value: String) : StudentRegistrationAction
    data class FacultySelected(val value: String) : StudentRegistrationAction
    data class StudyFormSelected(val value: String) : StudentRegistrationAction
    data class CourseSelected(val value: String) : StudentRegistrationAction
    data class GroupSelected(val value: String) : StudentRegistrationAction
    data class SubgroupSelected(val value: String) : StudentRegistrationAction
    data object CreateAccountClicked : StudentRegistrationAction
}

@Composable
fun StudentRegistrationScreen(
    state: StudentRegistrationUiState,
    onAction: (StudentRegistrationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val canCreateAccount =
        state.controlsEnabled && !state.isLoading && state.name.isNotBlank() &&
            state.selectedFaculty != null && state.selectedStudyForm != null &&
            state.selectedCourse != null && state.selectedGroup != null &&
            state.selectedSubgroup != null
    val formOffset by animateFloatAsState(
        targetValue = if (state.isLoading) -18f else 0f,
        label = "studentRegistrationFormOffset"
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
                Spacer(modifier = Modifier.height(28.dp))

                SoftAppear {
                    Text(
                        text = "Регистрация аккаунта",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.ScreenTitle
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SoftAppear {
                    Text(
                        text = "Заполните данные, чтобы привязать аккаунт к учебной группе",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.ScreenTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            RegistrationTextField(
                                value = state.name,
                                onValueChange = { onAction(StudentRegistrationAction.NameChanged(it)) },
                                label = "Ваше имя",
                                enabled = state.controlsEnabled && !state.isLoading
                            )

                            RegistrationSelector(
                                title = "Факультет",
                                value = state.selectedFaculty,
                                options = state.facultyOptions,
                                placeholder = "Выберите вариант",
                                enabled = state.controlsEnabled && !state.isLoading,
                                onSelected = { onAction(StudentRegistrationAction.FacultySelected(it)) }
                            )

                            RegistrationSelector(
                                title = "Форма обучения",
                                value = state.selectedStudyForm,
                                options = state.studyFormOptions,
                                placeholder = "Выберите вариант",
                                enabled = state.controlsEnabled && !state.isLoading,
                                onSelected = { onAction(StudentRegistrationAction.StudyFormSelected(it)) }
                            )

                            RegistrationSelector(
                                title = "Курс",
                                value = state.selectedCourse,
                                options = state.courseOptions,
                                placeholder = "Выберите вариант",
                                enabled = state.controlsEnabled && !state.isLoading,
                                onSelected = { onAction(StudentRegistrationAction.CourseSelected(it)) }
                            )

                            RegistrationSelector(
                                title = "Группа",
                                value = state.selectedGroup,
                                options = state.groupOptions,
                                placeholder = "Выберите вариант",
                                enabled = state.controlsEnabled && !state.isLoading,
                                onSelected = { onAction(StudentRegistrationAction.GroupSelected(it)) }
                            )

                            RegistrationSelector(
                                title = "Подгруппа",
                                value = state.selectedSubgroup,
                                options = state.subgroupOptions,
                                placeholder = "Выберите вариант",
                                enabled = state.controlsEnabled && !state.isLoading,
                                onSelected = { onAction(StudentRegistrationAction.SubgroupSelected(it)) }
                            )

                            FadeSlideVisibility(visible = !state.errorMessage.isNullOrBlank()) {
                                ErrorCard(message = state.errorMessage.orEmpty())
                            }

                            PressScale(enabled = canCreateAccount) {
                                Button(
                                    onClick = { onAction(StudentRegistrationAction.CreateAccountClicked) },
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
                                    Text(text = "Создать аккаунт", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            FadeSlideVisibility(visible = state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(AppColors.Overlay),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.White, strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun RegistrationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = AppShapes.extraLarge,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.FieldText),
        label = { Text(text = label, style = MaterialTheme.typography.bodySmall, color = AppColors.FieldText.copy(alpha = 0.82f)) },
        colors = outlinedFieldColors()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationSelector(
    title: String,
    value: String?,
    options: List<String>,
    placeholder: String,
    enabled: Boolean,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .appAnimatedContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = AppColors.White)

        PressScale(enabled = enabled) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = value.orEmpty(),
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .appAnimatedContentSize(),
                    readOnly = true,
                    enabled = enabled,
                    singleLine = true,
                    shape = AppShapes.extraLarge,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.FieldText),
                    placeholder = {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.FieldPlaceholder
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = outlinedFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = AppColors.FieldSurface
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            modifier = Modifier
                                .appAnimatedContentSize()
                                .appPressFeedback(),
                            text = {
                                Text(
                                    text = option,
                                    color = AppColors.FieldText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                expanded = false
                                onSelected(option)
                            }
                        )
                    }
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

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
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
    focusedTrailingIconColor = AppColors.FieldText,
    unfocusedTrailingIconColor = AppColors.FieldText.copy(alpha = 0.75f),
    disabledTrailingIconColor = AppColors.FieldText.copy(alpha = 0.45f),
    cursorColor = AppColors.LightGreen
)

@Preview(showBackground = true)
@Composable
private fun StudentRegistrationScreenPreview() {
    ScheduleIsEuTheme {
        StudentRegistrationScreen(
            state = StudentRegistrationUiState(
                facultyOptions = listOf("ФКП", "ФПМИ", "ФЭМ"),
                studyFormOptions = listOf("Очная", "Заочная"),
                courseOptions = listOf("1", "2", "3", "4"),
                groupOptions = listOf("10701121", "10701122"),
                selectedSubgroup = "1"
            ),
            onAction = {}
        )
    }
}
