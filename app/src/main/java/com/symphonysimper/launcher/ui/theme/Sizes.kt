package com.symphonysimper.launcher.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Control sizes for the whole launcher, taken from Material 3's size scale. Use these rather than
 * literal dp so every screen stays in step, and never make anything tappable smaller than [MinTouch].
 */
object Sizes {
    /** Android and Material minimum touch target; inline icon buttons are this size. */
    val MinTouch = 48.dp

    /** Material's medium control: buttons, toggles, menu rows, the search field. */
    val Control = 56.dp

    /** Result rows and controls floating over content: a step up, so they're easy to hit with a thumb. */
    val ControlLarge = 64.dp

    val Icon = 24.dp

    /** A chip's visible height; its touch area still grows to [MinTouch]. */
    val Chip = 32.dp

    /** App icons in the dock and the drawer, the same size in both; icons are rendered at this size. */
    val AppIcon = 60.dp

    /** Extra space under controls floating at the bottom, keeping them in easy thumb reach above the gesture bar. */
    val ThumbLift = 40.dp
}
