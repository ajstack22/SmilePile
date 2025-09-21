package com.smilepile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Animation components for smooth transitions and gesture-based interactions.
 * These components enhance user experience with fluid animations.
 */

/**
 * Animation constants for consistent timing across the app.
 */
object AnimationConstants {
    const val FAST_DURATION = 150
    const val MEDIUM_DURATION = 300
    const val SLOW_DURATION = 500

    const val SPRING_STIFFNESS = Spring.StiffnessMedium
    const val SPRING_DAMPING = Spring.DampingRatioMediumBouncy

    const val CROSSFADE_DURATION = 300
    const val FADE_DURATION = 200
    const val SCALE_DURATION = 200
}

/**
 * Smooth scale animation for photo grid items on interaction.
 */
@Composable
fun ScaleOnPressAnimation(
    pressed: Boolean,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = AnimationConstants.SPRING_DAMPING,
            stiffness = AnimationConstants.SPRING_STIFFNESS
        ),
        label = "scale_animation"
    )

    Box(
        modifier = Modifier.scale(scale)
    ) {
        content()
    }
}

/**
 * Smooth fade and slide animation for content transitions.
 */
@Composable
fun <T> SmoothContentTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            slideInVertically(
                animationSpec = tween(
                    durationMillis = AnimationConstants.MEDIUM_DURATION,
                    easing = EaseInOut
                ),
                initialOffsetY = { it / 3 }
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationConstants.FADE_DURATION,
                    easing = EaseInOut
                )
            ) togetherWith slideOutVertically(
                animationSpec = tween(
                    durationMillis = AnimationConstants.MEDIUM_DURATION,
                    easing = EaseInOut
                ),
                targetOffsetY = { -it / 3 }
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationConstants.FADE_DURATION,
                    easing = EaseInOut
                )
            )
        },
        label = "smooth_content_transition"
    ) { state ->
        content(state)
    }
}

/**
 * Animated visibility with smooth enter/exit transitions.
 */
@Composable
fun SmoothVisibilityAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = slideInVertically(
        animationSpec = tween(AnimationConstants.MEDIUM_DURATION),
        initialOffsetY = { -it }
    ) + fadeIn(animationSpec = tween(AnimationConstants.FADE_DURATION)),
    exit: ExitTransition = slideOutVertically(
        animationSpec = tween(AnimationConstants.MEDIUM_DURATION),
        targetOffsetY = { -it }
    ) + fadeOut(animationSpec = tween(AnimationConstants.FADE_DURATION)),
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit
    ) {
        content()
    }
}

/**
 * Expandable content animation for collapsible sections.
 */
@Composable
fun ExpandableContent(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        modifier = modifier,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = AnimationConstants.SPRING_DAMPING,
                stiffness = AnimationConstants.SPRING_STIFFNESS
            )
        ) + fadeIn(
            animationSpec = tween(AnimationConstants.FADE_DURATION)
        ),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = AnimationConstants.SPRING_DAMPING,
                stiffness = AnimationConstants.SPRING_STIFFNESS
            )
        ) + fadeOut(
            animationSpec = tween(AnimationConstants.FADE_DURATION)
        )
    ) {
        content()
    }
}

/**
 * Staggered animation for list items.
 */
@Composable
fun StaggeredAnimation(
    itemIndex: Int,
    delayPerItem: Int = 50,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(itemIndex) {
        delay(itemIndex * delayPerItem.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = AnimationConstants.MEDIUM_DURATION,
                easing = EaseInOut
            ),
            initialOffsetY = { it / 4 }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = AnimationConstants.FADE_DURATION,
                easing = EaseInOut
            )
        )
    ) {
        content()
    }
}

/**
 * Pulse animation for loading states or emphasis.
 */
@Composable
fun PulseAnimation(
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier.scale(scale)
    ) {
        content()
    }
}

/**
 * Bounce animation for user interactions.
 */
@Composable
fun BounceAnimation(
    triggered: Boolean,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (triggered) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bounce_animation"
    )

    Box(
        modifier = Modifier.scale(scale)
    ) {
        content()
    }
}

/**
 * Rotation animation for refresh indicators.
 */
@Composable
fun RotationAnimation(
    rotating: Boolean,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (rotating) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )

    Box(
        modifier = Modifier.graphicsLayer(rotationZ = rotation)
    ) {
        content()
    }
}

/**
 * Parallax scrolling effect for backgrounds.
 */
@Composable
fun ParallaxEffect(
    scrollState: LazyGridState,
    rate: Float = 0.5f,
    content: @Composable (Float) -> Unit
) {
    val offset by remember {
        derivedStateOf {
            scrollState.firstVisibleItemScrollOffset * rate
        }
    }

    content(offset)
}

/**
 * Spring-based drag gesture animation.
 */
@Composable
fun DragSpringAnimation(
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = AnimationConstants.SPRING_DAMPING,
            stiffness = AnimationConstants.SPRING_STIFFNESS
        ),
        label = "drag_offset_x"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = AnimationConstants.SPRING_DAMPING,
            stiffness = AnimationConstants.SPRING_STIFFNESS
        ),
        label = "drag_offset_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                translationX = animatedOffsetX,
                translationY = animatedOffsetY
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        // Handle press interactions
                        tryAwaitRelease()
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            }
    ) {
        content()
    }
}

/**
 * Entrance animation for photo grid items with staggered effect.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGridItemEntrance(
    itemIndex: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(itemIndex) {
        delay((itemIndex % 9) * 30L) // Stagger based on grid position
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialScale = 0.8f
        ) + fadeIn(
            animationSpec = tween(AnimationConstants.FADE_DURATION)
        )
    ) {
        content()
    }
}

/**
 * Smooth scroll to top animation.
 */
@Composable
fun rememberSmoothScrollToTop(
    scrollState: LazyGridState
): () -> Unit {
    return remember {
        {
            // Smooth scroll to top implementation would go here
            // This is a placeholder for the actual implementation
        }
    }
}

/**
 * Gesture-based scale animation for pinch-to-zoom.
 */
@Composable
fun GestureScaleAnimation(
    onScaleChange: (Float) -> Unit,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "gesture_scale"
    )

    Box(
        modifier = Modifier
            .scale(animatedScale)
            .fillMaxSize()
    ) {
        content()
    }
}