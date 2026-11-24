package com.cynorra.areweokay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Counts sensitive screens (crisis, journal, breathing, consent) currently shown; banner is hidden while > 0. */
object AdGate {
    var sensitiveScreens by mutableIntStateOf(0)
    val adsHidden: Boolean get() = sensitiveScreens > 0
}

/** Place inside a sensitive screen: hides the banner ad for as long as it is composed. */
@Composable
fun NoAds() {
    DisposableEffect(Unit) {
        AdGate.sensitiveScreens++
        onDispose { AdGate.sensitiveScreens-- }
    }
}
