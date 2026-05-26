package com.example.scheduleiseu.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.core.designsystem.theme.AppColors

@Composable
fun AppFieldLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                color = AppColors.White,
                shape = RectangleShape
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.Black
    )
}
