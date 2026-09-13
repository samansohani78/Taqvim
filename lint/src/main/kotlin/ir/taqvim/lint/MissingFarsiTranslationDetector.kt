/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.SdkConstants.ATTR_NAME
import com.android.SdkConstants.ATTR_TRANSLATABLE
import com.android.SdkConstants.FD_RES_VALUES
import com.android.SdkConstants.TAG_PLURALS
import com.android.SdkConstants.TAG_STRING
import com.android.SdkConstants.TAG_STRING_ARRAY
import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Element

/**
 * Persian is the mandatory translation (T-204): every translatable `string`, `plurals` and `string-array` of the
 * default `values` folder must also exist in `values-fa`. Other locales may be incomplete (they come from Weblate).
 */
class MissingFarsiTranslationDetector : ResourceXmlDetector() {
    private val defaults = LinkedHashMap<String, Location.Handle>()
    private val translated = HashSet<String>()

    override fun appliesTo(folderType: ResourceFolderType): Boolean = folderType == ResourceFolderType.VALUES

    override fun getApplicableElements(): Collection<String> = listOf(TAG_STRING, TAG_PLURALS, TAG_STRING_ARRAY)

    override fun visitElement(
        context: XmlContext,
        element: Element,
    ) {
        val name = element.getAttribute(ATTR_NAME)
        if (name.isEmpty() || element.getAttribute(ATTR_TRANSLATABLE) == "false") return
        when (context.file.parentFile?.name) {
            FD_RES_VALUES -> defaults.putIfAbsent(name, context.createLocationHandle(element))
            FARSI_FOLDER -> translated += name
        }
    }

    override fun afterCheckEachProject(context: Context) {
        defaults
            .filterKeys { it !in translated }
            .forEach { (name, handle) -> context.report(ISSUE, handle.resolve(), messageFor(name)) }
        defaults.clear()
        translated.clear()
    }

    companion object {
        private const val FARSI_FOLDER = "values-fa"

        /** Report text for the resource [name]; plain text so every lint output format shows it verbatim. */
        fun messageFor(name: String): String = "Missing Persian translation for string resource '$name' (values-fa)"

        /** The `MissingFarsiTranslation` issue. */
        val ISSUE: Issue =
            taqvimIssue(
                id = "MissingFarsiTranslation",
                brief = "Untranslated Persian string",
                explanation =
                    "Persian (`fa`) must stay 100% translated (docs/i18n/STRINGS.md). Add the string to " +
                        "`values-fa`, or mark it `translatable=\"false\"` if it must never be translated.",
                implementation = Implementation(MissingFarsiTranslationDetector::class.java, Scope.ALL_RESOURCES_SCOPE),
                category = Category.MESSAGES,
            )
    }
}
