package com.fitness.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Swaps the real [FitnessApplication] for the generated Hilt test
 * application so @AndroidEntryPoint / @HiltAndroidApp-style DI works in
 * instrumented tests without initialising production services (e.g. the
 * token manager, encrypted preferences).
 *
 * Registered in build.gradle.kts via `testInstrumentationRunner`.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication_Application::class.java.name, context)
    }
}
