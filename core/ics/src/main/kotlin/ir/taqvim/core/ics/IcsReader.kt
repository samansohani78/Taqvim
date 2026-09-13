/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

/** A component (§3.6) with its own properties and nested components. */
internal data class Component(
    val name: String,
    val line: Int,
    val properties: List<ContentLine>,
    val children: List<Component>,
) {
    fun property(name: String): ContentLine? = properties.firstOrNull { it.name == name }

    fun properties(name: String): List<ContentLine> = properties.filter { it.name == name }

    fun text(name: String): String? = property(name)?.value?.let(ContentLines::unescapeText)
}

/**
 * Reads iCalendar text (RFC 5545) into [IcsCalendar]. Structural problems (malformed content lines, unbalanced
 * BEGIN/END, no VCALENDAR) fail the whole read; events or values that cannot be understood are skipped with a warning.
 */
public object IcsReader {
    /** Reads [text], which may use CRLF or bare LF line breaks. */
    public fun read(text: String): IcsParseResult {
        val errors = mutableListOf<IcsProblem>()
        val lines =
            ContentLines.unfold(text).mapNotNull { (number, raw) ->
                ContentLines.parse(number, raw).also {
                    if (it ==
                        null
                    ) {
                        errors += IcsProblem(number, "malformed content line")
                    }
                }
            }
        val roots = ComponentTree.build(lines, errors)
        if (errors.isEmpty() && roots.isEmpty()) errors += IcsProblem(1, "no VCALENDAR component")
        if (errors.isNotEmpty()) return IcsParseResult.Failure(errors)
        val warnings = mutableListOf<IcsProblem>()
        val events =
            roots.flatMap { root ->
                root.children.filter { it.name == "VEVENT" }.mapNotNull { event(it, warnings) }
            }
        val productId = roots.firstNotNullOfOrNull { it.text("PRODID") }.orEmpty()
        return IcsParseResult.Success(IcsCalendar(productId, events), warnings)
    }

    private fun event(
        component: Component,
        warnings: MutableList<IcsProblem>,
    ): IcsEvent? {
        val uid = component.text("UID")?.takeIf { it.isNotBlank() }
        val start = component.property("DTSTART")?.let { IcsValues.dateTime(it.value, it, warnings) }
        if (uid == null || start == null) {
            warnings += IcsProblem(component.line, "VEVENT without a valid UID and DTSTART skipped")
            return null
        }
        if (component.property("DURATION") != null) {
            warnings += IcsProblem(component.line, "DURATION is not supported; the event is imported without an end")
        }
        if (component.property("RDATE") != null) warnings += IcsProblem(component.line, "RDATE is not supported")
        return IcsEvent(
            uid = uid,
            start = start,
            end = component.property("DTEND")?.let { dateTimeOrWarn(it, warnings) },
            summary = component.text("SUMMARY"),
            description = component.text("DESCRIPTION"),
            recurrence = component.property("RRULE")?.let { recurrenceOrWarn(it, warnings) },
            exceptionDates = component.properties("EXDATE").flatMap { exceptionDates(it, warnings) },
            alarms = component.children.filter { it.name == "VALARM" }.mapNotNull { alarm(it, warnings) },
        )
    }

    private fun dateTimeOrWarn(
        property: ContentLine,
        warnings: MutableList<IcsProblem>,
    ): IcsDateTime? =
        IcsValues.dateTime(property.value, property, warnings).also {
            if (it == null) warnings += IcsProblem(property.line, "invalid ${property.name} ignored")
        }

    private fun recurrenceOrWarn(
        property: ContentLine,
        warnings: MutableList<IcsProblem>,
    ): Recurrence? =
        IcsValues.recurrence(property, warnings).also {
            if (it == null) warnings += IcsProblem(property.line, "unsupported or invalid RRULE ignored")
        }

    private fun exceptionDates(
        property: ContentLine,
        warnings: MutableList<IcsProblem>,
    ): List<IcsDateTime> =
        property.value.split(',').mapNotNull { value ->
            IcsValues.dateTime(value, property, warnings).also {
                if (it == null) warnings += IcsProblem(property.line, "invalid EXDATE value '$value' ignored")
            }
        }

    private fun alarm(
        component: Component,
        warnings: MutableList<IcsProblem>,
    ): DisplayAlarm? {
        val action = component.property("ACTION")?.value?.uppercase()
        val trigger = component.property("TRIGGER")?.let(::trigger)
        val problem =
            when {
                action != "DISPLAY" -> "VALARM with ACTION ${action ?: "missing"} ignored"
                trigger == null -> "VALARM without a valid TRIGGER ignored"
                else -> null
            }
        if (problem != null) warnings += IcsProblem(component.line, problem)
        return trigger?.takeIf { problem == null }?.let { DisplayAlarm(it, component.text("DESCRIPTION").orEmpty()) }
    }

    private fun trigger(property: ContentLine): AlarmTrigger? =
        if (property.parameter("VALUE").equals("DATE-TIME", ignoreCase = true)) {
            (IcsValues.dateTime(property.value, property, mutableListOf()) as? IcsDateTime.Utc)
                ?.let { AlarmTrigger.Absolute(it.instant) }
        } else {
            IcsValues.duration(property.value)?.let {
                AlarmTrigger.Relative(it, relatedToEnd = property.parameter("RELATED").equals("END", ignoreCase = true))
            }
        }
}

/** Builds the component tree from content lines, reporting unbalanced BEGIN/END as errors. */
internal object ComponentTree {
    private class Open(
        val name: String,
        val line: Int,
    ) {
        val properties = mutableListOf<ContentLine>()
        val children = mutableListOf<Component>()

        fun close() = Component(name, line, properties.toList(), children.toList())
    }

    /** Top-level VCALENDAR components of [lines]. */
    fun build(
        lines: List<ContentLine>,
        errors: MutableList<IcsProblem>,
    ): List<Component> {
        val stack = ArrayDeque<Open>()
        val roots = mutableListOf<Component>()
        lines.forEach { line ->
            when {
                line.name == "BEGIN" -> stack.addLast(Open(line.value.uppercase(), line.line))
                line.name == "END" -> close(line, stack, errors)?.let { roots += it }
                stack.isEmpty() -> errors += IcsProblem(line.line, "property ${line.name} outside any component")
                else -> stack.last().properties += line
            }
        }
        stack.forEach { errors += IcsProblem(it.line, "BEGIN:${it.name} is never closed") }
        roots.filter { it.name != "VCALENDAR" }.forEach {
            errors +=
                IcsProblem(it.line, "top-level ${it.name} is not a VCALENDAR")
        }
        return roots.filter { it.name == "VCALENDAR" }
    }

    /** Closes the innermost component; returns it when it was a top-level component. */
    private fun close(
        line: ContentLine,
        stack: ArrayDeque<Open>,
        errors: MutableList<IcsProblem>,
    ): Component? {
        val open = stack.removeLastOrNull()
        if (open == null || open.name != line.value.uppercase()) {
            errors += IcsProblem(line.line, "END:${line.value} does not close ${open?.name ?: "any component"}")
            return null
        }
        val component = open.close()
        val parent = stack.lastOrNull() ?: return component
        parent.children += component
        return null
    }
}
