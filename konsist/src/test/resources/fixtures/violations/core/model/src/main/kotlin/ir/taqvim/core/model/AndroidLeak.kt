/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.model

import android.util.Log
import ir.taqvim.feature.calendar.CalendarScreen

var lastMessage: String = ""

object Registry {
    val listeners = mutableListOf<String>()
}

class AndroidLeak {
    fun log() = Log.d("tag", lastMessage + CalendarScreen::class.simpleName)
}
