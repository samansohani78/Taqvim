/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.automation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import ir.taqvim.app.R

/** A launcher shortcut (T-1215) that opens a `taqvim://` link (T-1103). */
data class AppShortcut(
    val id: String,
    val link: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
)

/**
 * The app's dynamic launcher shortcuts (T-1215): today, a new event, prayer times and the converter. They only open
 * screens through `taqvim://` links resolved by `MainActivity`, never an alias, so they keep working when the launcher
 * icon changes (T-1214); they are attached to the enabled launcher entry and published again when it changes.
 */
object AppShortcuts {
    val ALL: List<AppShortcut> =
        listOf(
            AppShortcut("today", "taqvim://calendar", R.string.shortcut_today, R.drawable.nav_ic_calendar),
            AppShortcut("new_event", "taqvim://event/new", R.string.shortcut_new_event, R.drawable.shortcut_ic_add),
            AppShortcut("prayer_times", "taqvim://times", R.string.shortcut_prayer_times, R.drawable.nav_ic_times),
            AppShortcut("converter", "taqvim://convert", R.string.shortcut_converter, R.drawable.nav_ic_tools),
        )

    /** The intent of [shortcut]: its link, opened only by this app. */
    fun intentOf(
        context: Context,
        shortcut: AppShortcut,
    ): Intent = Intent(Intent.ACTION_VIEW, shortcut.link.toUri()).setPackage(context.packageName)

    /**
     * Publishes the shortcuts for the launcher entry [activity] in the current language; nothing is sent when the
     * published ones already match, to stay within the system's rate limit. Returns whether they were published.
     */
    fun publish(
        context: Context,
        activity: String,
    ): Boolean {
        val component = ComponentName(context.packageName, activity)
        val wanted =
            ALL.mapIndexed { rank, shortcut ->
                ShortcutInfoCompat
                    .Builder(context, shortcut.id)
                    .setShortLabel(context.getString(shortcut.label))
                    .setIcon(IconCompat.createWithResource(context, shortcut.icon))
                    .setIntent(intentOf(context, shortcut))
                    .setActivity(component)
                    .setRank(rank)
                    .build()
            }
        val published = ShortcutManagerCompat.getDynamicShortcuts(context)
        // The system returns shortcuts in no particular order, so they are compared as sets.
        return if (published.map { it.signature() }.toSet() == wanted.map { it.signature() }.toSet()) {
            false
        } else {
            ShortcutManagerCompat.setDynamicShortcuts(context, wanted)
        }
    }

    private fun ShortcutInfoCompat.signature(): List<Any?> = listOf(id, shortLabel.toString(), activity, rank)
}
