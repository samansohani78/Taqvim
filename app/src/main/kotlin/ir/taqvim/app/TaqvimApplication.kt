/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.app.Application
import ir.taqvim.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Process entry point: builds the Koin DI graph (see docs/adr/0002-dependency-injection-koin.md). */
class TaqvimApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TaqvimApplication)
            modules(appModule)
        }
    }
}
