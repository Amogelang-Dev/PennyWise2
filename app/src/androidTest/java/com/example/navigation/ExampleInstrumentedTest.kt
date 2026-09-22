package com.example.navigation

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/* This is a test file that checks if the app is set up correctly on a real or virtual phone */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        /* This test makes sure the app starts up with the right name and settings */
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.navigation", appContext.packageName)
    }
}