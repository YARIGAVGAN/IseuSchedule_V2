package com.example.scheduleiseu.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.scheduleiseu.R
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppDimens
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize
import com.example.scheduleiseu.core.ui.animation.appRevealMotion

@Composable
fun AppHeader(
    title: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.HeaderGreen)
            .appAnimatedContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(AppDimens.HeaderHeight)
                .padding(horizontal = AppDimens.HeaderHorizontalPadding)
                .appAnimatedContentSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(AppDimens.HeaderSideBoxWidth), contentAlignment = Alignment.CenterStart) {
                PressScale {
                    IconButton(onClick = onMenuClick, modifier = Modifier.size(AppDimens.HeaderIconSize)) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_menu),
                            contentDescription = "Открыть меню",
                            tint = Color.White,
                            modifier = Modifier.size(AppDimens.HeaderIconSize),
                        )
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .appRevealMotion(initialOffsetY = 0f, initialScale = 0.995f),
            )
            Box(modifier = Modifier.width(AppDimens.HeaderSideBoxWidth), contentAlignment = Alignment.CenterEnd) {
                trailingContent?.invoke()
            }
        }
    }
}

@Preview
@Composable
private fun AppHeaderPreview() {
    ScheduleIsEuTheme {
        AppHeader(title = "Заголовок", onMenuClick = {})
    }
}
