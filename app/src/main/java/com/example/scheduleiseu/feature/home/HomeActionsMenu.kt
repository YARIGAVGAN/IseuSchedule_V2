package com.example.scheduleiseu.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppDimens
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.SoftAppear
import com.example.scheduleiseu.domain.model.UserRole

@Composable
fun HomeActionsMenu(
    role: UserRole,
    showGroupSelection: Boolean,
    onDismiss: () -> Unit,
    onSelectTeacherClick: () -> Unit,
    onSelectGroupClick: () -> Unit,
    onSelectWeekClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            SoftAppear(modifier = modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
                shape = RoundedCornerShape(AppDimens.CardCornerLarge),
                color = AppColors.White,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                            PressScale {
                                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Закрыть", tint = AppColors.TextDark)
                                }
                            }
                        }
                        if (role == UserRole.Student) {
                            HomeActionButton(text = "Выбрать преподавателя", onClick = { onDismiss(); onSelectTeacherClick() })
                            if (showGroupSelection) {
                                HomeActionButton(text = "Выбрать группу", onClick = { onDismiss(); onSelectGroupClick() })
                            }
                        }
                        HomeActionButton(text = "Выбрать неделю", onClick = { onDismiss(); onSelectWeekClick() })
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PressScale(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.extraLarge,
            contentPadding = PaddingValues(vertical = 18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.HeaderGreen, contentColor = AppColors.White),
        ) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HomeActionsMenuPreview() {
    ScheduleIsEuTheme {
        HomeActionsMenu(role = UserRole.Student, showGroupSelection = true, onDismiss = {}, onSelectTeacherClick = {}, onSelectGroupClick = {}, onSelectWeekClick = {})
    }
}
