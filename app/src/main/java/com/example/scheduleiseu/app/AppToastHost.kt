package com.example.scheduleiseu.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.ui.animation.FadeSlideVisibility

@Immutable
internal data class AppToastMessage(
    val message: String,
    val durationMillis: Long,
    val id: Long = System.currentTimeMillis()
)

@Composable
internal fun AppToastHost(
    message: AppToastMessage?,
    modifier: Modifier = Modifier
) {
    FadeSlideVisibility(visible = message != null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                color = AppColors.DarkGreen.copy(alpha = 0.96f),
                shape = AppShapes.extraLarge
            ) {
                Text(
                    text = message?.message.orEmpty(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.White,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
