package com.example.scheduleiseu.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.scheduleiseu.core.designsystem.theme.AppColors
import com.example.scheduleiseu.core.designsystem.theme.AppShapes
import com.example.scheduleiseu.core.ui.animation.PressScale
import com.example.scheduleiseu.core.ui.animation.appAnimatedContentSize

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SimpleSelectionBottomSheet(
    title: String,
    items: List<String>,
    selectedItem: String? = null,
    isScrollable: Boolean = false,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.BottomSheetSurface,
        contentColor = AppColors.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = AppColors.White)

            val selectedIndex = remember(items, selectedItem) {
                items.indexOfFirst { it == selectedItem }.coerceAtLeast(0)
            }
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

            LaunchedEffect(selectedIndex, items.size) {
                if (items.isNotEmpty()) {
                    listState.animateScrollToItem(selectedIndex)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isScrollable) Modifier.heightIn(max = 420.dp) else Modifier),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item }
                ) { _, item ->
                    val isSelected = item == selectedItem
                    RowCard(
                        text = item,
                        selected = isSelected,
                        onClick = { onSelect(item) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun RowCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressScale(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .appAnimatedContentSize()
                .background(
                    color = if (selected) AppColors.BottomSheetCardSelected else AppColors.BottomSheetCard,
                    shape = AppShapes.extraLarge
                )
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) AppColors.HeaderGreen else AppColors.BottomSheetCardBorder,
                    shape = AppShapes.extraLarge
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = if (selected) 16.dp else 14.dp)
        ) {
        Text(
            text = text,
            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = AppColors.BottomSheetCardText,
            modifier = Modifier.align(Alignment.CenterStart)
        )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = AppColors.HeaderGreen,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}
