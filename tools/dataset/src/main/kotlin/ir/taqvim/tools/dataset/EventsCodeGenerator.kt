/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/** One generated Kotlin source file. */
data class GeneratedSource(
    val fileName: String,
    val content: String,
)

/**
 * Compiles dataset files into Kotlin sources for `:data:events` (D-08): `OfficialEventsPartN.kt` files holding at most
 * [EVENTS_PER_FILE] `EventDefinition`s each (sorted by id), and `OfficialEvents.kt` exposing them all. The input must
 * already have passed [DatasetValidator]. Output is stable, so a regenerated file can be compared with the committed
 * one.
 */
object EventsCodeGenerator {
    /** Package of the generated sources. */
    const val PACKAGE = "ir.taqvim.data.events.generated"

    /** Events per part file, keeping generated files well below the 400-line limit. */
    const val EVENTS_PER_FILE = 8

    private const val EVENTS = "ir.taqvim.core.events"
    private const val MODEL = "ir.taqvim.core.model"
    private const val PERSIAN = "fa"
    private const val INDENT = "    "
    private val EVENT_DEFINITION = ClassName(EVENTS, "EventDefinition")
    private val EVENT_ID = ClassName(EVENTS, "EventId")
    private val EVENT_SOURCE = ClassName(EVENTS, "EventSource")
    private val EVENT_CATEGORY = ClassName(EVENTS, "EventCategory")
    private val EVENT_FLAG = ClassName(EVENTS, "EventFlag")
    private val LOCALIZED_TEXT = ClassName(EVENTS, "LocalizedText")
    private val CITATION = ClassName(EVENTS, "Citation")
    private val VALIDITY = ClassName(EVENTS, "Validity")
    private val ASTRO_KIND = ClassName(EVENTS, "AstroKind")
    private val TITHI_OBSERVANCE = ClassName("ir.taqvim.core.calendar", "TithiObservance")
    private val CALENDAR_SYSTEM = ClassName(MODEL, "CalendarSystem")
    private val WEEKDAY = ClassName(MODEL, "Weekday")
    private val EVENT_LIST = LIST.parameterizedBy(EVENT_DEFINITION)

    /** Constructor parameters of each `EventRule`, in declaration order. */
    private val RULE_PARAMETERS =
        mapOf(
            "Fixed" to listOf("month", "day"),
            "NthWeekdayOfMonth" to listOf("month", "weekday", "n"),
            "LastWeekdayOfMonth" to listOf("month", "weekday", "offsetDays"),
            "LastDayOfMonth" to listOf("month"),
            "Single" to listOf("year", "month", "day"),
            "NthDayOfYear" to listOf("n"),
            "RelativeToEvent" to listOf("eventId", "offsetDays"),
            "Astronomical" to listOf("kind", "offsetDays", "timeZone", "month"),
            "LunarTithi" to listOf("month", "tithi", "observance", "endTithi", "endOffsetDays"),
        )

    /** `Week`'s `start` is a nested rule object, not a flat parameter (ADR-0046); handled separately in [ruleCode]. */
    private const val WEEK = "Week"

    private const val HEADER =
        "/*\n * Copyright (c) 2026 Saman Sohani. All Rights Reserved.\n" +
            " * Proprietary and confidential. See the LICENSE file in the repository root.\n */\n"
    private const val GENERATED_NOTE = "Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit."

    /** Sources for all events of [files] (file name → JSON text). */
    fun generate(
        files: Map<String, String>,
        eventsPerFile: Int = EVENTS_PER_FILE,
    ): List<GeneratedSource> {
        require(eventsPerFile > 0) { "eventsPerFile must be positive (was $eventsPerFile)" }
        val events = files.values.flatMap(::eventsOf).sortedBy { it.text("id") }
        val parts = events.chunked(eventsPerFile)
        return parts.mapIndexed { index, part -> partFile(index + 1, part) } + indexFile(parts.size)
    }

