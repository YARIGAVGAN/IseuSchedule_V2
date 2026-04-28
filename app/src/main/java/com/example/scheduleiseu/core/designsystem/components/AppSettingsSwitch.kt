package com.example.scheduleiseu.core.designsystem.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppDimens
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.appPressFeedback
import com.example.scheduleiseu.core.ui.animation.appRevealMotion

@Composable
fun AppSettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
            .width(AppDimens.SwitchWidth)
            .scale(AppDimens.SwitchScale)
            .appPressFeedback()
            .appRevealMotion(initialOffsetX = 8f, initialOffsetY = 0f, initialScale = 0.96f),
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = AppColors.HeaderGreen,
            checkedBorderColor = AppColors.HeaderGreen,
            uncheckedThumbColor = AppColors.HeaderGreen,
            uncheckedTrackColor = Color.Black,
            uncheckedBorderColor = Color.Black,
        ),
    )
}

@Preview
@Composable
private fun AppSettingsSwitchPreview() {
    ScheduleIsEuTheme {
        AppSettingsSwitch(checked = true, onCheckedChange = {})
    }
}
