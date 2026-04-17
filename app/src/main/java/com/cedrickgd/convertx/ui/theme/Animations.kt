package com.cedrickgd.convertx.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

/**
 * Standard "fade + slide-up" enter transition used across Convert-X screens and cards.
 */
fun fadeSlideIn(
    durationMillis: Int = 260,
    delayMillis: Int = 0,
    slideFromOffsetY: Int = 32
): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis)) +
        slideInVertically(
            animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis),
            initialOffsetY = { slideFromOffsetY }
        )

/**
 * Matching exit transition for [fadeSlideIn].
 */
fun fadeSlideOut(
    durationMillis: Int = 200,
    slideToOffsetY: Int = 32
): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = durationMillis)) +
        slideOutVertically(
            animationSpec = tween(durationMillis = durationMillis),
            targetOffsetY = { slideToOffsetY }
        )

/**
 * Helper that returns an [EnterTransition] for the Nth child in a staggered list.
 * Usage: `AnimatedVisibility(..., enter = staggerChildren(index * 60))`.
 */
fun staggerChildren(delayMillis: Int): EnterTransition =
    fadeSlideIn(durationMillis = 300, delayMillis = delayMillis, slideFromOffsetY = 24)
