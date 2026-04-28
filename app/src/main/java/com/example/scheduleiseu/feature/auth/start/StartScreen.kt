package com.example.scheduleiseu.feature.auth.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.R
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.SoftAppear
import com.example.scheduleiseu.core.ui.animation.appRevealMotion

@Composable
fun StartScreen(
    onStudentClick: () -> Unit,
    onTeacherClick: () -> Unit,
) {
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SoftAppear {
                Text(
                    text = "Здравствуйте!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppColors.ScreenTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.appRevealMotion(initialOffsetY = 0f, initialScale = 0.99f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            SoftAppear {
                Text(
                    text = "Выберите режим работы",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.ScreenTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.appRevealMotion(initialOffsetY = 0f, initialScale = 0.995f)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            SoftAppear {
                StartActionButton(text = "Студент", onClick = onStudentClick)
            }
            Spacer(modifier = Modifier.height(16.dp))
            SoftAppear {
                StartActionButton(text = "Преподаватель", onClick = onTeacherClick)
            }
        }
    }
}

@Composable
private fun StartActionButton(text: String, onClick: () -> Unit) {
    PressScale(modifier = Modifier.fillMaxWidth(0.74f), enabled = true) {
        Button(
            onClick = onClick,
            modifier = Modifier.height(52.dp).fillMaxWidth(),
            shape = AppShapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.HeaderGreen,
                contentColor = AppColors.White
            )
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() {
    ScheduleIsEuTheme {
        StartScreen(onStudentClick = {}, onTeacherClick = {})
    }
}
