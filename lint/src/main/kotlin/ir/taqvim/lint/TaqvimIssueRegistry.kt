/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

/** Registers Taqvim's custom lint issues (T-004). */
class TaqvimIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = emptyList()

    override val api: Int = CURRENT_API

    override val vendor: Vendor =
        Vendor(
            vendorName = "Taqvim",
            identifier = "ir.taqvim:lint",
            feedbackUrl = "https://github.com/samansohani78/Taqvim/issues",
        )
}
