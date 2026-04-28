package com.example.scheduleiseu.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.scheduleiseu.R
import com.example.scheduleiseu.core.ui.animation.SoftAppear
import com.example.scheduleiseu.core.ui.animation.appRevealMotion

@Composable
fun AppScreenScaffold(
    title: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes backgroundRes: Int = R.drawable.app_back,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppBackground(
        backgroundRes = backgroundRes,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            AppHeader(
                title = title,
                onMenuClick = onMenuClick,
                modifier = Modifier.appRevealMotion(initialOffsetY = 0f),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .appRevealMotion()
            ) {
                SoftAppear(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}
