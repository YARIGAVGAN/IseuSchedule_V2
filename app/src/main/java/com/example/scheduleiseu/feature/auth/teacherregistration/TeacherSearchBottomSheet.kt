package com.example.scheduleiseu.feature.auth.teacherregistration

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppDimens
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.AppCrossfade
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.domain.model.TeacherSearchItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TeacherSearchBottomSheet(
    query: String,
    results: List<TeacherSearchItem>,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onTeacherClick: (TeacherSearchItem) -> Unit,
    onClearQueryClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = AppColors.BottomSheetSurface,
        contentColor = AppColors.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .background(color = AppColors.BottomSheetHandle, shape = AppShapes.small)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDimens.OuterPadding)
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Поиск преподавателя", style = MaterialTheme.typography.titleLarge, color = AppColors.White)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Введите имя преподавателя и выберите нужного сотрудника",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.White.copy(alpha = 0.76f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            TeacherSearchField(value = query, onValueChange = onQueryChange)
            Spacer(modifier = Modifier.height(14.dp))
            TeacherSearchSheetContent(
                query = query,
                results = results,
                isLoading = isLoading,
                onTeacherClick = onTeacherClick,
                onClearQueryClick = onClearQueryClick
            )
        }
    }
}

@Composable
private fun TeacherSearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = AppShapes.extraLarge,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.FieldText),
        label = { Text(text = "Имя преподавателя", style = MaterialTheme.typography.bodySmall) },
        placeholder = {
            Text(
                text = "Например, Иванов Иван Иванович",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.FieldPlaceholder
            )
        },
        leadingIcon = {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = AppColors.FieldText)
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColors.FieldText,
            unfocusedTextColor = AppColors.FieldText,
            focusedContainerColor = AppColors.FieldSurface,
            unfocusedContainerColor = AppColors.FieldSurface,
            focusedBorderColor = AppColors.LightGreen,
            unfocusedBorderColor = AppColors.FieldBorder,
            focusedLabelColor = AppColors.ScreenTextSecondary,
            unfocusedLabelColor = AppColors.ScreenTextSecondary,
            cursorColor = AppColors.LightGreen
        )
    )
}

@Composable
private fun TeacherSearchSheetContent(
    query: String,
    results: List<TeacherSearchItem>,
    isLoading: Boolean,
    onTeacherClick: (TeacherSearchItem) -> Unit,
    onClearQueryClick: () -> Unit
) {
    AppCrossfade(
        targetState = when {
            isLoading -> "loading"
            query.isBlank() -> "hint"
            results.isEmpty() -> "empty"
            else -> "results"
        },
        label = "teacherSearchContent",
    ) { targetState ->
        when (targetState) {
            "loading" -> SearchLoadingState()
            "hint" -> SearchHintState()
            "empty" -> SearchEmptyState(query = query, onClearQueryClick = onClearQueryClick)
            "results" -> TeacherResultsList(items = results, onTeacherClick = onTeacherClick)
        }
    }
}

@Composable
private fun SearchLoadingState() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.White, strokeWidth = 2.dp)
    }
}

@Composable
private fun SearchHintState() {
    InfoCard(
        icon = { Icon(imageVector = Icons.Outlined.PersonSearch, contentDescription = null, tint = AppColors.FieldText) },
        title = "Начните поиск",
        body = "Введите фамилию, имя или полное ФИО преподавателя, чтобы увидеть доступные варианты."
    )
}

@Composable
private fun SearchEmptyState(query: String, onClearQueryClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .appAnimatedContentSize()
            .background(color = AppColors.BottomSheetCard, shape = AppShapes.extraLarge)
            .border(width = 1.dp, color = AppColors.BottomSheetCardBorder, shape = AppShapes.extraLarge)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Ничего не найдено", style = MaterialTheme.typography.titleMedium, color = AppColors.BottomSheetCardText)
            Text(
                text = "По запросу \"$query\" преподаватели не найдены. Попробуйте сократить запрос или изменить написание.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.BottomSheetCardSecondary
            )
            PressScale {
                TextButton(onClick = onClearQueryClick) {
                    Text(text = "Очистить поиск", color = AppColors.HeaderGreen)
                }
            }
        }
    }
}

@Composable
private fun TeacherResultsList(items: List<TeacherSearchItem>, onTeacherClick: (TeacherSearchItem) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(320.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(items = items, key = { it.id }) { teacher ->
            TeacherResultCard(
                item = teacher,
                onClick = { onTeacherClick(teacher) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun TeacherResultCard(
    item: TeacherSearchItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressScale(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .appAnimatedContentSize()
                .background(color = AppColors.BottomSheetCard, shape = AppShapes.extraLarge)
                .border(width = 1.dp, color = AppColors.BottomSheetCardBorder, shape = AppShapes.extraLarge)
                .clickable(onClick = onClick)
                .padding(14.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color = AppColors.HeaderGreen.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Outlined.School, contentDescription = null, tint = AppColors.HeaderGreen)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.BottomSheetCardText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.BottomSheetCardSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
}

@Composable
private fun InfoCard(icon: @Composable () -> Unit, title: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .appAnimatedContentSize()
            .background(color = AppColors.BottomSheetCard, shape = AppShapes.extraLarge)
            .border(width = 1.dp, color = AppColors.BottomSheetCardBorder, shape = AppShapes.extraLarge)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            icon()
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = AppColors.BottomSheetCardText)
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = AppColors.BottomSheetCardSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, heightDp = 700)
@Composable
private fun TeacherSearchBottomSheetPreview() {
    ScheduleIsEuTheme {
        TeacherSearchBottomSheet(
            query = "иванов",
            results = listOf(
                TeacherSearchItem(id = "1", fullName = "Иванов Иван Иванович", subtitle = "Кафедра программной инженерии")
            ),
            isLoading = false,
            onQueryChange = {},
            onTeacherClick = {},
            onClearQueryClick = {},
            onDismiss = {}
        )
    }
}
