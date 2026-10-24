@file:JvmName("EdgeToEdge")

package com.google.android.diskusage.ui.common

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Starting with targetSdk 35 the platform lays every window out behind the
 * system bars, and targetSdk 36 removes the opt-out entirely. The content
 * therefore has to keep clear of the bars itself.
 *
 * Pads the activity content root with the system bar, display cutout and IME
 * insets so the UI keeps the exact bounds it had while the window still fit
 * the system windows. Call right after `setContentView`.
 */
fun Activity.applySystemBarInsets() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val content = findViewById<View>(android.R.id.content) ?: return
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.updatePadding(
            left = bars.left,
            top = bars.top,
            right = bars.right,
            bottom = maxOf(bars.bottom, ime.bottom)
        )
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(content)
}
