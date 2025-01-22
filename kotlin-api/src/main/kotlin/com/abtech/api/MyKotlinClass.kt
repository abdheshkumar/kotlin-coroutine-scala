package com.abtech.api

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.withContext

class MyKotlinClass {
    suspend fun hello(name: String): String {
        println()
        return "Hello, $name!"
    }

    suspend fun helloWithContext(name: String): String = withContext(CoroutineName("helloWithContext")) {
        println()
        "Hello, $name!"
    }

    private fun println() {
        println("Running by ${Thread.currentThread().name}")
    }
}