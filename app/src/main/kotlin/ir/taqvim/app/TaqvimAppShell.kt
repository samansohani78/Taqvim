/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.view.View
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import ir.taqvim.app.di.themeSettings
import ir.taqvim.app.navigation.AppNavDisplay
import ir.taqvim.app.navigation.AppNavigationFrame
import ir.taqvim.app.navigation.AppRouter
import ir.taqvim.app.navigation.ContextExternalActions
import ir.taqvim.app.navigation.rememberAppNavigator
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeSettings
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.calendar.CalendarMessage
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** The app: the Taqvim theme around the navigation frame and the screens of the back stack (ADR-0015). */
@Composable
fun TaqvimAppShell(modifier: Modifier = Modifier) {
    val navigator = rememberAppNavigator()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val router =
        remember(navigator, context, resources) {
            AppRouter(navigator::navigate, ContextExternalActions(context)) { message ->
                scope.launch { snackbar.showSnackbar(resources.getString(message.text)) }
            }
        }
    val preferences = koinInject<UserPreferencesRepository>()
    val stored by preferences.preferences.collectAsState(initial = null)
    TaqvimTheme(stored?.themeSettings() ?: ThemeSettings(), layoutTextDirection()) {
        AppNavigationFrame(navigator.backStack.selectedTab, navigator::select, snackbar, modifier) {
            AppNavDisplay(navigator, router)
        }
    }
}

/** The writing direction of the current configuration (the app language, T-1501). */
@Composable
private fun layoutTextDirection(): TextDirection {
    val rtl = LocalConfiguration.current.layoutDirection == View.LAYOUT_DIRECTION_RTL
    return if (rtl) TextDirection.RTL else TextDirection.LTR
}

/** The text shown for this calendar message. */
@get:StringRes
private val CalendarMessage.text: Int
    get() =
        when (this) {
            CalendarMessage.NO_UPCOMING_OCCURRENCE -> R.string.message_no_upcoming_occurrence
            CalendarMessage.SETTING_NOT_SAVED -> R.string.message_setting_not_saved
        }
