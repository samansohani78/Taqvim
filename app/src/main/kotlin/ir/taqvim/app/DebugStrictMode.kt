/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.os.StrictMode

/**
 * StrictMode in debug builds (docs/PLAN.md T-1804, ADR-0017): disk and network access on the main thread, leaked
 * closeables and cleartext network use are logged. Violations are logged rather than fatal, because libraries outside
 * Taqvim's control also trigger them.
 */
internal object DebugStrictMode {
    /** Installs the debug policies when [debug] is true; release builds keep the platform's lax defaults. */
    fun install(debug: Boolean) {
        if (!debug) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy
                .Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy
                .Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
    }
}
