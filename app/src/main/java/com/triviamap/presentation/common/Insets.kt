package com.triviamap.presentation.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/** Navigation-bar-only variant of [systemBarsSafePadding]. */
fun Modifier.navigationBarsSafePadding(): Modifier = composed {
    windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.tappableElement))
}

/**
 * Keeps content clear of the status bar and of the navigation bar. `tappableElement` covers OEM
 * 3-button bars that some devices do not report through `navigationBars`.
 */
fun Modifier.systemBarsSafePadding(): Modifier = composed {
    windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.tappableElement))
}
