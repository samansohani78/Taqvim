/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

/** DT-038: the optional scope dimension — backward compatible, validated, and used by the Nepal records. */
class EventScopeTest {
    private val datasetDirectory = File(property("taqvim.dataset.directory"))
    private val schema = File(datasetDirectory, "events.v1.json").readText()

    private fun validate(vararg files: Pair<String, String>) = DatasetValidator(schema).validate(files.toMap())

    @Test
    fun `a record without a scope is still valid, because that is what every record meant before`() {
        validate("nepal/nepal-official-holidays.json" to nepalText()).shouldBeEmpty()
        // Every record that carries no scope keeps working: most of the dataset does not.
        val scoped = nepalEvents().count { it["scope"] != null }
        (scoped in 1 until nepalEvents().size) shouldBe true
    }

    @Test
    fun `the Nepal split the dimension was added for is expressed, not approximated`() {
        val hill = nepalEvents().first { it.text("id") == "np.holiday.fagu-purnima-hill" }
        val terai = nepalEvents().first { it.text("id") == "np.holiday.fagu-purnima-terai" }

        // The two days of the same festival, told apart by the moment the tithi is read: the hills keep the day whose
        // sunset falls in the full-moon tithi, the Terai the day whose sunrise does. Both reproduce both notices.
        (hill["rule"] as JsonObject).text("observance") shouldBe "SUNSET"
        (terai["rule"] as JsonObject).text("observance") shouldBe "SUNRISE"
        (hill["scope"] as JsonObject).text("level") shouldBe "REGION"
        areasOf(terai).shouldContainExactly("np.region.terai")

        // Neither is a day off: the app cannot tell which district the user is in (DT-038).
        (hill["isHoliday"] as JsonPrimitive).content shouldBe "false"
        (terai["isHoliday"] as JsonPrimitive).content shouldBe "false"
    }

    @Test
    fun `an area from another country, or of another level, is rejected`() {
        val wrongCountry = nepalText().replace("\"np.region.terai\"", "\"ir.region.terai\"")
        val wrongLevel = nepalText().replace("\"np.community.newar\"", "\"np.district.newar\"")

        validate("nepal/x.json" to wrongCountry).map { it.kind } shouldContainExactly listOf(IssueKind.SCOPE_MISMATCH)
        validate("nepal/x.json" to wrongLevel).map { it.kind } shouldContainExactly listOf(IssueKind.SCOPE_MISMATCH)
    }

    private fun nepalText() = File(datasetDirectory, "nepal/nepal-official-holidays.json").readText()

    private fun nepalEvents(): List<JsonObject> =
        ((Json.parseToJsonElement(nepalText()) as? JsonObject)?.get("events") as? JsonArray)
            .orEmpty()
            .mapNotNull { it as? JsonObject }

    private fun areasOf(event: JsonObject): List<String> =
        ((event["scope"] as JsonObject)["areas"] as JsonArray).map { (it as JsonPrimitive).content }

    private fun JsonObject.text(key: String): String = (this[key] as JsonPrimitive).content

    private companion object {
        fun property(name: String): String = requireNotNull(System.getProperty(name)) { "$name is not set" }
    }
}
