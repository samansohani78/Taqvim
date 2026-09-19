/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.diagnostics

import ir.taqvim.feature.about.DiagnosticsRedactor
import java.io.File
import java.util.Collections
import java.util.IdentityHashMap
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

        /** Longest record written; a longer one is cut at its end (traces are kept short enough to fit). */
        const val MAX_CHARS: Int = 16_000

        /** Directory name under the app's private files. */
        const val DIRECTORY: String = "crash"

        private const val PREFIX = "crash-"
        private const val SUFFIX = ".txt"
    }
}

/**
 * The text of a crash record: the facts of the run, then the stack trace with its causes and suppressed failures.
 * The facts are structured values the app chose; an exception message is arbitrary text that may carry an event
 * title, a place or a token, so every message is redacted before it is stored, not only when it is reported
 * (review R03). Class names and code frames are kept as they are.
 */
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
            trace(error),
        ).joinToString("\n")

    /**
     * The stack trace of [error] in the JVM's own layout, with every message redacted. As the JVM does, a cause or a
     * suppressed failure omits the frames it shares with the trace around it (`... n more`), and each failure keeps
     * at most [MAX_FRAMES] of its own, so the causes fit in a record instead of being cut off at its end.
     */
    fun trace(error: Throwable): String =
        buildString {
            append(error, Enclosing(prefix = "", indent = "", frames = emptyArray()), identitySet())
        }

    private class Enclosing(
        val prefix: String,
        val indent: String,
        val frames: Array<StackTraceElement>,
    )

    private fun identitySet(): MutableSet<Throwable> = Collections.newSetFromMap(IdentityHashMap())

    private fun StringBuilder.append(
        error: Throwable,
        around: Enclosing,
        seen: MutableSet<Throwable>,
    ) {
        if (!seen.add(error)) {
            appendLine("${around.indent}${around.prefix}[CIRCULAR REFERENCE: ${error.javaClass.name}]")
            return
        }
        appendLine("${around.indent}${around.prefix}${headline(error)}")
        val frames = error.stackTrace
        val own = frames.size - shared(frames, around.frames)
        frames.take(minOf(own, MAX_FRAMES)).forEach { appendLine("${around.indent}\tat $it") }
        val omitted = frames.size - minOf(own, MAX_FRAMES)
        if (omitted > 0) appendLine("${around.indent}\t... $omitted more")
        error.suppressed.forEach { append(it, Enclosing("Suppressed: ", "${around.indent}\t", frames), seen) }
        error.cause?.let { append(it, Enclosing("Caused by: ", around.indent, frames), seen) }
    }

    /** How many frames at the bottom of [frames] are also at the bottom of [enclosing]. */
    private fun shared(
        frames: Array<StackTraceElement>,
        enclosing: Array<StackTraceElement>,
    ): Int {
        var count = 0
        while (count < frames.size && count < enclosing.size &&
            frames[frames.size - 1 - count] == enclosing[enclosing.size - 1 - count]
        ) {
            count++
        }
        return count
    }

    private fun headline(error: Throwable): String =
        error.message?.let { "${error.javaClass.name}: ${DiagnosticsRedactor.redact(it)}" } ?: error.javaClass.name

    /** Frames kept for one failure; deeper ones are counted in its `... n more` line. */
    private const val MAX_FRAMES = 40
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
