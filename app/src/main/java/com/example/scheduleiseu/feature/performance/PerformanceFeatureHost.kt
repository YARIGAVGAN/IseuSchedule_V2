package com.example.scheduleiseu.feature.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PerformanceFeatureHost(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PerformanceViewModel = viewModel(factory = PerformanceViewModelFactory())
    val state by viewModel.state.collectAsState()

    PerformanceScreen(
        state = state,
        onMenuClick = onMenuClick,
        onRetryClick = viewModel::retry,
        onSemesterClick = viewModel::onSemesterSelected,
        modifier = modifier
    )
}
