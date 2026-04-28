package com.example.scheduleiseu.core.ui.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role

object AppMotion {
    const val PressDurationMillis = 140
    const val ContentChangeDurationMillis = 220
    const val VisibilityDurationMillis = 260
    const val ScreenTransitionDurationMillis = 300
    const val PressedScale = 0.97f
    const val InitialOffsetYPx = 18f
    const val InitialOffsetXPx = 22f
    const val InitialScale = 0.985f
    val Easing = FastOutSlowInEasing

    val PressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val RevealSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
}

@Composable
fun PressScale(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressedScale: Float = AppMotion.PressedScale,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.appPressFeedback(
            enabled = enabled,
            pressedScale = pressedScale,
        ),
        content = content,
    )
}

fun Modifier.appPressFeedback(
    enabled: Boolean = true,
    pressedScale: Float = AppMotion.PressedScale,
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = AppMotion.PressSpring,
        label = "appPressFeedbackScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.96f else 1f,
        animationSpec = AppMotion.PressSpring,
        label = "appPressFeedbackAlpha",
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }.then(
        if (enabled) {
            Modifier.pointerInput(enabled) {
                try {
                    awaitEachGesture {
                        awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        isPressed = true
                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        } while (event.changes.any { it.pressed })
                        isPressed = false
                    }
                } finally {
                    isPressed = false
                }
            }
        } else {
            Modifier
        }
    )
}

fun Modifier.appClickable(
    enabled: Boolean = true,
    pressedScale: Float = AppMotion.PressedScale,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    appPressFeedback(enabled = enabled, pressedScale = pressedScale)
        .clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
}

fun Modifier.appRevealMotion(
    visible: Boolean = true,
    initialOffsetY: Float = AppMotion.InitialOffsetYPx,
    initialOffsetX: Float = 0f,
    initialScale: Float = AppMotion.InitialScale,
): Modifier = composed {
    var shouldReveal by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        shouldReveal = visible
    }

    val alpha by animateFloatAsState(
        targetValue = if (shouldReveal) 1f else 0f,
        animationSpec = AppMotion.RevealSpring,
        label = "appRevealAlpha",
    )
    val translationY by animateFloatAsState(
        targetValue = if (shouldReveal) 0f else initialOffsetY,
        animationSpec = AppMotion.RevealSpring,
        label = "appRevealTranslationY",
    )
    val translationX by animateFloatAsState(
        targetValue = if (shouldReveal) 0f else initialOffsetX,
        animationSpec = AppMotion.RevealSpring,
        label = "appRevealTranslationX",
    )
    val scale by animateFloatAsState(
        targetValue = if (shouldReveal) 1f else initialScale,
        animationSpec = AppMotion.RevealSpring,
        label = "appRevealScale",
    )

    graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
        this.translationX = translationX
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun FadeSlideVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing)) +
            expandVertically(animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing)) +
            slideInVertically(
                animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing),
                initialOffsetY = { -it / 6 },
            ),
        exit = fadeOut(animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing)) +
            shrinkVertically(animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing)) +
            slideOutVertically(
                animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing),
                targetOffsetY = { -it / 8 },
            ),
    ) {
        Box(modifier = Modifier.appRevealMotion()) {
            content()
        }
    }
}

@Composable
fun FadeVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing)),
        exit = fadeOut(animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing)),
    ) {
        Box(modifier = Modifier.appRevealMotion(initialOffsetY = 0f, initialScale = 0.99f)) {
            content()
        }
    }
}

@Composable
fun FloatUpVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing)) +
            slideInVertically(
                animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing),
                initialOffsetY = { it / 5 },
            ) +
            scaleIn(
                initialScale = 0.98f,
                animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing),
            ),
        exit = fadeOut(animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing)) +
            slideOutVertically(
                animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing),
                targetOffsetY = { it / 7 },
            ) +
            scaleOut(
                targetScale = 0.985f,
                animationSpec = tween(AppMotion.VisibilityDurationMillis, easing = AppMotion.Easing),
            ),
    ) {
        Box(modifier = Modifier.appRevealMotion(initialOffsetY = AppMotion.InitialOffsetYPx)) {
            content()
        }
    }
}

@Composable
fun SoftAppear(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(AppMotion.ContentChangeDurationMillis, easing = AppMotion.Easing)) +
            slideInVertically(
                animationSpec = tween(AppMotion.ContentChangeDurationMillis, easing = AppMotion.Easing),
                initialOffsetY = { it / 9 },
            ) +
            scaleIn(
                initialScale = 0.99f,
                animationSpec = tween(AppMotion.ContentChangeDurationMillis, easing = AppMotion.Easing),
            ),
        exit = fadeOut(animationSpec = tween(AppMotion.ContentChangeDurationMillis, easing = AppMotion.Easing)) +
            scaleOut(
                targetScale = 0.99f,
                animationSpec = tween(AppMotion.ContentChangeDurationMillis, easing = AppMotion.Easing),
            ),
    ) {
        Box(modifier = Modifier.appRevealMotion(initialOffsetY = 12f, initialScale = 0.99f)) {
            content()
        }
    }
}

@Composable
fun <T> AppCrossfade(
    targetState: T,
    modifier: Modifier = Modifier,
    label: String = "AppCrossfade",
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(
                    animationSpec = tween(
                        durationMillis = AppMotion.ContentChangeDurationMillis,
                        easing = AppMotion.Easing
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = AppMotion.ContentChangeDurationMillis,
                        easing = AppMotion.Easing
                    ),
                    initialOffsetY = { it / 10 }
                ),
                initialContentExit = fadeOut(
                    animationSpec = tween(
                        durationMillis = AppMotion.ContentChangeDurationMillis / 2,
                        easing = AppMotion.Easing
                    )
                ) + slideOutVertically(
                    animationSpec = tween(
                        durationMillis = AppMotion.ContentChangeDurationMillis / 2,
                        easing = AppMotion.Easing
                    ),
                    targetOffsetY = { -it / 12 }
                ),
                sizeTransform = SizeTransform(clip = false)
            )
        },
        label = label,
    ) { state ->
        Box(modifier = Modifier.appRevealMotion(initialOffsetY = 8f, initialScale = 0.995f)) {
            content(state)
        }
    }
}

fun Modifier.appAnimatedContentSize(): Modifier = composed {
    animateContentSize(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )
}

fun appScreenEnterTransition(): EnterTransition {
    return fadeIn(animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing)) +
        slideInHorizontally(
            animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing),
            initialOffsetX = { it / 7 },
        ) +
        scaleIn(
            initialScale = 0.992f,
            animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing),
        )
}

fun appScreenExitTransition(): ExitTransition {
    return fadeOut(animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing)) +
        slideOutHorizontally(
            animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing),
            targetOffsetX = { -it / 10 },
        ) +
        scaleOut(
            targetScale = 0.992f,
            animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing),
        )
}

fun appScreenPopEnterTransition(): EnterTransition {
    return fadeIn(animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing)) +
        slideInHorizontally(
            animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing),
            initialOffsetX = { -it / 10 },
        ) +
        scaleIn(
            initialScale = 0.992f,
            animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing),
        )
}

fun appScreenPopExitTransition(): ExitTransition {
    return fadeOut(animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing)) +
        slideOutHorizontally(
            animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing),
            targetOffsetX = { it / 7 },
        ) +
        scaleOut(
            targetScale = 0.992f,
            animationSpec = tween(AppMotion.ScreenTransitionDurationMillis, easing = AppMotion.Easing),
        )
}
