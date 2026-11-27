package com.symphonysimper.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Unread notification count per package, filled by BadgeService. */
object Badges {
    var counts by mutableStateOf<Map<String, Int>>(emptyMap())
}