    private fun eventsOf(text: String): List<JsonObject> {
        val document = Json.parseToJsonElement(text) as? JsonObject
        return (document?.get("events") as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
    }

    private fun partProperty(number: Int) = "OFFICIAL_EVENTS_PART_$number"

    private fun partFile(
        number: Int,
        events: List<JsonObject>,
    ): GeneratedSource {
        val name = "OfficialEventsPart$number"
        val property =
            PropertySpec
                .builder(partProperty(number), EVENT_LIST, KModifier.INTERNAL)
                .addKdoc("Part %L of the dataset events.\n\n%L", number, GENERATED_NOTE)
                .initializer("listOf(\n⇥%L,\n⇤)", events.map(::eventCode).joinToCode(",\n"))
                .build()
        val suppression =
            AnnotationSpec
                .builder(Suppress::class)
                // Dataset text is data, and official titles can exceed the line limit as single string literals.
                .addMember("%S, %S", "NoHardcodedNonLatinText", "MaxLineLength")
                .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                .build()
        val file = fileBuilder(name).addAnnotation(suppression).addProperty(property).build()
        return GeneratedSource("$name.kt", render(file))
    }

    private fun indexFile(parts: Int): GeneratedSource {
        val all =
            if (parts == 0) {
                CodeBlock.of("emptyList()")
            } else {
                (1..parts).map { CodeBlock.of("%L", partProperty(it)) }.joinToCode(" +\n")
            }
        val property =
            PropertySpec
                .builder("ALL", EVENT_LIST)
                .addKdoc("Every dataset event, sorted by id.")
                .initializer(all)
                .build()
        val type =
            TypeSpec
                .objectBuilder("OfficialEvents")
                .addKdoc("Events compiled from the dataset (D-08).\n\n%L", GENERATED_NOTE)
                .addProperty(property)
                .build()
        return GeneratedSource("OfficialEvents.kt", render(fileBuilder("OfficialEvents").addType(type).build()))
    }

    private fun fileBuilder(name: String): FileSpec.Builder =
        FileSpec.builder(PACKAGE, name).indent(INDENT).addKotlinDefaultImports(includeJvm = false, includeJs = false)

    /** KotlinPoet escapes the `data` package segment; package names need no escaping, so the escape is removed. */
    private fun render(file: FileSpec): String =
        HEADER + file.toString().replace("package ir.taqvim.`data`.", "package ir.taqvim.data.")

    private fun eventCode(event: JsonObject): CodeBlock {
        val code = CodeBlock.builder().add("%T(\n⇥", EVENT_DEFINITION)
        code.add("id = %T(%S),\n", EVENT_ID, event.text("id"))
        code.add("calendar = %T.%L,\n", CALENDAR_SYSTEM, event.text("calendar"))
        code.add("source = %T.%L,\n", EVENT_SOURCE, event.text("source"))
        code.add("category = %T.%L,\n", EVENT_CATEGORY, event.text("category"))
        code.add("isHoliday = %L,\n", event.getValue("isHoliday").jsonPrimitive.boolean)
        code.add("title = %L,\n", localizedText(event.child("title")))
        code.add("rule = %L,\n", ruleCode(event.child("rule")))
        (event["validity"] as? JsonObject)?.let { code.add("validity = %L,\n", validityCode(it)) }
        event.strings("flags").takeIf { it.isNotEmpty() }?.let { flags ->
            code.add("flags = setOf(%L),\n", flags.map { CodeBlock.of("%T.%L", EVENT_FLAG, it) }.joinToCode())
        }
        event.strings("aliases").takeIf { it.isNotEmpty() }?.let { aliases ->
            code.add("aliases = %L,\n", multiline("listOf", aliases.map { CodeBlock.of("%S", it) }))
        }
        val citations = (event["citations"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>().map(::citationCode)
        code.add("citations = %L,\n", multiline("listOf", citations))
        (event["links"] as? JsonObject)?.takeIf { it.isNotEmpty() }?.let { links ->
            code.add("links = %L,\n", multiline("mapOf", pairs(links.entries.sortedBy { it.key })))
        }
        return code.add("⇤)").build()
    }

    /** `name(` + one argument per line + `)`, with a trailing comma. */
    private fun multiline(
        name: String,
        arguments: List<CodeBlock>,
    ): CodeBlock = CodeBlock.of("%L(\n⇥%L,\n⇤)", name, arguments.joinToCode(",\n"))

    private fun localizedText(title: JsonObject): CodeBlock {
        val entries = pairs(title.entries.sortedWith(compareBy({ it.key != PERSIAN }, { it.key })))
        return if (entries.size == 1) {
            CodeBlock.of("%T(mapOf(%L))", LOCALIZED_TEXT, entries.single())
        } else {
            CodeBlock.of("%T(\n⇥%L,\n⇤)", LOCALIZED_TEXT, multiline("mapOf", entries))
        }
    }

    private fun pairs(entries: List<Map.Entry<String, JsonElement>>): List<CodeBlock> =
        entries.map { (key, value) -> CodeBlock.of("%S to %S", key, value.jsonPrimitive.content) }

    private fun ruleCode(rule: JsonObject): CodeBlock {
        val type = rule.text("type")
        if (type == WEEK) {
            val start = ruleCode(rule.child("start"))
            val lengthDays = rule.getValue("lengthDays").jsonPrimitive.int
            return CodeBlock.of(
                "%T(start = %L, lengthDays = %L)",
                ClassName(EVENTS, "EventRule", WEEK),
                start,
                lengthDays,
            )
        }
        val parameters = requireNotNull(RULE_PARAMETERS[type]) { "unknown rule type '$type'" }
        val arguments = parameters.mapNotNull { name -> rule[name]?.let { argument(name, it.jsonPrimitive) } }
        return CodeBlock.of("%T(%L)", ClassName(EVENTS, "EventRule", type), arguments.joinToCode())
    }

    private fun argument(
        name: String,
        value: JsonPrimitive,
    ): CodeBlock =
        when (name) {
            "weekday" -> CodeBlock.of("%L = %T.%L", name, WEEKDAY, value.content)
            "kind" -> CodeBlock.of("%L = %T.%L", name, ASTRO_KIND, value.content)
            "observance" -> CodeBlock.of("%L = %T.%L", name, TITHI_OBSERVANCE, value.content)
            "eventId" -> CodeBlock.of("%L = %T(%S)", name, EVENT_ID, value.content)
            "timeZone" -> CodeBlock.of("%L = %S", name, value.content)
            else -> CodeBlock.of("%L = %L", name, value.int)
        }

    private fun validityCode(validity: JsonObject): CodeBlock =
        CodeBlock.of(
            "%T(\n⇥calendar = %T.%L,\nfromYear = %L,\ntoYear = %L,\ncitation = %L,\n⇤)",
            VALIDITY,
            CALENDAR_SYSTEM,
            validity.text("calendar"),
            validity["fromYear"]?.jsonPrimitive?.int,
            validity["toYear"]?.jsonPrimitive?.int,
            citationCode(validity.child("citation")),
        )

    private fun citationCode(citation: JsonObject): CodeBlock {
        val arguments =
            listOfNotNull(
                CodeBlock.of("url = %S", citation.text("url")),
                CodeBlock.of("title = %S", citation.text("title")),
                (citation["page"] as? JsonPrimitive)?.let { CodeBlock.of("page = %S", it.content) },
            )
        return CodeBlock.of("%T(\n⇥%L,\n⇤)", CITATION, arguments.joinToCode(",\n"))
    }

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.child(key: String): JsonObject =
        requireNotNull(this[key] as? JsonObject) { "missing object '$key'" }

    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray).orEmpty().map { it.jsonPrimitive.content }
}
