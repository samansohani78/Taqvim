/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import ir.taqvim.app.di.themeSettings
import ir.taqvim.app.diagnostics.CrashContext
import ir.taqvim.app.navigation.AppDestination
import ir.taqvim.app.navigation.AppNavDisplay
import ir.taqvim.app.navigation.AppNavigationFrame
import ir.taqvim.app.navigation.AppRouter
import ir.taqvim.app.navigation.ContextExternalActions
import ir.taqvim.app.navigation.rememberAppNavigator
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeSettings
import ir.taqvim.data.database.backup.RestoreState
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.calendar.CalendarMessage
import ir.taqvim.feature.settings.OnboardingRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** The test tag of the first-run onboarding shown in place of the navigation frame (T-1501). */
internal const val ONBOARDING_TAG: String = "screen:onboarding"

/**
 * The app: the Taqvim theme around the navigation frame and the screens of the back stack (ADR-0015). Until the
 * first-run onboarding (T-1501, ADR-0023) is completed or skipped it is shown instead of the frame. A [link] (T-1103)
 * is opened once the frame is shown, after which [onLinkOpened] is called. While a restore left by the previous process
 * is not settled ([restore]), a waiting screen replaces everything (ADR-0032).
 */
@Composable
fun TaqvimAppShell(
    modifier: Modifier = Modifier,
    link: AppDestination? = null,
    onLinkOpened: () -> Unit = {},
    restore: RestoreState = RestoreState.SETTLED,
) {
    val preferences = koinInject<UserPreferencesRepository>()
    val stored by preferences.preferences.collectAsState(initial = null)
    val onboarded = stored?.onboardingCompleted
    val navigator = rememberAppNavigator()
    val currentOnLinkOpened by rememberUpdatedState(onLinkOpened)
    LaunchedEffect(link, onboarded) {
        if (link != null && onboarded == true) {
            navigator.navigate(link)
            currentOnLinkOpened()
        }
    }
    val context = LocalContext.current
    val resources = LocalResources.current
    RecordShownScreen(navigator.backStack.top)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val router =
        remember(navigator, context, resources) {
            AppRouter(navigator::navigate, ContextExternalActions(context)) { message ->
                scope.launch { snackbar.showSnackbar(resources.getString(message.text)) }
            }
        }
    TaqvimTheme(stored?.themeSettings() ?: ThemeSettings(), layoutTextDirection()) {
        when {
            restore != RestoreState.SETTLED -> {
                RestoreHold(restore, modifier)
            }

            onboarded == null -> {
                Box(modifier.fillMaxSize())
            }

            onboarded == false -> {
                OnboardingRoute(modifier.fillMaxSize().testTag(ONBOARDING_TAG))
            }

            else -> {
                AppNavigationFrame(navigator.backStack.selectedTab, navigator::select, snackbar, modifier) {
                    AppNavDisplay(navigator, router)
                }
            }
        }
    }
}

/**
 * Keeps the crash log's facts on the screen now shown (T-1504), so a crash report that arrives without a logcat says
 * where the app was.
 */
@Composable
private fun RecordShownScreen(shown: AppDestination) {
    val crashContext = koinInject<CrashContext>()
    LaunchedEffect(crashContext, shown) { crashContext.onRoute(shown::class.simpleName.orEmpty()) }
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
            CalendarMessage.DATE_OUT_OF_RANGE -> R.string.message_date_out_of_range
        }
