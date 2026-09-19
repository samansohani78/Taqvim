/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.diagnostics

import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * What the app knows about the current run when it crashes (T-1504). Everything is a short fact, never user data:
 * [route] is the screen shown and [settings] the chosen calendars, Islamic variant and override.
 */
data class CrashFacts(
    /** Version name, code and build type. */
    val app: String,
    /** Android release and API level. */
    val android: String,
    /** Manufacturer and model. */
    val device: String,
    /** Language tag of the app. */
    val locale: String,
    val route: String = UNKNOWN,
    val settings: String = UNKNOWN,
) {
    companion object {
        /** Stands in for a fact the app has not learned yet, e.g. the route before the first screen is shown. */
        const val UNKNOWN: String = "unknown"
    }
}

/**
 * The facts of the current run. The crash handler reads them on whichever thread died, so they are held in an
 * [AtomicReference] rather than a flow: at that moment nothing may suspend, allocate a coroutine or touch a store.
 */
class CrashContext(
    initial: CrashFacts,
) {
    private val state = AtomicReference(initial)

    val facts: CrashFacts get() = state.get()

    /** The screen now shown, e.g. `Calendar`. */
    fun onRoute(route: String) {
        state.updateAndGet { it.copy(route = route) }
    }

    /** The settings that decide which calendars are computed, e.g. `calendars=PERSIAN+GREGORIAN`. */
    fun onSettings(settings: String) {
        state.updateAndGet { it.copy(settings = settings) }
    }
}

/** A crash record kept on this device. */
data class StoredCrash(
    val atEpochMillis: Long,
    val text: String,
)

/**
 * Crash records in [directory], one file each, named after the time they were written. At most [MAX_FILES] are kept
 * and each is cut to [MAX_CHARS], so a crash loop cannot fill the device. Reading and writing are plain file calls:
 * the writer runs inside an uncaught exception handler, where a database or a coroutine would not be safe.
 */
class CrashLogStore(
    private val directory: File,
) {
    /** Writes one record; `false` when the device refused it, which must never fail the crash itself. */
    fun write(
        atEpochMillis: Long,
        text: String,
    ): Boolean =
        runCatching {
            directory.mkdirs()
            File(directory, "$PREFIX$atEpochMillis$SUFFIX").writeText(text.take(MAX_CHARS))
            prune()
        }.isSuccess

    /** The stored records, newest first; unreadable files are skipped. */
    fun stored(): List<StoredCrash> =
        runCatching {
            files().mapNotNull { file ->
                val at = timeOf(file) ?: return@mapNotNull null
                runCatching { StoredCrash(at, file.readText()) }.getOrNull()
            }
        }.getOrDefault(emptyList())

    /** Forgets every stored record. */
    fun clear() {
        runCatching { files().forEach { it.delete() } }
    }

    /** The record files, newest first. */
    private fun files(): List<File> =
        directory
            .listFiles { file -> file.isFile && timeOf(file) != null }
            .orEmpty()
            .sortedByDescending(::timeOf)

    private fun prune() {
        files().drop(MAX_FILES).forEach { it.delete() }
    }

    private fun timeOf(file: File): Long? =
        file.name
            .takeIf {
                it.startsWith(
                    PREFIX,
                ) && it.endsWith(SUFFIX)
            }?.removeSurrounding(PREFIX, SUFFIX)
            ?.toLongOrNull()

    companion object {
        /** Records kept; older ones are deleted as new ones arrive. */
        const val MAX_FILES: Int = 3

        /** Longest record written; a longer one is cut at its end, keeping the failure and its causes. */
        const val MAX_CHARS: Int = 16_000

        /** Directory name under the app's private files. */
        const val DIRECTORY: String = "crash"

        private const val PREFIX = "crash-"
        private const val SUFFIX = ".txt"
    }
}

/** The text of a crash record: the facts of the run, then the stack trace with its causes. */
internal object CrashRecord {
    fun text(
        facts: CrashFacts,
        thread: String,
        error: Throwable,
        atEpochMillis: Long,
    ): String =
        listOf(
            "at=$atEpochMillis",
            "app=${facts.app}",
            "android=${facts.android}",
            "device=${facts.device}",
            "locale=${facts.locale}",
            "settings=${facts.settings}",
            "route=${facts.route}",
            "thread=$thread",
            "",
            error.stackTraceToString(),
        ).joinToString("\n")
}

/**
 * Keeps the stack trace of a crash on this device so the next problem report can carry it (T-1504). It never
 * swallows the crash: the handler that was installed before it still runs, so Android shows and logs the failure as
 * it always would. Nothing is uploaded; the record leaves the device only inside a report the user sends.
 */
class CrashCapture(
    private val store: CrashLogStore,
    private val context: CrashContext,
    private val clock: () -> Long,
    private val previous: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(
        thread: Thread,
        error: Throwable,
    ) {
        val now = clock()
        runCatching { store.write(now, CrashRecord.text(context.facts, thread.name, error, now)) }
        previous?.uncaughtException(thread, error)
    }
}
