/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui

import android.graphics.Paint

const val DEFAULT_TEXT_SIZE = 14f

val SUPPORTED_SCALES = listOf(1.0f, 1.3f, 2.0f)

class DayPainter {
    private val paint = Paint()

    fun textSize(scale: Float): Float = paint.textSize * scale
}
