package com.app.miklink.testsupport

import java.io.BufferedReader
import java.io.InputStreamReader

object FixtureLoader {
    fun load(path: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Missing fixture: $path")

        BufferedReader(InputStreamReader(stream)).use { reader ->
            return reader.readText()
        }
    }
}
