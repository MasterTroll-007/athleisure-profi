package com.fitness.app

import android.app.Application
import dagger.hilt.android.testing.CustomTestApplication

/**
 * Custom Application used during instrumented tests. Hilt generates
 * `HiltTestApplication_Application` from the base Application — we register
 * this class via the custom [HiltTestRunner] so the app lives under a Hilt-
 * aware runtime without the real `FitnessApplication` side effects.
 */
@CustomTestApplication(Application::class)
interface HiltTestApplication
