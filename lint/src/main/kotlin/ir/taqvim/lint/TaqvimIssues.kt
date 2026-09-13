/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

/** Creates a Taqvim error issue analysed per Kotlin/Java source file. */
internal fun taqvimIssue(
    id: String,
    brief: String,
    explanation: String,
    detector: Class<out Detector>,
    category: Category = Category.CORRECTNESS,
): Issue = taqvimIssue(id, brief, explanation, Implementation(detector, Scope.JAVA_FILE_SCOPE), category)

/** Creates a Taqvim error issue with a custom [implementation], e.g. for detectors that also scan the manifest. */
internal fun taqvimIssue(
    id: String,
    brief: String,
    explanation: String,
    implementation: Implementation,
    category: Category = Category.CORRECTNESS,
): Issue =
    Issue.create(
        id = id,
        briefDescription = brief,
        explanation = explanation,
        category = category,
        priority = ISSUE_PRIORITY,
        severity = Severity.ERROR,
        implementation = implementation,
    )

private const val ISSUE_PRIORITY = 6
