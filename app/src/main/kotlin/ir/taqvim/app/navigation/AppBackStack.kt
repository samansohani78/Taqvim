/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The back stack (ADR-0015). The calendar is always at the bottom; another tab's screen sits directly above it, and
 * screens opened from a tab are pushed on top. Back pops one screen; the calendar alone is not popped, so system back
 * leaves the app from there.
 */
@Immutable
data class AppBackStack(
    val entries: List<AppDestination> = listOf(AppDestination.Calendar),
) {
    init {
        require(entries.firstOrNull() == AppDestination.Calendar) { "the back stack must start at the calendar" }
    }

    /** The screen shown. */
    val top: AppDestination
        get() = entries.last()

    /** The tab the shown screen belongs to. */
    val selectedTab: TopLevelTab
        get() = entries.getOrNull(1)?.let(TopLevelTab::of) ?: TopLevelTab.CALENDAR

    /** Whether back pops a screen. */
    val canPop: Boolean
        get() = entries.size > 1

    /** Opens [destination]: a tab's own screen selects that tab, and the screen already shown is not opened twice. */
    fun push(destination: AppDestination): AppBackStack {
        val tab = TopLevelTab.of(destination)
        return when {
            tab != null -> select(tab)
            destination == top -> this
            else -> copy(entries = entries + destination)
        }
    }

    /** Closes the shown screen, unless it is the calendar alone. */
    fun pop(): AppBackStack = if (canPop) copy(entries = entries.dropLast(1)) else this

    /** Selects [tab] and closes the screens opened from any tab; selecting the current tab returns to its screen. */
    fun select(tab: TopLevelTab): AppBackStack =
        if (tab == TopLevelTab.CALENDAR) {
            AppBackStack()
        } else {
            AppBackStack(listOf(AppDestination.Calendar, tab.destination))
        }

    companion object {
        private val SERIALIZER = ListSerializer(AppDestination.serializer())

        /** [stack] as saved text. */
        fun encode(stack: AppBackStack): String = Json.encodeToString(SERIALIZER, stack.entries)

        /** The back stack saved as [text]; the calendar alone when the text cannot be read (e.g. after an update). */
        fun decode(text: String): AppBackStack =
            runCatching { AppBackStack(Json.decodeFromString(SERIALIZER, text)) }.getOrDefault(AppBackStack())
    }
}

/** The app's navigation state; see [rememberAppNavigator]. Main thread only. */
@Stable
class AppNavigator(
    initial: AppBackStack = AppBackStack(),
) {
    var backStack: AppBackStack by mutableStateOf(initial)
        private set

    fun navigate(destination: AppDestination) {
        backStack = backStack.push(destination)
    }

    fun select(tab: TopLevelTab) {
        backStack = backStack.select(tab)
    }

    fun back() {
        backStack = backStack.pop()
    }

    companion object {
        /** Saves the back stack as text, so it survives configuration changes and process death. */
        val SAVER: Saver<AppNavigator, String> =
            Saver(save = { AppBackStack.encode(it.backStack) }, restore = { AppNavigator(AppBackStack.decode(it)) })
    }
}

/** The navigator of this composition, restored after configuration changes and process death. */
@Composable
fun rememberAppNavigator(): AppNavigator = rememberSaveable(saver = AppNavigator.SAVER) { AppNavigator() }
